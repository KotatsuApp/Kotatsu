package org.koitharu.kotatsu.ui.common

import moxy.MvpPresenter
import moxy.MvpView
import org.koin.core.KoinComponent

abstract class BasePresenter<V : MvpView> : MvpPresenter<V>(), KoinComponent