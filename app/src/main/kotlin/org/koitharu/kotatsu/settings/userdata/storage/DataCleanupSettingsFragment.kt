package org.koitharu.kotatsu.settings.userdata.storage

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import androidx.preference.Preference
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.StateFlow
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.exceptions.resolve.SnackbarErrorObserver
import org.koitharu.kotatsu.core.prefs.AppSettings
import org.koitharu.kotatsu.core.ui.BasePreferenceFragment
import org.koitharu.kotatsu.core.ui.dialog.buildAlertDialog
import org.koitharu.kotatsu.core.ui.util.ReversibleActionObserver
import org.koitharu.kotatsu.core.util.FileSize
import org.koitharu.kotatsu.core.util.ext.getQuantityStringSafe
import org.koitharu.kotatsu.core.util.ext.observe
import org.koitharu.kotatsu.core.util.ext.observeEvent
import org.koitharu.kotatsu.local.data.CacheDir

@AndroidEntryPoint
class DataCleanupSettingsFragment : BasePreferenceFragment(R.string.data_removal) {

    private val viewModel by viewModels<DataCleanupSettingsViewModel>()
    private val loadingPrefs = HashSet<String>()

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.pref_data_cleanup)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<Preference>(AppSettings.KEY_PAGES_CACHE_CLEAR)?.bindBytesSizeSummary(checkNotNull(viewModel.cacheSizes[CacheDir.PAGES]))
        findPreference<Preference>(AppSettings.KEY_THUMBS_CACHE_CLEAR)?.bindBytesSizeSummary(checkNotNull(viewModel.cacheSizes[CacheDir.THUMBS]))
        findPreference<Preference>(AppSettings.KEY_HTTP_CACHE_CLEAR)?.bindBytesSizeSummary(viewModel.httpCacheSize)
        findPreference<Preference>(AppSettings.KEY_SEARCH_HISTORY_CLEAR)?.let { pref ->
            viewModel.searchHistoryCount.observe(viewLifecycleOwner) {
                pref.summary = if (it < 0) {
                    view.context.getString(R.string.loading_)
                } else {
                    pref.context.resources.getQuantityStringSafe(R.plurals.items, it, it)
                }
            }
        }
        findPreference<Preference>(AppSettings.KEY_UPDATES_FEED_CLEAR)?.let { pref ->
            viewModel.feedItemsCount.observe(viewLifecycleOwner) {
                pref.summary = if (it < 0) {
                    view.context.getString(R.string.loading_)
                } else {
                    pref.context.resources.getQuantityStringSafe(R.plurals.items, it, it)
                }
            }
        }
        findPreference<Preference>(AppSettings.KEY_WEBVIEW_CLEAR)?.isVisible = viewModel.isBrowserDataCleanupEnabled

        viewModel.loadingKeys.observe(viewLifecycleOwner) { keys ->
            loadingPrefs.addAll(keys)
            loadingPrefs.forEach { prefKey ->
                findPreference<Preference>(prefKey)?.isEnabled = prefKey !in keys
            }
        }
        viewModel.onError.observeEvent(viewLifecycleOwner, SnackbarErrorObserver(listView, this))
        viewModel.onActionDone.observeEvent(viewLifecycleOwner, ReversibleActionObserver(listView))
        viewModel.onChaptersCleanedUp.observeEvent(viewLifecycleOwner, ::onChaptersCleanedUp)
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean = when (preference.key) {
        AppSettings.KEY_COOKIES_CLEAR -> {
            clearCookies()
            true
        }

        AppSettings.KEY_SEARCH_HISTORY_CLEAR -> {
            clearSearchHistory()
            true
        }

        AppSettings.KEY_PAGES_CACHE_CLEAR -> {
            viewModel.clearCache(preference.key, CacheDir.PAGES)
            true
        }

        AppSettings.KEY_THUMBS_CACHE_CLEAR -> {
            viewModel.clearCache(preference.key, CacheDir.THUMBS, CacheDir.FAVICONS)
            true
        }

        AppSettings.KEY_HTTP_CACHE_CLEAR -> {
            viewModel.clearHttpCache()
            true
        }

        AppSettings.KEY_CHAPTERS_CLEAR -> {
            cleanupChapters()
            true
        }

        AppSettings.KEY_WEBVIEW_CLEAR -> {
            viewModel.clearBrowserData()
            true
        }

        AppSettings.KEY_CLEAR_MANGA_DATA -> {
            viewModel.clearMangaData()
            true
        }

        AppSettings.KEY_UPDATES_FEED_CLEAR -> {
            viewModel.clearUpdatesFeed()
            true
        }

        else -> super.onPreferenceTreeClick(preference)
    }

    private fun onChaptersCleanedUp(result: Pair<Int, Long>) {
        val c = context ?: return
        val text = if (result.first == 0 && result.second == 0L) {
            c.getString(R.string.no_chapters_deleted)
        } else {
            c.getString(
                R.string.chapters_deleted_pattern,
                c.resources.getQuantityStringSafe(R.plurals.chapters, result.first, result.first),
                FileSize.BYTES.format(c, result.second),
            )
        }
        Snackbar.make(listView, text, Snackbar.LENGTH_SHORT).show()
    }

    private fun Preference.bindBytesSizeSummary(stateFlow: StateFlow<Long>) {
        stateFlow.observe(viewLifecycleOwner) { size ->
            summary = if (size < 0) {
                context.getString(R.string.computing_)
            } else {
                FileSize.BYTES.format(context, size)
            }
        }
    }

    private fun clearSearchHistory() {
        buildAlertDialog(context ?: return) {
            setTitle(R.string.clear_search_history)
            setMessage(R.string.text_clear_search_history_prompt)
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearSearchHistory()
            }
        }.show()
    }

    private fun clearCookies() {
        buildAlertDialog(context ?: return) {
            setTitle(R.string.clear_cookies)
            setMessage(R.string.text_clear_cookies_prompt)
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.clear) { _, _ ->
                viewModel.clearCookies()
            }
        }.show()
    }

    private fun cleanupChapters() {
        buildAlertDialog(context ?: return) {
            setTitle(R.string.delete_read_chapters)
            setMessage(R.string.delete_read_chapters_prompt)
            setNegativeButton(android.R.string.cancel, null)
            setPositiveButton(R.string.delete) { _, _ ->
                viewModel.cleanupChapters()
            }
        }.show()
    }
}
