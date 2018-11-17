package com.gnefedev.backend

import com.itextpdf.html2pdf.ConverterProperties
import com.itextpdf.html2pdf.HtmlConverter
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.sun.xml.internal.messaging.saaj.util.ByteOutputStream
import org.intellij.lang.annotations.Language
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

fun main(args: Array<String>) {
    SpringApplication.run(Application::class.java)
}

@SpringBootApplication
class Application

@RestController
class PdfController {
    @GetMapping("/api/pdf/test")
    fun test() = "test"

    @PostMapping("/api/pdf/print")
    fun printTable(@RequestBody html: String): Resource = ByteOutputStream()
        .use { outputStream ->
            writePdf(outputStream, html)
            val result = InputStreamResource(outputStream.newInputStream())
            outputStream.flush()
            result
        }

    fun writePdf(out: ByteOutputStream, html: String) {
        val writer = PdfWriter(out)
        val pdfDoc = PdfDocument(writer)

        //Set the result to be tagged
        pdfDoc.setTagged()
        pdfDoc.defaultPageSize = PageSize.A4.rotate()

        val converterProperties = ConverterProperties()

        @Language("HTML")
        val fullHtml = """
            <!DOCTYPE html>
<html lang="en" xmlns:âge-break-after="http://www.w3.org/1999/xhtml">
<head>
    <meta charset="UTF-8">
    <title>pdfHTML Accessibility example</title>
    <style>
        body {
            font-size: 8px;
        }
    </style>
</head>
<body>$html</body>
</html>
        """.trimIndent()

        //Create acroforms from text and button input fields
        converterProperties.isCreateAcroForm = true

        HtmlConverter.convertToPdf(fullHtml, pdfDoc, converterProperties)
        pdfDoc.close()
    }
}
