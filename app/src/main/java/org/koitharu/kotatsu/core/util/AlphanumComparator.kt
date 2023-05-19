package org.koitharu.kotatsu.core.util

class AlphanumComparator : Comparator<String> {

	override fun compare(s1: String?, s2: String?): Int {
		if (s1 == null || s2 == null) {
			return 0
		}
		var thisMarker = 0
		var thatMarker = 0
		val s1Length = s1.length
		val s2Length = s2.length
		while (thisMarker < s1Length && thatMarker < s2Length) {
			val thisChunk = getChunk(s1, s1Length, thisMarker)
			thisMarker += thisChunk.length
			val thatChunk = getChunk(s2, s2Length, thatMarker)
			thatMarker += thatChunk.length
			// If both chunks contain numeric characters, sort them numerically
			var result: Int
			if (thisChunk[0].isDigit() && thatChunk[0].isDigit()) { // Simple chunk comparison by length.
				val thisChunkLength = thisChunk.length
				result = thisChunkLength - thatChunk.length
				// If equal, the first different number counts
				if (result == 0) {
					for (i in 0 until thisChunkLength) {
						result = thisChunk[i] - thatChunk[i]
						if (result != 0) {
							return result
						}
					}
				}
			} else {
				result = thisChunk.compareTo(thatChunk)
			}
			if (result != 0) return result
		}
		return s1Length - s2Length
	}

	private fun getChunk(s: String, slength: Int, cmarker: Int): String {
		var marker = cmarker
		val chunk = StringBuilder()
		var c = s[marker]
		chunk.append(c)
		marker++
		if (c.isDigit()) {
			while (marker < slength) {
				c = s[marker]
				if (!c.isDigit()) break
				chunk.append(c)
				marker++
			}
		} else {
			while (marker < slength) {
				c = s[marker]
				if (c.isDigit()) break
				chunk.append(c)
				marker++
			}
		}
		return chunk.toString()
	}
}
