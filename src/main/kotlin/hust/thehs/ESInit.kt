package hust.thehs

import com.jillesvangurp.ktsearch.KtorRestClient
import com.jillesvangurp.ktsearch.SearchClient
import com.jillesvangurp.ktsearch.createIndex
import com.jillesvangurp.ktsearch.exists
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds


fun getClient(): SearchClient {
    val elasticSearch = ElasticSearch
    return elasticSearch.client
}

/* ElasticSearch Client Singleton */
private object ElasticSearch {
    var client: SearchClient
        private set
    init {
        runBlocking {
            val indexName = DEFAULT_INDEX_NAME

            client = SearchClient(
                KtorRestClient(
                    host = HOST,
                    port = PORT,
                    user = USER,
                    password = PASSWORD,
                    https = USE_HTTPS
                )
            )

            if (!client.exists(indexName)) {
                println("Index $indexName does not exist. Creating...")
                client.createIndex(indexName) {
                    settings {
                        replicas = 1
                        shards = 1
                        refreshInterval = 10.seconds
                    }
                    mappings(dynamicEnabled = true) {
                        keyword("file_path")
                        number<Int>("university_id")
                        keyword("category")
                        text("data")
                        number<Long>("upload_time")
                        keyword("url")
                        bool("private")
                        keyword("language")
                        number<Int>("type")
                    }
                }
            } else {
                println("Index $indexName is already exist.")
            }
            val engineInfo = client.engineInfo()
            println(engineInfo.variantInfo.variant.name + ":" + engineInfo.version.number)
        }
    }
}