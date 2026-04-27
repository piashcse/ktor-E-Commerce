package com.piashcse.utils.email

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.AppConstants
import kotlinx.coroutines.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail

private val emailScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Sends an email asynchronously using the configured SMTP server.
 */
fun sendEmail(
    toEmail: String,
    verificationCode: String,
    fromEmail: String = DotEnvConfig.emailUsername,
    subject: String = AppConstants.SmtpServer.EMAIL_SUBJECT,
    smtpHost: String = DotEnvConfig.emailHost,
    smtpPort: Int = DotEnvConfig.emailPort,
    smtpUser: String = DotEnvConfig.emailUsername,
    smtpPassword: String = DotEnvConfig.emailPassword
) {
    emailScope.launch {
        try {
            SimpleEmail().apply {
                hostName = smtpHost
                setSmtpPort(smtpPort)
                setAuthenticator(DefaultAuthenticator(smtpUser, smtpPassword))
                isSSLOnConnect = true
                setFrom(fromEmail)
                this.subject = subject
                setMsg("Your verification code is: $verificationCode")
                addTo(toEmail)
                send()
            }
        } catch (e: EmailException) {
            e.printStackTrace()
        }
    }
}
