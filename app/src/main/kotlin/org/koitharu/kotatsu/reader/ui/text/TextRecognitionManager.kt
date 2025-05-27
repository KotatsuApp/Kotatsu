package org.koitharu.kotatsu.reader.ui.text

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextRecognitionManager @Inject constructor() {

    private val textRecognizer: TextRecognizer by lazy {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    suspend fun detectText(bitmap: Bitmap): Flow<List<TextElement>> = callbackFlow {
        withContext(Dispatchers.Default) {
            val image = InputImage.fromBitmap(bitmap, 0)
            textRecognizer.process(image)
                .addOnSuccessListener { text ->
                    val elements = text.textBlocks.flatMap { block ->
                        block.lines.flatMap { line ->
                            line.elements.map { element ->
                                TextElement(
                                    text = element.text,
                                    boundingBox = element.boundingBox,
                                    confidence = element.confidence,
                                    recognizedLanguage = element.recognizedLanguage
                                )
                            }
                        }
                    }
                    trySend(elements)
                }
                .addOnFailureListener { error ->
                    close(error)
                }
        }

        awaitClose {
            textRecognizer.close()
        }
    }
}

data class TextElement(
    val text: String,
    val boundingBox: android.graphics.Rect?,
    val confidence: Float?,
    val recognizedLanguage: String
)
