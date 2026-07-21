package com.example.studyreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class DocumentImportResult(
  val text: String,
  val fileName: String,
  val truncated: Boolean,
)

interface DocumentTextExtractor : AutoCloseable {
  suspend fun extract(uri: Uri, onProgress: (current: Int, total: Int) -> Unit = { _, _ -> }): DocumentImportResult

  override fun close() = Unit
}

class AndroidDocumentTextExtractor(context: Context) : DocumentTextExtractor {
  private val appContext = context.applicationContext
  private val recognizer: TextRecognizer =
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

  override suspend fun extract(uri: Uri, onProgress: (Int, Int) -> Unit): DocumentImportResult =
    withContext(Dispatchers.IO) {
      val fileName = queryFileName(uri)
      val mimeType = appContext.contentResolver.getType(uri).orEmpty()
      when {
        mimeType == PDF_MIME || fileName.endsWith(".pdf", ignoreCase = true) ->
          extractPdf(uri, fileName, onProgress)

        mimeType == DOCX_MIME || fileName.endsWith(".docx", ignoreCase = true) ->
          extractDocx(uri, fileName)

        mimeType.startsWith("text/") || fileName.endsWith(".txt", ignoreCase = true) ->
          extractPlainText(uri, fileName)

        else -> throw IllegalArgumentException("Choose a PDF, DOCX, or TXT file.")
      }
    }

  private suspend fun extractPdf(
    uri: Uri,
    fileName: String,
    onProgress: (Int, Int) -> Unit,
  ): DocumentImportResult {
    val descriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
      ?: throw IllegalStateException("The PDF could not be opened.")
    descriptor.use { parcelDescriptor ->
      PdfRenderer(parcelDescriptor).use { renderer ->
        val pageCount = minOf(renderer.pageCount, MAX_PDF_PAGES)
        val pageText = mutableListOf<String>()
        for (pageIndex in 0 until pageCount) {
          onProgress(pageIndex + 1, pageCount)
          renderer.openPage(pageIndex).use { page ->
            val scale =
              (MAX_PDF_BITMAP_WIDTH.toFloat() / page.width.coerceAtLeast(1)).coerceIn(0.1f, 3f)
            val width = (page.width * scale).toInt().coerceAtLeast(1)
            val height = (page.height * scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            try {
              bitmap.eraseColor(Color.WHITE)
              page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
              val text = recognizer.process(InputImage.fromBitmap(bitmap, 0)).awaitResult().text.trim()
              if (text.isNotBlank()) pageText += text
            } finally {
              bitmap.recycle()
            }
          }
        }
        val text = pageText.joinToString("\n\n").take(MAX_IMPORTED_CHARACTERS)
        return DocumentImportResult(
          text = text,
          fileName = fileName,
          truncated = renderer.pageCount > pageCount || text.length >= MAX_IMPORTED_CHARACTERS,
        )
      }
    }
  }

  private fun extractDocx(uri: Uri, fileName: String): DocumentImportResult {
    val input = appContext.contentResolver.openInputStream(uri)
      ?: throw IllegalStateException("The document could not be opened.")
    val text = input.use(::extractDocxText).take(MAX_IMPORTED_CHARACTERS)
    return DocumentImportResult(text, fileName, text.length >= MAX_IMPORTED_CHARACTERS)
  }

  private fun extractPlainText(uri: Uri, fileName: String): DocumentImportResult {
    val input = appContext.contentResolver.openInputStream(uri)
      ?: throw IllegalStateException("The text file could not be opened.")
    val text = input.bufferedReader(Charsets.UTF_8).use { reader ->
      val buffer = CharArray(8_192)
      val output = StringBuilder()
      while (output.length < MAX_IMPORTED_CHARACTERS) {
        val count = reader.read(buffer, 0, minOf(buffer.size, MAX_IMPORTED_CHARACTERS - output.length))
        if (count < 0) break
        output.append(buffer, 0, count)
      }
      output.toString().trim()
    }
    return DocumentImportResult(text, fileName, text.length >= MAX_IMPORTED_CHARACTERS)
  }

  private fun queryFileName(uri: Uri): String {
    appContext.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (cursor.moveToFirst()) return cursor.getString(0).orEmpty().ifBlank { "Document" }
    }
    return uri.lastPathSegment?.substringAfterLast('/') ?: "Document"
  }

  override fun close() {
    recognizer.close()
  }
}

internal fun extractDocxText(input: InputStream): String {
  val documentXml =
    ZipInputStream(input).use { zip ->
      while (true) {
        val entry = zip.nextEntry ?: break
        if (entry.name == "word/document.xml") return@use zip.readBytesLimited(MAX_DOCX_XML_BYTES)
      }
      throw IllegalArgumentException("This DOCX file does not contain a readable document.")
    }

  val factory = DocumentBuilderFactory.newInstance().apply {
    isNamespaceAware = true
    setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    setFeature("http://xml.org/sax/features/external-general-entities", false)
    setFeature("http://xml.org/sax/features/external-parameter-entities", false)
  }
  val document = factory.newDocumentBuilder().parse(ByteArrayInputStream(documentXml))
  val paragraphs = document.getElementsByTagNameNS(WORDPROCESSING_NAMESPACE, "p")
  return buildString {
    for (paragraphIndex in 0 until paragraphs.length) {
      val textNodes = (paragraphs.item(paragraphIndex)).childNodes
      val paragraph = StringBuilder()
      fun collectText(node: org.w3c.dom.Node) {
        if (node.localName == "t") paragraph.append(node.textContent)
        if (node.localName == "tab") paragraph.append('\t')
        for (index in 0 until node.childNodes.length) collectText(node.childNodes.item(index))
      }
      for (index in 0 until textNodes.length) collectText(textNodes.item(index))
      paragraph.toString().trim().takeIf(String::isNotBlank)?.let {
        if (isNotEmpty()) append('\n')
        append(it)
      }
    }
  }
}

private fun InputStream.readBytesLimited(limit: Int): ByteArray {
  val output = java.io.ByteArrayOutputStream()
  val buffer = ByteArray(8_192)
  var total = 0
  while (true) {
    val count = read(buffer)
    if (count < 0) break
    total += count
    require(total <= limit) { "The DOCX document is too large to import safely." }
    output.write(buffer, 0, count)
  }
  return output.toByteArray()
}

private const val PDF_MIME = "application/pdf"
private const val DOCX_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
private const val WORDPROCESSING_NAMESPACE = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"
private const val MAX_PDF_PAGES = 60
private const val MAX_PDF_BITMAP_WIDTH = 1_600
private const val MAX_IMPORTED_CHARACTERS = 250_000
private const val MAX_DOCX_XML_BYTES = 12_000_000
