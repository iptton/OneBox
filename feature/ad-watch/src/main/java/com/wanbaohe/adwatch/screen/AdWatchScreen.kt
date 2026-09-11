package com.wanbaohe.adwatch.screen

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shifenmiao.common.ui.BaseScreen
import com.shifenmiao.theme.AppTheme
import com.t8rin.imagetoolbox.core.ui.widget.enhanced.EnhancedButton
import com.t8rin.imagetoolbox.core.ui.widget.glass.glassRegular
import com.wanbaohe.adwatch.R
import com.wanbaohe.adwatch.ads.RewardedAdStatus
import com.wanbaohe.adwatch.component.AdWatchComponent

@Composable
fun AdWatchScreen(
    component: AdWatchComponent
) {
    val state by component.uiState.collectAsState()
    val adStatus by component.adStatus.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity

    BaseScreen(
        title = stringResource(R.string.ad_watch_title),
        onGoBack = component.onGoBack,
    ) {
        val pagerState = rememberPagerState(pageCount = { Int.MAX_VALUE })

        // 看完广告到账后自动滑到下一页
        LaunchedEffect(state.watchedToday) {
            if (state.watchedToday > 0) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }

        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
        ) {
            AdWatchCard(
                rewardPoints = state.rewardPoints,
                remainingToday = state.remainingToday,
                adStatus = adStatus,
                isShowing = state.isShowing,
                onWatch = { activity?.let(component::onWatchClick) },
                onRetry = component::retryLoad,
            )
        }
    }
}

@Composable
private fun AdWatchCard(
    rewardPoints: Int,
    remainingToday: Int,
    adStatus: RewardedAdStatus,
    isShowing: Boolean,
    onWatch: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassRegular(shape = RoundedCornerShape(24.dp))
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.ad_watch_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingNormal))
            Text(
                text = stringResource(R.string.ad_watch_desc, rewardPoints),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingLarge))
            Text(
                text = stringResource(R.string.ad_watch_remaining_today, remainingToday),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(AppTheme.dimens.paddingLarge))

            when {
                adStatus == RewardedAdStatus.LOADING || isShowing -> {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.height(AppTheme.dimens.paddingNormal))
                    Text(
                        text = stringResource(R.string.ad_watch_loading),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                adStatus == RewardedAdStatus.FAILED -> {
                    Text(
                        text = stringResource(R.string.ad_watch_load_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Spacer(modifier = Modifier.height(AppTheme.dimens.paddingNormal))
                    EnhancedButton(onClick = onRetry) {
                        Text(text = stringResource(R.string.ad_watch_retry))
                    }
                }

                remainingToday <= 0 -> {
                    Text(
                        text = stringResource(R.string.ad_watch_daily_limit_reached),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> {
                    EnhancedButton(
                        onClick = onWatch,
                        enabled = adStatus == RewardedAdStatus.READY && !isShowing,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.primary,
                    ) {
                        Text(
                            text = stringResource(R.string.ad_watch_button, rewardPoints),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }
            }
        }
    }
}
