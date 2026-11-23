package com.piashcse.plugin

import com.charleskorn.kaml.SingleLineStringStyle
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import io.ktor.openapi.OpenApiDoc
import io.ktor.openapi.Operation
import io.ktor.openapi.PathItem
import io.ktor.openapi.ReferenceOr
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString

/**
 * Renders the generated OpenAPI document with deterministic operation ids.
 *
 * Ktor's route DSL does not infer ids, and writing them by hand for every route
 * drifts quickly. Deriving `{method}{PathInPascalCase}` from the path gives
 * generated clients stable method names without touching 400+ route files.
 */
@OptIn(ExperimentalSerializationApi::class)
internal object OpenApiDocs {
    private val yaml =
        Yaml(
            configuration =
                YamlConfiguration(
                    encodeDefaults = false,
                    singleLineStringStyle = SingleLineStringStyle.PlainExceptAmbiguous,
                ),
        )

    fun render(document: OpenApiDoc): String = yaml.encodeToString(document.withGeneratedOperationIds())

    private fun OpenApiDoc.withGeneratedOperationIds(): OpenApiDoc {
        val used = mutableSetOf<String>()
        return copy(paths = paths.mapValues { (path, item) -> item.withOperationIds(path, used) })
    }

    private fun ReferenceOr<PathItem>.withOperationIds(
        path: String,
        used: MutableSet<String>,
    ): ReferenceOr<PathItem> =
        when (this) {
            is ReferenceOr.Reference -> this
            is ReferenceOr.Value -> ReferenceOr.Value(value.withOperationIds(path, used))
        }

    private fun PathItem.withOperationIds(
        path: String,
        used: MutableSet<String>,
    ): PathItem =
        copy(
            get = get?.withOperationId("get", path, used),
            put = put?.withOperationId("put", path, used),
            post = post?.withOperationId("post", path, used),
            delete = delete?.withOperationId("delete", path, used),
            patch = patch?.withOperationId("patch", path, used),
            head = head?.withOperationId("head", path, used),
            options = options?.withOperationId("options", path, used),
            trace = trace?.withOperationId("trace", path, used),
        )

    private fun Operation.withOperationId(
        method: String,
        path: String,
        used: MutableSet<String>,
    ): Operation {
        val base = operationId ?: operationIdFor(method, path)
        var id = base
        var suffix = 2
        while (!used.add(id)) {
            id = "$base$suffix"
            suffix += 1
        }
        return copy(operationId = id)
    }

    internal fun operationIdFor(
        method: String,
        path: String,
    ): String {
        val suffix =
            path.trim('/')
                .split('/')
                .filter { it.isNotBlank() }
                .joinToString("") { segment ->
                    val name = segment.removeSurrounding("{", "}")
                    val capitalized =
                        name.split('-', '_', '.').joinToString("") { part ->
                            part.replaceFirstChar { it.uppercaseChar() }
                        }
                    if (segment.startsWith("{")) "By$capitalized" else capitalized
                }
        return method.lowercase() + suffix.replaceFirstChar { it.uppercaseChar() }
    }
}
