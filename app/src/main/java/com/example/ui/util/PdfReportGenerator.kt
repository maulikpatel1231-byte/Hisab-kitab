package com.example.ui.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.Project
import com.example.data.model.Transaction
import com.example.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

object PdfReportGenerator {

    private const val PAGE_WIDTH = 595 // Standard A4 width in points
    private const val PAGE_HEIGHT = 842 // Standard A4 height in points
    private const val MARGIN = 36f

    fun generateAndSharePdf(
        context: Context,
        project: Project?,
        transactions: List<Transaction>
    ): File? {
        try {
            val pdfDoc = PdfDocument()

            val titlePaint = Paint().apply {
                color = Color.rgb(15, 118, 110) // Deep Teal
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.rgb(71, 85, 105)
                textSize = 10f
                isAntiAlias = true
            }

            val headerBgPaint = Paint().apply {
                color = Color.rgb(241, 245, 249)
                style = Paint.Style.FILL
            }

            val textBoldPaint = Paint().apply {
                color = Color.rgb(15, 23, 42)
                textSize = 10f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val textRegularPaint = Paint().apply {
                color = Color.rgb(51, 65, 85)
                textSize = 9f
                isAntiAlias = true
            }

            val notePaint = Paint().apply {
                color = Color.rgb(100, 116, 139)
                textSize = 8f
                isAntiAlias = true
            }

            val greenPaint = Paint().apply {
                color = Color.rgb(22, 163, 74) // Green
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val redPaint = Paint().apply {
                color = Color.rgb(220, 38, 38) // Red
                textSize = 9.5f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.rgb(226, 232, 240)
                strokeWidth = 0.8f
            }

            val totalIn = transactions.filter { it.type == TransactionType.INCOMING.name }.sumOf { it.amount }
            val totalOut = transactions.filter { it.type == TransactionType.OUTGOING.name }.sumOf { it.amount }
            val netBalance = totalIn - totalOut

            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            val dateShortFormat = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            val nowFormatted = dateFormat.format(Date())

            val projectName = project?.displayName ?: "Hisab Kitab"
            val ownerEmail = project?.ownerEmail ?: ""

            // Calculate pagination
            // Page 1 has header banner + 3 summary boxes + table
            // Page 1 can fit ~18 rows; subsequent pages can fit ~26 rows
            val firstPageRows = 18
            val otherPageRows = 26
            val totalTransactions = transactions.size
            val remainingAfterFirst = max(0, totalTransactions - firstPageRows)
            val additionalPages = if (remainingAfterFirst == 0) 0 else ((remainingAfterFirst + otherPageRows - 1) / otherPageRows)
            val totalPages = 1 + additionalPages

            var txIndex = 0

            for (pageNumber in 1..totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                var currentY = MARGIN

                // Top Header Branding (Every page)
                canvas.drawText("HISAB KITAB", MARGIN, currentY + 16f, titlePaint)
                val pageIndicator = "Page $pageNumber of $totalPages"
                canvas.drawText(pageIndicator, PAGE_WIDTH - MARGIN - 70f, currentY + 14f, subtitlePaint)

                currentY += 24f
                canvas.drawText("Project: $projectName   |   Generated: $nowFormatted", MARGIN, currentY + 8f, subtitlePaint)
                if (ownerEmail.isNotBlank()) {
                    currentY += 12f
                    canvas.drawText("Account: $ownerEmail", MARGIN, currentY + 6f, subtitlePaint)
                }

                currentY += 16f
                canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)
                currentY += 12f

                // On Page 1: Render 3 Summary Metric Cards
                if (pageNumber == 1) {
                    val boxWidth = (PAGE_WIDTH - (MARGIN * 2) - 16f) / 3f
                    val boxHeight = 46f

                    // 1. Total Cash In Card
                    val inBox = RectF(MARGIN, currentY, MARGIN + boxWidth, currentY + boxHeight)
                    val inBg = Paint().apply { color = Color.rgb(240, 253, 244); style = Paint.Style.FILL }
                    canvas.drawRoundRect(inBox, 6f, 6f, inBg)
                    canvas.drawText("TOTAL CASH IN (+)", inBox.left + 10f, inBox.top + 16f, Paint().apply {
                        color = Color.rgb(22, 101, 52); textSize = 8f; isFakeBoldText = true; isAntiAlias = true
                    })
                    canvas.drawText(FormatUtils.formatCurrency(totalIn), inBox.left + 10f, inBox.top + 34f, Paint().apply {
                        color = Color.rgb(22, 163, 74); textSize = 13f; isFakeBoldText = true; isAntiAlias = true
                    })

                    // 2. Total Cash Out Card
                    val outLeft = MARGIN + boxWidth + 8f
                    val outBox = RectF(outLeft, currentY, outLeft + boxWidth, currentY + boxHeight)
                    val outBg = Paint().apply { color = Color.rgb(254, 242, 242); style = Paint.Style.FILL }
                    canvas.drawRoundRect(outBox, 6f, 6f, outBg)
                    canvas.drawText("TOTAL CASH OUT (-)", outBox.left + 10f, outBox.top + 16f, Paint().apply {
                        color = Color.rgb(153, 27, 27); textSize = 8f; isFakeBoldText = true; isAntiAlias = true
                    })
                    canvas.drawText(FormatUtils.formatCurrency(totalOut), outBox.left + 10f, outBox.top + 34f, Paint().apply {
                        color = Color.rgb(220, 38, 38); textSize = 13f; isFakeBoldText = true; isAntiAlias = true
                    })

                    // 3. Net Balance Card
                    val netLeft = outLeft + boxWidth + 8f
                    val netBox = RectF(netLeft, currentY, netLeft + boxWidth, currentY + boxHeight)
                    val netBg = Paint().apply { color = Color.rgb(240, 253, 250); style = Paint.Style.FILL }
                    canvas.drawRoundRect(netBox, 6f, 6f, netBg)
                    canvas.drawText("NET CASH BALANCE", netBox.left + 10f, netBox.top + 16f, Paint().apply {
                        color = Color.rgb(15, 118, 110); textSize = 8f; isFakeBoldText = true; isAntiAlias = true
                    })
                    val netColor = if (netBalance >= 0) Color.rgb(15, 118, 110) else Color.rgb(220, 38, 38)
                    canvas.drawText(FormatUtils.formatCurrency(netBalance), netBox.left + 10f, netBox.top + 34f, Paint().apply {
                        color = netColor; textSize = 13f; isFakeBoldText = true; isAntiAlias = true
                    })

                    currentY += boxHeight + 16f
                }

                // Table Header
                val tableHeaderHeight = 22f
                val headerRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + tableHeaderHeight)
                canvas.drawRect(headerRect, headerBgPaint)

                // Columns layout:
                // # (24), Date (90), Description/Party (220), Mode (60), Type (40), Amount (remaining)
                val colNum = MARGIN + 6f
                val colDate = MARGIN + 28f
                val colDesc = MARGIN + 120f
                val colMode = PAGE_WIDTH - MARGIN - 170f
                val colType = PAGE_WIDTH - MARGIN - 105f
                val colAmount = PAGE_WIDTH - MARGIN - 55f

                val headerTextY = currentY + 15f
                canvas.drawText("#", colNum, headerTextY, textBoldPaint)
                canvas.drawText("DATE & TIME", colDate, headerTextY, textBoldPaint)
                canvas.drawText("DESCRIPTION / PARTY", colDesc, headerTextY, textBoldPaint)
                canvas.drawText("MODE", colMode, headerTextY, textBoldPaint)
                canvas.drawText("TYPE", colType, headerTextY, textBoldPaint)
                canvas.drawText("AMOUNT", colAmount, headerTextY, textBoldPaint)

                currentY += tableHeaderHeight

                val rowsToRender = if (pageNumber == 1) firstPageRows else otherPageRows
                var rowsRendered = 0

                while (txIndex < transactions.size && rowsRendered < rowsToRender) {
                    val tx = transactions[txIndex]
                    val isIncoming = tx.type == TransactionType.INCOMING.name
                    val rowHeight = 24f

                    // Alternating row background
                    if (rowsRendered % 2 == 1) {
                        val rowRect = RectF(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY + rowHeight)
                        val altBg = Paint().apply { color = Color.rgb(248, 250, 252); style = Paint.Style.FILL }
                        canvas.drawRect(rowRect, altBg)
                    }

                    val rowTextY = currentY + 14f

                    // 1. Serial Number
                    canvas.drawText("${txIndex + 1}", colNum, rowTextY, textRegularPaint)

                    // 2. Date
                    val txDateStr = dateShortFormat.format(Date(tx.timestamp))
                    canvas.drawText(txDateStr, colDate, rowTextY, textRegularPaint)

                    // 3. Description & Party
                    val desc = buildString {
                        if (!tx.personName.isNullOrBlank()) {
                            val prefix = if (isIncoming) "From: " else "To: "
                            append(prefix)
                            append(tx.personName)
                            if (tx.category.isNotBlank() && tx.category != "General") {
                                append(" • ${tx.category}")
                            }
                        } else {
                            append(tx.category)
                        }
                    }
                    val truncatedDesc = if (desc.length > 34) desc.take(32) + ".." else desc
                    canvas.drawText(truncatedDesc, colDesc, rowTextY, textBoldPaint)

                    // Optional Note underneath if space
                    if (tx.note.isNotBlank()) {
                        val truncatedNote = if (tx.note.length > 36) tx.note.take(34) + ".." else tx.note
                        canvas.drawText("Note: $truncatedNote", colDesc, currentY + 22f, notePaint)
                    }

                    // 4. Payment Mode
                    canvas.drawText(tx.paymentMode, colMode, rowTextY, textRegularPaint)

                    // 5. Type (IN / OUT)
                    if (isIncoming) {
                        canvas.drawText("IN", colType, rowTextY, greenPaint)
                    } else {
                        canvas.drawText("OUT", colType, rowTextY, redPaint)
                    }

                    // 6. Amount
                    val amountText = (if (isIncoming) "+" else "-") + FormatUtils.formatCurrency(tx.amount)
                    val paintToUse = if (isIncoming) greenPaint else redPaint
                    canvas.drawText(amountText, colAmount, rowTextY, paintToUse)

                    currentY += rowHeight
                    canvas.drawLine(MARGIN, currentY, PAGE_WIDTH - MARGIN, currentY, linePaint)

                    txIndex++
                    rowsRendered++
                }

                // If no entries at all
                if (transactions.isEmpty() && pageNumber == 1) {
                    canvas.drawText(
                        "No transactions recorded yet in this project.",
                        MARGIN + 120f,
                        currentY + 30f,
                        subtitlePaint
                    )
                }

                // Page Footer
                val footerY = PAGE_HEIGHT - MARGIN + 10f
                canvas.drawLine(MARGIN, footerY - 14f, PAGE_WIDTH - MARGIN, footerY - 14f, linePaint)
                canvas.drawText("Hisab Kitab - Smart Digital Ledger App", MARGIN, footerY, subtitlePaint)
                canvas.drawText(
                    "Total Entries: ${transactions.size}",
                    PAGE_WIDTH - MARGIN - 90f,
                    footerY,
                    subtitlePaint
                )

                pdfDoc.finishPage(page)
            }

            // Save PDF File to Cache
            val cacheDir = context.cacheDir
            val fileName = "HisabKitab_Statement_${System.currentTimeMillis()}.pdf"
            val file = File(cacheDir, fileName)
            val outputStream = FileOutputStream(file)
            pdfDoc.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDoc.close()

            // Open Android Share Sheet
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Hisab Kitab - Transaction Statement")
                putExtra(
                    Intent.EXTRA_TEXT,
                    "Attached is the transaction statement of ${project?.name ?: "Hisab Kitab"} with ${transactions.size} entries.\nTotal In: ${FormatUtils.formatCurrency(totalIn)}\nTotal Out: ${FormatUtils.formatCurrency(totalOut)}\nNet Balance: ${FormatUtils.formatCurrency(netBalance)}"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Hisab Kitab PDF via"))
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
            return null
        }
    }
}
