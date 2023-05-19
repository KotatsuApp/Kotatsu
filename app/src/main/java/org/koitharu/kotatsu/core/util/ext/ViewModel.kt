package org.koitharu.kotatsu.core.util.ext

import androidx.annotation.MainThread
import androidx.fragment.app.Fragment
import androidx.fragment.app.createViewModelLazy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras

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
