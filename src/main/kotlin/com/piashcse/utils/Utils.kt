package com.piashcse.utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail

fun sendEmail(
    toEmail: String,
    verificationCode: String,
    fromEmail: String = AppConstants.SmtpServer.SENDING_EMAIL,
    subject: String = AppConstants.SmtpServer.EMAIL_SUBJECT,
    smtpHost: String = AppConstants.SmtpServer.HOST_NAME,
    smtpPort: Int = AppConstants.SmtpServer.PORT,
    smtpUser: String = AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR,
    smtpPassword: String = AppConstants.SmtpServer.DEFAULT_AUTHENTICATOR_PASSWORD
) {
    try {
        CoroutineScope(Dispatchers.IO).launch {
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
        }
    } catch (e: EmailException) {
        throw CommonException("Sending email failed")
    }
}