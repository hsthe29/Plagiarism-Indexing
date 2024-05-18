package hust.thehs.plugins.extractor

import hust.thehs.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripperByArea
import vn.pipeline.Annotation
import vn.pipeline.VnCoreNLP
import java.awt.geom.Rectangle2D
import java.io.File


@OptIn(ExperimentalSerializationApi::class)
private val PrettyJson = Json {
    prettyPrint = true
    prettyPrintIndent = "  "
}

class TextExtractor(
    top: CM = 0.0.cm,
    bottom: CM = 0.0.cm,
    left: CM = 0.0.cm,
    right: CM = 0.0.cm
) {
    private val A4_DPI_WIDTH = 21.0.cm.toDpiSize
    private val A4_DPI_HEIGHT = 29.7.cm.toDpiSize

    private val REGION_NAME = "page-content"

    private val pdfStripper = PDFTextStripperByArea()

    //    String originalUrl;
    private val vnUpper = "A-ZẮẰẲẴẶĂẤẦẨẪẬÂÁÀÃẢẠĐẾỀỂỄỆÊÉÈẺẼẸÍÌỈĨỊỐỒỔỖỘÔỚỜỞỠỢƠÓÒÕỎỌỨỪỬỮỰƯÚÙỦŨỤÝỲỶỸỴ"
    private val vnLower = "a-zắằẳẵặăấầẩẫậâáàãảạđếềểễệêéèẻẽẹíìỉĩịốồổỗộôớờởỡợơóòỏõọứừửữựưúùủũụýỳỷỹỵ"

    private val regexPreface =
        """\s+(LỜI NÓI ĐẦU|LỜI MỞ ĐẦU|MỞ ĐẦU|PHẦN MỞ ĐẦU|Lời nói đầu|Lời mở đầu|Mở đầu|Phần mở đầu)([^\S\n]*)\n"""
    private val regexAbbreviation = """\s+(DANH MỤC (TỪ|) VIẾT TẮT|Danh mục (từ|) viết tắt)([\s\S]+|)?\n"""
    private val regexListOfTables =
        """\s+(DANH MỤC (CÁC|) BẢNG|DANH SÁCH (CÁC|) BẢNG|Danh mục (các|) bảng|Danh sách (các|) bảng)([^\S\n]+|)\n"""
    private val regexListOfFigures =
        """\s+(DANH MỤC (CÁC|) HÌNH ẢNH|DANH SÁCH (CÁC|) HÌNH|Danh mục (các|) hình ảnh|Danh sách (các|) hình)([^\S\n]+|)\n"""
    private val regexThanks = """\s+(LỜI CẢM ƠN|Lời cảm ơn)([^\S\n]+|)\n"""
    private val regexAbstract = """\s+TÓM TẮT[\s\S]+?\n\s"""
    private val romanNumbers = arrayOf(
        "0", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII",
        "XIII", "XIV", "XV"
    )

    private val regexExtra = """\s+(Danh mục tài liệu tham khảo|DANH MỤC TÀI LIỆU THAM KHẢO|Tài liệu tham khảo|TÀI LIỆU THAM KHẢO)([^\S\n]*)\n"""

    private val regexSplitSentences = """([.!?]\s)|(\n{2,})|(:\s*\n)"""

    private lateinit var mainContentIndices: Pair<Int, Int>

    init {
        val width = A4_DPI_WIDTH.value - right.toDpiSize.value - left.toDpiSize.value
        val height = A4_DPI_HEIGHT.value - bottom.toDpiSize.value - top.toDpiSize.value

        pdfStripper.addRegion(
            this.REGION_NAME, Rectangle2D.Double(
                left.toDpiSize.value,
                top.toDpiSize.value,
                width,
                height
            )
        )
    }

    private fun regexChapter(chapter: Int) =
        """\s+(CHƯƠNG|Chương) ($chapter|${this.romanNumbers[chapter]})[^$vnLower$vnUpper]+"""

    private fun checkExtension(filePath: String) {
        assert(filePath.split('.').last() == "pdf") { "Please input PDF file" }
    }

    private fun createSavePath(filePath: String) = filePath.substring(0, filePath.length - 3) + "json"

    private fun indexPreamble(text: String, regex: String): Int {
        val match = regex.toRegex().find(text)
        return match?.range?.first ?: -1
    }

    private fun indexEndPart(text: String, regex : String): Int {
        val matches = regex.toRegex().findAll(text)
        if (matches.none())
            return text.length
        return matches.last().range.first
    }

    private fun findMainContent(text: String): String {
        var startIndex = indexPreamble(text, regexPreface)
        if (startIndex == -1)
            startIndex = indexPreamble(text, regexChapter(1))
        if (startIndex == -1) startIndex =
            indexPreamble(text, regexAbbreviation)
        if (startIndex == -1) startIndex =
            indexPreamble(text, regexListOfTables)
        if (startIndex == -1) startIndex =
            indexPreamble(text, regexListOfFigures)
        if (startIndex == -1) startIndex =
            indexPreamble(text, regexThanks)
        if (startIndex == -1) startIndex =
            indexPreamble(text, regexAbstract)
        if (startIndex == -1) startIndex = 0

        val endIndex = indexEndPart(text, regexExtra)
        mainContentIndices = startIndex to endIndex

        return text.substring(startIndex, endIndex)
    }

    private fun normalize(text: String): String {
        val normalizedText = text.replace(13.toChar().toString(), "")
        return normalizedText.replace("""\s+\n""".toRegex(), "\n")
    }

    private fun processContent(mainContent: String): List<String> {
        val sentences = mainContent.split(regexSplitSentences.toRegex())
        val processedSentences = sentences.map { s ->
                s.replace('\n', ' ').trim()
        }
        return processedSentences
    }

    private fun annotate(text: String): Sentence {
        val vnCoreNLP = VnCoreNLP(arrayOf("wseg", "pos"))
        val annotation = Annotation(text)
        try {
            vnCoreNLP.annotate(annotation)
        } catch (oom: OutOfMemoryError) {
            println("Error: OOM when annotate sentence!")
        }

        val words = mutableListOf<String>()
        val posTags = mutableListOf<String>()

        for (taggedWord in annotation.words) {
            val word = taggedWord.form
            val posTag = taggedWord.posTag

            words.add(word)
            posTags.add(posTag)
        }
        return Sentence(
            text = text,
            tokens = words,
            posTags = posTags
        )
    }

    private fun save(savePath: String, jsonData: DocumentData) {
        val str = PrettyJson.encodeToString(jsonData)
        File(savePath).writeText(str)
    }

    fun run(filePath: String) {
        this.checkExtension(filePath)
        val savePath = this.createSavePath(filePath)

        val file = File(filePath)
        val document = Loader.loadPDF(file)

        val textPages = mutableListOf<String>()

        for (page in document.pages) {
            pdfStripper.extractRegions(page)
            val text = pdfStripper.getTextForRegion(this.REGION_NAME)
            textPages.add(normalize(text))
        }

        val textDocument = textPages.joinToString("")
        val mainContent = findMainContent(textDocument)
        val sentenceContents = this.processContent(mainContent)

        val sentences = mutableListOf<Sentence>()
        for (text in sentenceContents) {
            if (text.isNotEmpty()) {
                sentences.add(annotate(text))
            }
        }
        val jsonData = DocumentData(
            url = "/",
            sentences = sentences
        )
        this.save(savePath, jsonData)
    }
}

