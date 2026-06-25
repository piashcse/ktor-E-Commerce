package com.piashcse.utils.email

import com.piashcse.config.DotEnvConfig
import com.piashcse.constants.AppConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailException
import org.apache.commons.mail.SimpleEmail

private val emailScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

object EmailSender {
    private val smtpHost = DotEnvConfig.emailHost
    private val emailPort = DotEnvConfig.emailPort
    private val smtpUser = DotEnvConfig.emailUsername
    private val smtpPassword = DotEnvConfig.emailPassword
    private val fromEmail = DotEnvConfig.emailUsername

    fun sendOtp(
        toEmail: String,
        otp: String,
    ) {
        emailScope.launch {
            try {
                SimpleEmail().apply {
                    hostName = smtpHost
                    setSmtpPort(emailPort)
                    setAuthenticator(DefaultAuthenticator(smtpUser, smtpPassword))
                    isSSLOnConnect = true
                    setFrom(fromEmail)
                    subject = AppConstants.SmtpServer.EMAIL_SUBJECT
                    setMsg("Your verification code is: $otp")
                    addTo(toEmail)
                    send()
                }
            } catch (e: EmailException) {
                e.printStackTrace()
            }
        }
    }
}
