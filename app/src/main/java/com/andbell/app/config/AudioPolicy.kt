package com.andbell.app.config

import com.andbell.app.BuildConfig

data class AudioPolicy(
    val includeBundledAudio: Boolean,
)

object AudioPolicyProvider {
    fun current(): AudioPolicy = AudioPolicy(
        includeBundledAudio = BuildConfig.INCLUDE_BUNDLED_AUDIO,
    )
}
