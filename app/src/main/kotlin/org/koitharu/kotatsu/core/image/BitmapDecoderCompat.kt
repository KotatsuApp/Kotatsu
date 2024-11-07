package org.koitharu.kotatsu.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import android.webkit.MimeTypeMap
import com.davemorrissey.labs.subscaleview.decoder.ImageDecodeException
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.aomedia.avif.android.AvifDecoder
import org.aomedia.avif.android.AvifDecoder.Info
import org.jetbrains.annotations.Blocking
import org.koitharu.kotatsu.core.util.ext.toByteBuffer
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files

object BitmapDecoderCompat {

	private const val FORMAT_AVIF = "avif"

	@Blocking
	fun decode(file: File): Bitmap = when (val format = getMimeType(file)?.subtype) {
		FORMAT_AVIF -> file.inputStream().use { decodeAvif(it.toByteBuffer()) }
		else -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			ImageDecoder.decodeBitmap(ImageDecoder.createSource(file))
		} else {
			checkBitmapNotNull(BitmapFactory.decodeFile(file.absolutePath), format)
		}
	}

	@Blocking
	fun decode(stream: InputStream, type: MediaType?): Bitmap {
		val format = type?.subtype
		if (format == FORMAT_AVIF) {
			return decodeAvif(stream.toByteBuffer())
		}
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			return checkBitmapNotNull(BitmapFactory.decodeStream(stream), format)
		}
		val byteBuffer = stream.toByteBuffer()
		return if (AvifDecoder.isAvifImage(byteBuffer)) {
			decodeAvif(byteBuffer)
		} else {
			ImageDecoder.decodeBitmap(ImageDecoder.createSource(byteBuffer))
		}
	}

	private fun getMimeType(file: File): MediaType? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
		Files.probeContentType(file.toPath())?.toMediaTypeOrNull()
	} else {
		MimeTypeMap.getSingleton().getMimeTypeFromExtension(file.extension)?.toMediaTypeOrNull()
	}

	private fun checkBitmapNotNull(bitmap: Bitmap?, format: String?): Bitmap =
		bitmap ?: throw ImageDecodeException(null, format)

	private fun decodeAvif(bytes: ByteBuffer): Bitmap {
		val info = Info()
		if (!AvifDecoder.getInfo(bytes, bytes.remaining(), info)) {
			throw ImageDecodeException(
				null,
				FORMAT_AVIF,
				"Requested to decode byte buffer which cannot be handled by AvifDecoder",
			)
		}
		val config = if (info.depth == 8 || info.alphaPresent) Bitmap.Config.ARGB_8888 else Bitmap.Config.RGB_565
		val bitmap = Bitmap.createBitmap(info.width, info.height, config)
		if (!AvifDecoder.decode(bytes, bytes.remaining(), bitmap)) {
			bitmap.recycle()
			throw ImageDecodeException(null, FORMAT_AVIF)
		}
		return bitmap
	}
}
