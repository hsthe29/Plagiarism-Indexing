package hust.thehs

import com.jillesvangurp.ktsearch.*
import com.jillesvangurp.searchdsls.querydsl.matchPhrase
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Date

private fun listDir(directoryPath: String): Pair<List <String>, List <String>> {
    val directory = File(directoryPath)
    assert(directory.isDirectory) { "Require Directory but found File instead!" }

    val subItems = directory.listFiles()
    val files = subItems?.filter { it.isFile }?.map{ it.path } ?: listOf()
    val subDirs = subItems?.filter { it.isDirectory }?.map { it.path } ?: listOf()
    return subDirs to files
}

suspend fun indexDocument(client: SearchClient,
                          data: DocumentData,
                          filePath: String,
                          universityId: Int,
                          category: Int,
                          url: String,
                          private: Boolean,
                          language: Language,
                          type: DocumentType
) {

    val indexName = DEFAULT_INDEX_NAME
    val res = client.search(indexName) {
        query = matchPhrase("file_path", filePath)
    }

    val recordId  = res.hits?.hits?.first()?.id

    val documentIndex = DocumentIndex(
        filePath = filePath,
        universityId = universityId,
        category = category,
        data = data,
        uploadTime = Date().time,
        url = url,
        private = private,
        language = language,
        type = type
    )

    if (recordId != null) {
        client.updateDocument(target = indexName,
            id = recordId, doc = documentIndex, refresh = Refresh.True)
    }
    else {
        client.indexDocument(indexName, document = documentIndex, opType = OperationType.Create, refresh = Refresh.True)
    }
}

suspend fun indexFolder(client: SearchClient, directoryPath: String, indexConfig: IndexConfig) {
    val (subDirs, files) = listDir(directoryPath)
    println("Indexing folder $directoryPath")
    for(file in files) {
        if (file.endsWith(".json")) {
            val jsonString = File(file).readText(Charsets.UTF_8)

            val data = Json.decodeFromString<DocumentData>(jsonString)

            indexDocument(
                client=client,
                data=data,
                filePath=file,
                universityId=indexConfig.universityId,
                category=indexConfig.category,
                url=indexConfig.url,
                private=indexConfig.private,
                language=when (indexConfig.language){
                    "vi" -> Language.VI
                    "en" -> Language.EN
                    else -> throw Exception("Not found language ${indexConfig.language}")},
                type=when (indexConfig.type){
                    0 -> DocumentType.THESIS
                    1 -> DocumentType.PAPER
                    2 -> DocumentType.INTERNET
                    3 -> DocumentType.PROPOSAL
                    else -> throw Exception("Not found document type ${indexConfig.type}")}
            )
        }
    }
    for(subDir in subDirs) {
        indexFolder(client, subDir, indexConfig)
    }
}

suspend fun index(directoryPath: String) {
    val client = getClient()
    indexFolder(client, directoryPath, IndexConfig(0, 0, "", true, "vi", 0))
}


suspend fun main() {
    index("")
}