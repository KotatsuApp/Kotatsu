package org.koitharu.kotatsu.settings.about.changelog

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import org.jsoup.internal.StringUtil
import org.koitharu.kotatsu.core.github.AppUpdateRepository
import org.koitharu.kotatsu.core.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ChangelogViewModel @Inject constructor(
	private val appUpdateRepository: AppUpdateRepository,
) : BaseViewModel() {

	val changelog = MutableStateFlow<String?>(null)

	init {
		launchLoadingJob(Dispatchers.Default) {
			val versions = appUpdateRepository.getAvailableVersions()
			val stringJoiner = StringUtil.StringJoiner("\n\n\n")
			for (version in versions) {
				stringJoiner.add("# ")
					.append(version.name)
					.append("\n\n")
					.append(version.description)
			}
			changelog.value = stringJoiner.complete()
		}
	}
}
