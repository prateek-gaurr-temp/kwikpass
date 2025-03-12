package com.gk.kwikpass.ImageViewModal

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ImageViewModel : ViewModel() {
    private val bannerImageVm = MutableStateFlow<String?>(null)
    private val logoVm = MutableStateFlow<String?>(null)


    val bannerImage: StateFlow<String?> = bannerImageVm

    fun setBannerImage(source: String) {
        bannerImageVm.value = source
    }

    val logo: StateFlow<String?> = logoVm

    fun setLogo(source: String) {
        logoVm.value = source
    }
}