private fun extractFolder(extractor: TextExtractor, directoryPath: String) {
    println("Index folder: $directoryPath")
    val (subDirs, files) = listDir(directoryPath)

    for (file in files) {
        if (file.endsWith(".pdf")) {
            println("Index file: $file")
            extractor.run(file)
        }
    }

    for (subDir in subDirs) {
        extractFolder(extractor, subDir)
    }
}

fun extract(directoryPath: String) {
    println("Extracting...")
    require(
        isInDirectory(
            ROOT_DATA_DIRECTORY,
            directoryPath
        )
    ) { "Input directory, must located in folder $ROOT_DATA_DIRECTORY" }
    val textExtractor = TextExtractor(
        top = MARGIN_TOP.cm,
        bottom = MARGIN_BOTTOM.cm,
        left = MARGIN_LEFT.cm,
        right = MARGIN_RIGHT.cm
    )
    extractFolder(textExtractor, directoryPath)
}

fun main() {
//    println(absolutePathOf("datn/datn_trandangtuyen_final_2.2m.pdf"))

    val textExtractor = TextExtractor(
        top = MARGIN_TOP.cm,
        bottom = MARGIN_BOTTOM.cm,
        left = MARGIN_LEFT.cm,
        right = MARGIN_RIGHT.cm
    )
    val files = File("D:\\CoopyData\\Test").listFiles()


    println(files.contentToString())
    files?.get(0)?.let { textExtractor.run(it.path) }
//    println(isInDirectory(ROOT_DATA_DIRECTORY, "D:/CoopyData/Test"))
//    textExtractor.run(absolutePathOf("datn/datn_trandangtuyen_final_2.2m.pdf"))

}