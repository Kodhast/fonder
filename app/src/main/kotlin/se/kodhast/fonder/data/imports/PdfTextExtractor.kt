package se.kodhast.fonder.data.imports

/**
 * Extraherar textinnehållet ur en PDF-fil. Abstraherat bort från PDFBox-implementationen så
 * [AvrakningsnotaPdfParser]s anropare kan enhetstestas med en fejk, utan det tunga
 * PDF-biblioteket (samma princip som [se.kodhast.fonder.data.network.FondlistaHtmlSource]).
 */
fun interface PdfTextExtractor {
    fun extractText(bytes: ByteArray): String
}
