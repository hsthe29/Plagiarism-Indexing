package hust.thehs.plugins

import com.jillesvangurp.ktsearch.deleteDocument
import com.jillesvangurp.ktsearch.search
import com.jillesvangurp.searchdsls.querydsl.matchPhrase
import hust.thehs.*
import hust.thehs.plugins.extractor.extract
import hust.thehs.plugins.index.index
import hust.thehs.plugins.search.search
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.Resources
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json


fun Application.configureRouting() {
    val client = getClient()

    install(Resources)

    routing {
        get("/") {
            call.respondText("""{"name": "Plagiarism Indexing Server", "version": "1.0.0"}""")
        }

        post("/search") {
            val searchConfig = call.receive<SearchConfig>()
            val respondJson = search(client, searchConfig)
            call.respondText(Json.encodeToString(respondJson))
        }

        post("/index") {
            val indexConfig = call.receive<IndexConfig>()
            println("Index config: $indexConfig")
            if (indexConfig.extract) {
                extract(indexConfig.directoryPath)
            }
            index(indexConfig.directoryPath, indexConfig)
            call.respondText(indexConfig.toString())
        }

        get("/remove") {
            val queryParameters = call.request.queryParameters
            val filePath = queryParameters["file_path"] ?: ""

            val res = client.search(target = DEFAULT_INDEX_NAME) {
                query = matchPhrase("file_path", filePath)
            }

            val hits = res.hits?.hits ?: listOf()
            for(hit in hits) {
                client.deleteDocument(target = "documents", id = hit.id)
                println("Deleted document id = ${hit.id}")
            }
        }
    }
}
