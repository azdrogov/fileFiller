import org.apache.poi.sl.usermodel.TextParagraph
import org.odftoolkit.odfdom.doc.{OdfDocument, OdfTextDocument}
import org.odftoolkit.odfdom.incubator.search.TextNavigation
import org.odftoolkit.simple.ChartDocument.OdfMediaType
import org.odftoolkit.simple.TextDocument
import org.odftoolkit.simple.text.Paragraph
import org.w3c.dom.NodeList

import scala.jdk.CollectionConverters._


object Main {
  def main(args: Array[String]): Unit = {
    val inputFile = "/home/azdrogov/template.odt"
    val outputFile = "/home/azdrogov/output.odt"
    val variables = Map("{{name}}" -> "Макс", "{{animal}}" -> "Обезьяной")

    // Загрузка документа
    val textDocument = TextDocument.loadDocument(inputFile)

    // Получение всего текста документа
    val paragraphs = textDocument.getParagraphIterator.asScala

    for (paragraph <- paragraphs) {
      var textContent = paragraph.getOdfElement.getTextContent
      for ((placeholder, replacement) <- variables) {
        textContent = textContent.replace(placeholder, replacement)
      }
      paragraph.getOdfElement.setTextContent(textContent)
    }

    textDocument.save(outputFile)
  }

}
























