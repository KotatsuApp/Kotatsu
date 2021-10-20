package org.koitharu.kotatsu.core.github

import com.google.common.truth.Truth
import org.junit.Test

class VersionIdTest {

	@Test
	fun testVersionIdParse() {
		val version = VersionId.parse("2.0")
		Truth.assertThat(version.major).isEqualTo(2)
		Truth.assertThat(version.minor).isEqualTo(0)
		Truth.assertThat(version.build).isEqualTo(0)
		Truth.assertThat(version.variantType).isEmpty()
		Truth.assertThat(version.variantNumber).isEqualTo(0)
	}

	@Test
	fun testVersionIdVariantParse() {
		val version = VersionId.parse("2.0.1-b1")
		Truth.assertThat(version.major).isEqualTo(2)
		Truth.assertThat(version.minor).isEqualTo(0)
		Truth.assertThat(version.build).isEqualTo(1)
		Truth.assertThat(version.variantType).isEqualTo("b")
		Truth.assertThat(version.variantNumber).isEqualTo(1)
	}

	@Test
	fun testVersionIdCompare() {
		val version1 = VersionId.parse("1.99.99")
		val version2 = VersionId.parse("2.0.0")
		Truth.assertThat(version1 < version2).isTrue()
	}
	@Test
	fun testVersionIdVariantCompare() {
		val version1 = VersionId.parse("2.0.1-a2")
		val version2 = VersionId.parse("2.0.1-b1")
		Truth.assertThat(version1 < version2).isTrue()
	}
}