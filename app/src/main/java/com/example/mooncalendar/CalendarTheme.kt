package com.example.mooncalendar

import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material.ripple.LocalRippleTheme
import androidx.compose.material.ripple.RippleTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

private val Yellow200 = Color(0xffffeb46)
private val Blue200 = Color(0xff91a4fc)

private val DarkColors = darkColors(
    primary = Yellow200,
    secondary = Blue200,
    background = Color.Black
    // ...
)
private val LightColors = lightColors(
    primary = Yellow200,
    secondary = Blue200,
    background = Color.White
    // ...
)
@Composable
fun CalendarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = LightColors
    ) {
        CompositionLocalProvider(
            LocalRippleTheme provides NoRippleTheme
        ) {
            content()
        }
    }
}

@Immutable
private object NoRippleTheme : RippleTheme {
    @Composable
    override fun defaultColor() = RippleTheme.defaultRippleColor(
        contentColor = MaterialTheme.colors.background,
        lightTheme = MaterialTheme.colors.isLight
    )

    @Composable
    override fun rippleAlpha() = RippleTheme.defaultRippleAlpha(
        contentColor = MaterialTheme.colors.background,
        lightTheme = MaterialTheme.colors.isLight
    )
}