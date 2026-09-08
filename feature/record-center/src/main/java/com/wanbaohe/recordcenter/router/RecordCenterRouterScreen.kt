package com.wanbaohe.recordcenter.router

import androidx.compose.runtime.Composable
import com.wanbaohe.recordcenter.router.screenLogic.RecordCenterRouterComponent
import com.wanbaohe.recordcenter.screen.AddRecordScreen
import com.wanbaohe.recordcenter.screen.RecordCenterScreen
import com.wanbaohe.recordcenter.screen.RecordListScreen

@Composable
fun RecordCenterRouterScreen(component: RecordCenterRouterComponent) {
    when (val child = component.child) {
        is RecordCenterRouterComponent.RecordCenterChild.Main -> RecordCenterScreen(
            component = child.component,
        )

        is RecordCenterRouterComponent.RecordCenterChild.RecordList -> RecordListScreen(
            component = child.component,
        )

        is RecordCenterRouterComponent.RecordCenterChild.AddRecord -> AddRecordScreen(
            component = child.component,
        )
    }
}
