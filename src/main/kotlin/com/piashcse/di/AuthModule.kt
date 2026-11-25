package com.piashcse.di

import com.piashcse.config.DotEnvConfig
import com.piashcse.feature.auth.GoogleTokenVerifier
import com.piashcse.feature.auth.MfaService
import com.piashcse.feature.auth.PasskeyService
import com.piashcse.feature.auth.SocialAuthService
import com.piashcse.feature.auth.UserAuthenticationService
import org.koin.dsl.module

/** Authentication services and their provider adapters. */
val authModule =
    module {
        single { MfaService() }
        single { GoogleTokenVerifier(DotEnvConfig.googleClientId) }
        single { SocialAuthService(get(), get(), get()) }
        single { PasskeyService(get(), DotEnvConfig.webauthnRpId, DotEnvConfig.webauthnRpName, DotEnvConfig.webauthnOrigin, get()) }
        single { UserAuthenticationService(get(), get()) }
    }
