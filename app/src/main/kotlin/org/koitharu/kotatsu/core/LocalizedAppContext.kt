package org.koitharu.kotatsu.core

import javax.inject.Qualifier

@Qualifier
@Target(
	AnnotationTarget.FUNCTION,
	AnnotationTarget.PROPERTY_GETTER,
	AnnotationTarget.PROPERTY_SETTER,
	AnnotationTarget.VALUE_PARAMETER,
	AnnotationTarget.FIELD,
)
annotation class LocalizedAppContext
