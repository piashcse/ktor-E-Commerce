package com.piashcse.feature.privacy

import com.piashcse.plugin.customerOnlyAuth
import com.piashcse.utils.common.MessageResponse
import com.piashcse.utils.extension.currentUserId
import com.piashcse.utils.extension.respondOk
import io.ktor.server.request.receive
import io.ktor.server.routing.*
import org.koin.ktor.ext.inject

/** GDPR data-subject endpoints: full export and account erasure. */
fun Route.privacyRoutes() {
    val privacyService: PrivacyService by inject()
    customerOnlyAuth {
        /**
         * @tag Privacy
         * @description Export all personal data held for the authenticated user
         */
        get("export") {
            call.respondOk(privacyService.export(call.currentUserId))
        }

        /**
         * @tag Privacy
         * @description Erase/anonymize the authenticated account (password required)
         */
        post("erase") {
            val body = call.receive<EraseAccountRequest>()
            privacyService.erase(call.currentUserId, body.password)
            call.respondOk(MessageResponse("Account erased and anonymized"))
        }
    }
}
