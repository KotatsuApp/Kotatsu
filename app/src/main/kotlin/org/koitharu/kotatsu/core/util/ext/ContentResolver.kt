package org.koitharu.kotatsu.core.util.ext

import android.annotation.TargetApi
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import org.koitharu.kotatsu.parsers.util.removeSuffix
import java.io.File
import java.lang.reflect.Array as ArrayReflect

private const val PRIMARY_VOLUME_NAME = "primary"

fun Uri.resolveFile(context: Context): File? {
	val volumeId = getVolumeIdFromTreeUri(this) ?: return null
	val volumePath = getVolumePath(volumeId, context)?.removeSuffix(File.separatorChar) ?: return null
	val documentPath = getDocumentPathFromTreeUri(this)?.removeSuffix(File.separatorChar) ?: return null

	return File(
		if (documentPath.isNotEmpty()) {
			if (documentPath.startsWith(File.separator)) {
				volumePath + documentPath
			} else {
				volumePath + File.separator + documentPath
			}
		} else {
			volumePath
		},
	)
}

private fun getVolumePath(volumeId: String, context: Context): String? {
	return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
		getVolumePathForAndroid11AndAbove(volumeId, context)
	} else {
		getVolumePathBeforeAndroid11(volumeId, context)
	}
}


private fun getVolumePathBeforeAndroid11(volumeId: String, context: Context): String? = runCatching {
	val mStorageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
	val storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
	val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
	val getUuid = storageVolumeClazz.getMethod("getUuid")
	val getPath = storageVolumeClazz.getMethod("getPath")
	val isPrimary = storageVolumeClazz.getMethod("isPrimary")
	val result = getVolumeList.invoke(mStorageManager)
	val length = ArrayReflect.getLength(checkNotNull(result))
	(0 until length).firstNotNullOfOrNull { i ->
		val storageVolumeElement = ArrayReflect.get(result, i)
		val uuid = getUuid.invoke(storageVolumeElement) as String?
		val primary = isPrimary.invoke(storageVolumeElement) as Boolean
		when {
			primary && volumeId == PRIMARY_VOLUME_NAME -> getPath.invoke(storageVolumeElement) as String
			uuid == volumeId -> getPath.invoke(storageVolumeElement) as String
			else -> null
		}
	}
}.onFailure {
	it.printStackTraceDebug()
}.getOrNull()

@TargetApi(Build.VERSION_CODES.R)
private fun getVolumePathForAndroid11AndAbove(volumeId: String, context: Context): String? = runCatching {
	val storageManager = context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
	storageManager.storageVolumes.firstNotNullOfOrNull { volume ->
		if (volume.isPrimary && volumeId == PRIMARY_VOLUME_NAME) {
			volume.directory?.path
		} else {
			val uuid = volume.uuid
			if (uuid != null && uuid == volumeId) volume.directory?.path else null
		}
	}
}.onFailure {
	it.printStackTraceDebug()
}.getOrNull()

private fun getVolumeIdFromTreeUri(treeUri: Uri): String? {
	val docId = DocumentsContract.getTreeDocumentId(treeUri)
	val split = docId.split(":".toRegex())
	return split.firstOrNull()?.takeUnless { it.isEmpty() }
}

private fun getDocumentPathFromTreeUri(treeUri: Uri): String? {
	val docId = DocumentsContract.getTreeDocumentId(treeUri)
	val split: Array<String?> = docId.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
	return if (split.size >= 2 && split[1] != null) split[1] else File.separator
}
