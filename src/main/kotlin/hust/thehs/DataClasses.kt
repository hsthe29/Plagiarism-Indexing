package hust.thehs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
enum class Language(val lang: String){
    @SerialName("vi") VI("vi"),
    @SerialName("en") EN("en")
}

// thesis = 0, paper = 1, internet = 2, proposal = 3
@Serializable
enum class DocumentType(val type: Int) {
    @SerialName("thesis") THESIS(0),
    @SerialName("paper") PAPER(1),
    @SerialName("internet") INTERNET(2),
    @SerialName("proposal") PROPOSAL(3)
}

@Serializable
data class DocumentData(
    val url: String = "/",
    val sentences: List<Sentence>
)

@Serializable
data class DocumentIndex(
    @SerialName("file_path") val filePath: String,
    @SerialName("university_id") val universityId: Int,
    val category: Int,
    val text: String,
    val data: DocumentData,
    @SerialName("upload_time") val uploadTime: Long,
    val url: String,
    val private: Boolean,
    val language: String,
    val type: Int
)

@Serializable
data class IndexConfig(
    @SerialName("directory_path")
    val directoryPath: String,
    val extract: Boolean,
    @SerialName("university_id")
    val universityId: Int,
    val category: Int,
    val private: Boolean,
    val language: String,
    val type: Int
)

@Serializable
data class SearchConfig(
    @SerialName("num_files")
    val numFiles: Int = 0,
    val keywords: List<String> = listOf(),
    val language: String = ""
)

@Serializable
data class Hit(var score: Double = 0.0,
               @SerialName("file_path")
               var filePath: String = "",
               @SerialName("university_id")
               var universityId: Int = -1,
               var category: Int = -1,
               var data: DocumentData,
               @SerialName("upload_time")
               var uploadTime: Long = -1L,
               var url: String = "",
               var private: Boolean = false,
               var language: String = "",
               var type: Int = - 1)

@Serializable
data class RespondJson(
    val hits: List<Hit>,
    val status: Int
)

/* *********************** Text Extractor *********************** */
@Serializable
data class Sentence(
    val text: String,
    val tokens: List<String>,
    @SerialName("pos_tags")
    val posTags: List<String>)

/* *************************************************************** */