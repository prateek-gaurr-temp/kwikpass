package com.gk.kwikpass.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

object CoroutineUtils {
    val coroutine: CoroutineScope = CoroutineScope(SupervisorJob())
}