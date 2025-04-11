package com.piashcse.utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail
import kotlin.math.pow
import kotlin.random.Random

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

fun generateOTP(length: Int = 6): String {
    val min = 10.0.pow(length - 1).toInt()  // Example: 10^(4-1) = 1000
    val max = (10.0.pow(length) - 1).toInt()  // Example: 10^4 - 1 = 9999
    return Random.nextInt(min, max).toString()
}