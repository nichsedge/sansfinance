package com.sans.finance.presentation.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

object NavigationTransitions {
    private const val DEFAULT_DURATION_MS = 200
    private const val MODAL_DURATION_MS = 220
    private const val FADE_DURATION_MS = 150

    val defaultEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(DEFAULT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(FADE_DURATION_MS))
    }

    val defaultExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(DEFAULT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(FADE_DURATION_MS))
    }

    val defaultPopEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(DEFAULT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(FADE_DURATION_MS))
    }

    val defaultPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.End,
            animationSpec = tween(DEFAULT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(FADE_DURATION_MS))
    }

    val modalEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = tween(MODAL_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeIn(animationSpec = tween(FADE_DURATION_MS))
    }

    val modalPopExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = tween(DEFAULT_DURATION_MS, easing = FastOutSlowInEasing)
        ) + fadeOut(animationSpec = tween(FADE_DURATION_MS))
    }

    val tabEnterTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        EnterTransition.None
    }

    val tabExitTransition: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        ExitTransition.None
    }
}

