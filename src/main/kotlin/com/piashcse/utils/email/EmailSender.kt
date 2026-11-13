package com.piashcse.utils.email

import com.piashcse.config.DotEnvConfig
import com.piashcse.service.CacheService
import com.piashcse.utils.resilience.CircuitBreaker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail
import org.slf4j.LoggerFactory

object EmailSender {
    private val log = LoggerFactory.getLogger(EmailSender::class.java)

    /** Keep recipient addresses out of logs (PII); show only the domain. */
    private fun maskEmail(value: String): String {
        val at = value.indexOf('@')
        return if (at <= 0) "***" else "${value.first()}***${value.substring(at)}"
    }

    private val smtpHost = DotEnvConfig.emailHost
    private val emailPort = DotEnvConfig.emailPort
    private val smtpUser = DotEnvConfig.emailUsername
    private val smtpPassword = DotEnvConfig.emailPassword
    private val fromEmail = DotEnvConfig.emailUsername

    private const val MAX_EMAILS_PER_WINDOW = 3
    private const val EMAIL_WINDOW_SECONDS = 15 * 60L
    private val smtpBreaker = CircuitBreaker("smtp", failureThreshold = 3, cooldownMs = 60_000)

    suspend fun send(
        toEmail: String,
        subject: String,
        body: String,
    ) {
        val rateLimitKey = "email_rate_limit:$toEmail"
        val recentSends = CacheService.cache.get<Int>(rateLimitKey) ?: 0
        if (recentSends >= MAX_EMAILS_PER_WINDOW) {
            log.warn("Rate limit exceeded for ${maskEmail(toEmail)} (subject: $subject)")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val email =
                    SimpleEmail().apply {
                        hostName = smtpHost
                        setSmtpPort(emailPort)
                        setAuthenticator(DefaultAuthenticator(smtpUser, smtpPassword))
                        isSSLOnConnect = DotEnvConfig.emailSslEnabled
                        isStartTLSEnabled = !DotEnvConfig.emailSslEnabled
                        isStartTLSRequired = !DotEnvConfig.emailSslEnabled
                        // A hung SMTP server must not block the single EventBus
                        // collector forever; fail and let retry/dead-letter handle it.
                        setSocketTimeout(10_000)
                        setSocketConnectionTimeout(10_000)
                        setFrom(fromEmail)
                        this.subject = subject
                        setMsg(body)
                        addTo(toEmail)
                    }
                smtpBreaker.execute {
                    email.send()
                }
                // Only successful sends consume the rate-limit slot.
                CacheService.cache.set(rateLimitKey, recentSends + 1, EMAIL_WINDOW_SECONDS)
                log.info("Email sent successfully to ${maskEmail(toEmail)} (subject: $subject)")
            } catch (e: EmailException) {
                // Propagate so the EventBus retries/dead-letters instead of silently dropping.
                log.error("Failed to send email to ${maskEmail(toEmail)}: ${e.message}", e)
                throw e
            }
        }
    }
}
