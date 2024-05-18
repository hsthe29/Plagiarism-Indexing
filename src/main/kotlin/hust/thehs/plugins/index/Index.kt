package hust.thehs.plugins.index

import com.jillesvangurp.ktsearch.*
import com.jillesvangurp.searchdsls.querydsl.term
import hust.thehs.*
import kotlinx.serialization.json.Json
import java.io.File
import java.util.Date


suspend fun indexDocument(client: SearchClient,
                          data: DocumentData,
                          filePath: String,
                          universityId: Int,
                          category: Int,
                          url: String,
                          private: Boolean,
                          language: String,
                          type: Int
) {

    val indexName = DEFAULT_INDEX_NAME
    val res = client.search(indexName) {
        query = term("file_path", filePath)
    }

    val recordId = if (res.hits!!.total!!.value > 0)
        res.hits?.hits?.first()?.id
    else
        null

    val analyzeText = data.sentences.joinToString("\n")
    val documentIndex = DocumentIndex(
        filePath = filePath,
        universityId = universityId,
        category = category,
        text = analyzeText,
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
        val newId = filePath.split(File.separator).last().dropLast(4)
        client.indexDocument(indexName, document = documentIndex, id = newId, opType = OperationType.Create, refresh = Refresh.True)
    }
}

suspend fun indexFolder(client: SearchClient, directoryPath: String, indexConfig: IndexConfig) {
    val (subDirs, files) = listDir(directoryPath)
    println("Indexing folder $directoryPath")
    for(file in files) {
        if (file.endsWith(".json")) {
            println("Indexing file: $file")
            val jsonString = File(file).readText(Charsets.UTF_8)

            val data = Json.decodeFromString<DocumentData>(jsonString)

            indexDocument(
                client=client,
                data=data,
                filePath=file,
                universityId=indexConfig.universityId,
                category=indexConfig.category,
                url=data.url,
                private=indexConfig.private,
                language=indexConfig.language,
                type=indexConfig.type
            )
        }
    }
    for(subDir in subDirs) {
        indexFolder(client, subDir, indexConfig)
    }
}

suspend fun index(directoryPath: String, config: IndexConfig) {
    println("Indexing...")
    val client = getClient()
    indexFolder(client, directoryPath, config)
}
