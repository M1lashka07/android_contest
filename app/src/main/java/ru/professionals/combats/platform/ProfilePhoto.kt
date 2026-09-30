package ru.professionals.combats.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

/** Returns a temporary camera output URI readable by the external camera app. */
fun createCameraUri(context: Context): Uri {
    val directory = File(context.cacheDir, "shared").apply { mkdirs() }
    return FileProvider.getUriForFile(context, "${context.packageName}.files", File.createTempFile("avatar-", ".jpg", directory))
}

/** Decodes orientation and limits upload size without retaining EXIF location metadata. */
suspend fun readProfilePhoto(context: Context, uri: Uri): ByteArray = withContext(Dispatchers.IO) {
    val source = ImageDecoder.createSource(context.contentResolver, uri)
    val bitmap = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val longest = maxOf(info.size.width, info.size.height)
        if (longest > 1024) decoder.setTargetSize((info.size.width * 1024 / longest).coerceAtLeast(1), (info.size.height * 1024 / longest).coerceAtLeast(1))
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
    ByteArrayOutputStream().use { output -> bitmap.compress(Bitmap.CompressFormat.JPEG, 88, output); bitmap.recycle(); output.toByteArray() }
}
