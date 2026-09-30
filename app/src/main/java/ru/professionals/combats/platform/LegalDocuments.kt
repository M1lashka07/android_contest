package ru.professionals.combats.platform

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File

/** Opens publisher-provided documents; never substitutes a fabricated legal policy. */
fun openLegal(context: Context, kind: String) {
    val asset = if (kind == "privacy") "privacy.pdf" else "terms.pdf"
    try {
        if (context.assets.list("legal")?.contains(asset) != true) {
            AlertDialog.Builder(context).setTitle(if (kind == "privacy") "Privacy policy" else "User agreement")
                .setMessage("The document has not been supplied by the publisher yet. This training build is not ready for public distribution.")
                .setPositiveButton("OK", null).show()
            return
        }
        val file = File(context.cacheDir, "shared/$asset").apply { parentFile?.mkdirs() }
        context.assets.open("legal/$asset").use { input -> file.outputStream().use(input::copyTo) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
    } catch (e: Exception) {
        Log.e("LegalDocuments", "[LegalDocuments]: Open — Unable to display document", e)
        AlertDialog.Builder(context).setMessage("Install a PDF viewer to read this document.").setPositiveButton("OK", null).show()
    }
}
