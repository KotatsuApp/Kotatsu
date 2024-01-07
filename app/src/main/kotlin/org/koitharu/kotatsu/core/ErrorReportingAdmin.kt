package org.koitharu.kotatsu.core

import android.content.Context
import com.google.auto.service.AutoService
import org.acra.builder.ReportBuilder
import org.acra.config.CoreConfiguration
import org.acra.config.ReportingAdministrator

@AutoService(ReportingAdministrator::class)
class ErrorReportingAdmin : ReportingAdministrator {

	override fun shouldStartCollecting(
		context: Context,
		config: CoreConfiguration,
		reportBuilder: ReportBuilder
	): Boolean {
		return reportBuilder.exception?.isDeadOs() != true
	}

	private fun Throwable.isDeadOs(): Boolean {
		val className = javaClass.simpleName
		return className == "DeadSystemException" || className == "DeadSystemRuntimeException" || cause?.isDeadOs() == true
	}
}
