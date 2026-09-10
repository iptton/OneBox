package com.wanbaohe.period.router

import androidx.compose.runtime.Composable
import com.wanbaohe.period.router.screenLogic.PeriodRouterComponent
import com.wanbaohe.period.screen.PeriodDetailScreen
import com.wanbaohe.period.screen.PeriodEditorScreen
import com.wanbaohe.period.screen.PeriodMainScreen

@Composable
fun PeriodRouterScreen(component: PeriodRouterComponent) {
    when (val child = component.child) {
        is PeriodRouterComponent.PeriodChild.Main -> PeriodMainScreen(
            component = child.component,
        )
        is PeriodRouterComponent.PeriodChild.Editor -> PeriodEditorScreen(
            component = child.component,
        )
        is PeriodRouterComponent.PeriodChild.Detail -> PeriodDetailScreen(
            component = child.component,
        )
    }
}
