package org.koitharu.kotatsu.ui.common

import moxy.MvpPresenter
import moxy.MvpView
import org.koin.core.component.KoinComponent

abstract class BasePresenter<V : MvpView> : MvpPresenter<V>(), KoinComponent