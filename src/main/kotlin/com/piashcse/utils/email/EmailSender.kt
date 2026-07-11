package com.piashcse.utils.email

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.AppConstants
import com.piashcse.service.CacheService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail
import org.slf4j.LoggerFactory

object EmailSender {
    private val log = LoggerFactory.getLogger(EmailSender::class.java)
    private val smtpHost = DotEnvConfig.emailHost
    private val emailPort = DotEnvConfig.emailPort
    private val smtpUser = DotEnvConfig.emailUsername
    private val smtpPassword = DotEnvConfig.emailPassword
    private val fromEmail = DotEnvConfig.emailUsername

    private const val MAX_EMAILS_PER_WINDOW = 3
    private const val EMAIL_WINDOW_SECONDS = 15 * 60L

    suspend fun sendOtp(
        toEmail: String,
        otp: String,
        subject: String = AppConstants.SmtpServer.OTP_SUBJECT,
    ) = send(toEmail, subject, "Your verification code is: $otp")

    suspend fun send(
        toEmail: String,
        subject: String,
        body: String,
    ) {
        val rateLimitKey = "email_rate_limit:$toEmail"
        val recentSends = CacheService.cache.get<Int>(rateLimitKey) ?: 0
        if (recentSends >= MAX_EMAILS_PER_WINDOW) {
            log.warn("Rate limit exceeded for $toEmail (subject: $subject)")
            return
        }
        CacheService.cache.set(rateLimitKey, recentSends + 1, EMAIL_WINDOW_SECONDS)

        withContext(Dispatchers.IO) {
            try {
                SimpleEmail().apply {
                    hostName = smtpHost
                    setSmtpPort(emailPort)
                    setAuthenticator(DefaultAuthenticator(smtpUser, smtpPassword))
                    isSSLOnConnect = true
                    setFrom(fromEmail)
                    this.subject = subject
                    setMsg(body)
                    addTo(toEmail)
                    send()
                }
                log.info("Email sent successfully to $toEmail (subject: $subject)")
            } catch (e: EmailException) {
                log.error("Failed to send email to $toEmail: ${e.message}", e)
            }
        }
    }
}
