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
    val text: String
)

@Serializable
data class DocumentIndex(
    @SerialName("file_path") val filePath: String,
    @SerialName("university_id") val universityId: Int,
    val category: Int,
    val data: DocumentData,
    @SerialName("upload_time") val uploadTime: Long,
    val url: String,
    val private: Boolean,
    val language: Language,
    val type: DocumentType
)

data class IndexConfig(
    val universityId: Int,
    val category: Int,
    val url: String,
    val private: Boolean,
    val language: String,
    val type: Int
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