
package org.koitharu.kotatsu.utils.ext

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private val TYPE_JSON = "application/json".toMediaType()

fun JSONObject.toRequestBody() = toString().toRequestBody(TYPE_JSON)