package org.koitharu.kotatsu.utils

import com.google.common.truth.StringSubject
import java.util.regex.Pattern

private val PATTERN_URL_ABSOLUTE = Pattern.compile("https?://[^\\s]+", Pattern.CASE_INSENSITIVE)
private val PATTERN_URL_RELATIVE = Pattern.compile("^/[^\\s]+", Pattern.CASE_INSENSITIVE)

fun StringSubject.isRelativeUrl() = matches(PATTERN_URL_RELATIVE)

fun StringSubject.isAbsoluteUrl() = matches(PATTERN_URL_ABSOLUTE)

fun StringSubject.isNotAbsoluteUrl() = doesNotMatch(PATTERN_URL_ABSOLUTE)