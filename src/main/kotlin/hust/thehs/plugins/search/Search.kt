package hust.thehs.plugins.search

import com.jillesvangurp.ktsearch.SearchClient
import com.jillesvangurp.ktsearch.search
import com.jillesvangurp.searchdsls.querydsl.bool
import com.jillesvangurp.searchdsls.querydsl.match
import hust.thehs.DEFAULT_INDEX_NAME
import hust.thehs.Hit
import hust.thehs.RespondJson
import hust.thehs.SearchConfig
import kotlinx.serialization.json.Json
import kotlin.time.Duration.Companion.seconds


val JsonMapper = Json { ignoreUnknownKeys = true }


suspend fun search(client: SearchClient, searchConfig: SearchConfig): RespondJson {
    val numFiles = searchConfig.numFiles
    val keywords = searchConfig.keywords
    val res = when(val language = searchConfig.language) {
        "vi" -> {
            client.search(target = DEFAULT_INDEX_NAME, size = numFiles, timeout = 0.5.seconds) {
                query = bool {
                    must(
                        match("language", language)
                    )

                    should(
                        keywords.map { match("text", it) }
                    )
                }
            }
        }
        else -> {
            TODO("Not Implemented Error")
        }
    }

    return RespondJson(
        hits = res.hits?.hits?.map {
            JsonMapper.decodeFromString<Hit>(it.source.toString())
                .apply {
                    score = it.score ?: 0.0
                }
        }?.toList() ?: listOf(),
        status = 200
    )
}