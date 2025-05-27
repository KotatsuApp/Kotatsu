package org.koitharu.kotatsu.reader.ui.text

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import org.koitharu.kotatsu.R
import org.koitharu.kotatsu.core.ui.widgets.MaterialSpinnerAdapter
import org.koitharu.kotatsu.databinding.DialogTranslationBinding

class TranslationBottomSheet : BottomSheetDialogFragment() {

    private var _binding: DialogTranslationBinding? = null
    private val binding get() = _binding!!
    private val viewModel: TranslationViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogTranslationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupLanguageSpinner()
        setupButtons()
        observeState()
    }

    private fun setupLanguageSpinner() {
        val languages = listOf(
            "en" to "English",
            "id" to "Indonesian",
            "es" to "Spanish",
            "fr" to "French",
            "vi" to "Vietnamese"
            // Add more languages as needed
        )
        val adapter = MaterialSpinnerAdapter(requireContext(), languages.map { it.second })
        binding.spinnerLanguage.adapter = adapter
    }

    private fun setupButtons() {
        binding.buttonOfflineTranslate.setOnClickListener {
            val targetLanguage = getSelectedLanguageCode()
            val text = arguments?.getString(ARG_TEXT) ?: return@setOnClickListener
            viewModel.translateOffline(text, targetLanguage)
        }

        binding.buttonOnlineTranslate.setOnClickListener {
            val targetLanguage = getSelectedLanguageCode()
            val text = arguments?.getString(ARG_TEXT) ?: return@setOnClickListener
            val url = viewModel.getOnlineTranslationUrl(text, targetLanguage)
            openInBrowser(url)
        }
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.translationState.collect { state ->
                binding.progressBar.isVisible = state is TranslationState.Loading
                binding.textError.isVisible = state is TranslationState.Error
                if (state is TranslationState.Error) {
                    binding.textError.text = state.message
                }
                if (state is TranslationState.Translated) {
                    dismiss()
                }
            }
        }
    }

    private fun getSelectedLanguageCode(): String {
        val position = binding.spinnerLanguage.selectedItemPosition
        return when (position) {
            0 -> "en"
            1 -> "id"
            2 -> "es"
            3 -> "fr"
            4 -> "vi"
            else -> "en"
        }
    }

    private fun openInBrowser(url: String) {
        activity?.let {
            org.koitharu.kotatsu.core.nav.AppRouter(it).openInBrowser(url)
        }
        dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TEXT = "text"

        fun newInstance(text: String): TranslationBottomSheet {
            return TranslationBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                }
            }
        }
    }
}
