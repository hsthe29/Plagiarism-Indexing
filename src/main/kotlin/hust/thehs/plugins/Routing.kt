package hust.thehs.plugins

import com.jillesvangurp.ktsearch.search
import com.jillesvangurp.searchdsls.querydsl.bool
import com.jillesvangurp.searchdsls.querydsl.match
import hust.thehs.*
import io.ktor.http.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.html.InputType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds


fun Application.configureRouting() {
    val client = getClient()
    val indexName = DEFAULT_INDEX_NAME
    val JsonMapper = Json { ignoreUnknownKeys = true }


    install(Resources)
    routing {
        get("/") {
//            call.respondText("Hello World!")
        }
        get<Articles> { article ->
            // Get all articles ...
            call.respond("List of articles sorted starting from ${article.sort}")
        }

        get("/search") {
            val queryParameters = call.request.queryParameters
            val numFiles = queryParameters["numberFile"]?.toInt() ?: 0
            val keywords = queryParameters["keyword"]?.split(",") ?: listOf()
            val language = when(queryParameters["language"]) {
                "vi" -> Language.VI
                "en" -> Language.EN
                else -> throw Exception("Language ${queryParameters["language"]} is not valid or not supported")
            }
            val res = when(language) {
                Language.VI -> {
                    client.search(target = indexName, size = numFiles, timeout = 0.5.seconds) {
                        query = bool {
                            must(
                                match("language", language.lang)
                            )
                            should(
                                keywords.map { match("data", it) }
                            )
                        }
                    }
                }
                Language.EN -> {
                    client.search(target = indexName, size = numFiles, timeout = 0.5.seconds) {
                        query = bool {
                            should(
                                keywords.map { match("data", it) }
                            )
                        }
                    }

                }
            }

            val returnJson = RespondJson(
                hits = res.hits?.hits?.map {
                    JsonMapper.decodeFromString<Hit>(it.source.toString())
                        .apply {
                            score = it.score ?: 0.0
                        }
                }?.toList() ?: listOf(),
                status = 200
            )

            call.respondText(Json.encodeToString(returnJson))
        }

        post("/index") {

        }
    }
}

@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
