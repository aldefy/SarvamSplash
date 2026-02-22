package com.example.sarvamsplash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.sarvamsplash.theme.SarvamColors

@Composable
fun App() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SarvamColors.background),
    ) {
        SarvamSplashScreen()
    }
}
