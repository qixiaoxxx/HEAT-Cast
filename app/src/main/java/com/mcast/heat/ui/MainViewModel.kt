package com.mcast.heat.ui

import android.app.Application
import com.mcast.heat.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class MainViewModel @Inject constructor(
    application: Application
) : BaseViewModel(application) {

}