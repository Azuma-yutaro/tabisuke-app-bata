package com.example.tabisuke.utils

import android.content.Context
import android.graphics.pdf.PdfDocument
import android.graphics.Paint
import android.graphics.Color
import android.os.Environment
import com.example.tabisuke.ui.main.Event
import com.example.tabisuke.ui.scheduledetail.Schedule
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object PdfGenerator {

    fun generatePdf(context: Context, event: Event, schedules: List<Schedule>, fileName: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        paint.color = Color.BLACK
        paint.textSize = 24f

        var yPos = 40f

        // Event Title
        canvas.drawText(event.title, 40f, yPos, paint)
        yPos += 30f

        // Event Description
        if (event.description.isNotBlank()) {
            paint.textSize = 16f
            canvas.drawText("説明: ${event.description}", 40f, yPos, paint)
            yPos += 30f
        }

        // Event Period
        if (event.startDate.isNotBlank() || event.endDate.isNotBlank()) {
            canvas.drawText("期間: ${event.startDate} - ${event.endDate}", 40f, yPos, paint)
            yPos += 30f
        }

        // Schedules
        paint.textSize = 16f
        schedules.groupBy { it.date }.toSortedMap().forEach { (date, dailySchedules) ->
            canvas.drawText("\n日付: $date", 40f, yPos, paint)
            yPos += 20f
            dailySchedules.sortedBy { it.time }.forEach { schedule ->
                canvas.drawText("  ${schedule.time} - ${schedule.title} (${schedule.budget}円)", 60f, yPos, paint)
                yPos += 20f
                if (schedule.url.isNotBlank()) {
                    canvas.drawText("    URL: ${schedule.url}", 80f, yPos, paint)
                    yPos += 20f
                }
            }
        }

        pdfDocument.finishPage(page)

        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)

        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }
}