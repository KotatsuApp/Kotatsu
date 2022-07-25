package org.koitharu.kotatsu.utils.ext

import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.fragment.app.viewModels
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

@MainThread
inline fun <reified VM : ViewModel> ComponentActivity.assistedViewModels(
	noinline viewModelProducer: (SavedStateHandle) -> VM,
): Lazy<VM> = viewModels {
	object : AbstractSavedStateViewModelFactory(this, intent.extras) {
		override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
			return requireNotNull(modelClass.cast(viewModelProducer(handle)))
		}
	}
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.assistedViewModels(
	noinline viewModelProducer: (SavedStateHandle) -> VM,
): Lazy<VM> = viewModels {
	object : AbstractSavedStateViewModelFactory(this, arguments) {
		override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
			return requireNotNull(modelClass.cast(viewModelProducer(handle)))
		}
	}
}

@MainThread
inline fun <reified VM : ViewModel> Fragment.parentFragmentViewModels(
	noinline extrasProducer: (() -> CreationExtras)? = null,
	noinline factoryProducer: (() -> ViewModelProvider.Factory)? = null,
): Lazy<VM> = createViewModelLazy(
	viewModelClass = VM::class,
	storeProducer = { requireParentFragment().viewModelStore },
	extrasProducer = { extrasProducer?.invoke() ?: requireParentFragment().defaultViewModelCreationExtras },
	factoryProducer = factoryProducer ?: { requireParentFragment().defaultViewModelProviderFactory },
)
