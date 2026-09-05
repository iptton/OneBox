package com.shifenmiao.ai.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.shifenmiao.ai.voice.AsrState
import com.shifenmiao.ai.voice.VoiceRecognizer
import com.shifenmiao.core.R
import com.t8rin.imagetoolbox.core.ui.utils.helper.AppToastHost
import com.t8rin.imagetoolbox.core.ui.widget.glass.GlassStyle
import com.t8rin.imagetoolbox.core.ui.widget.glass.glassBackground

/**
 * 语音输入录音面板(讯飞大模型识别)。
 *
 * 打开后先经 [VoiceRecognizer.prepare] 向网关换签名 wss URL,再直连讯飞流式识别;
 * 中部麦克风按钮随 [VoiceRecognizer.rms] 做脉冲缩放,点击 = 提前说完;
 * 下滑/点外部 = 放弃(不回填);识别完成后结果经 [onResult] 回填并自动关闭。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceInputSheet(
    visible: Boolean,
    recognizer: VoiceRecognizer,
    onResult: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) return

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state by recognizer.state.collectAsState()
    val rms by recognizer.rms.collectAsState()
    // 已终结(成功/失败)标记,防止重复回调 onResult/onDismiss
    var terminated by remember { mutableStateOf(false) }
    // 本轮会话已 start 标记:防止打开瞬间消费到上一轮残留的 Finished/Error 状态
    var started by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val auth = recognizer.prepare()
        if (auth == null) {
            AppToastHost.showToast(R.string.ai_voice_error)
            onDismiss()
        } else {
            recognizer.start(auth.first, auth.second, scope)
            started = true
        }
    }

    LaunchedEffect(state) {
        if (!started || terminated) return@LaunchedEffect
        when (val s = state) {
            is AsrState.Finished -> {
                terminated = true
                if (s.text.isNotBlank()) onResult(s.text)
                recognizer.cancel()
                onDismiss()
            }

            is AsrState.Error -> {
                terminated = true
                recognizer.cancel()
                // 联调期带上讯飞错误详情(如 11201: licc failed),方便定位
                AppToastHost.showToast(
                    context.getString(R.string.ai_voice_error) + " (${s.message})"
                )
                onDismiss()
            }

            else -> Unit
        }
    }

    ModalBottomSheet(
        onDismissRequest = {
            recognizer.cancel()
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.ai_voice_listening),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 大号圆形麦克风按钮,尺寸随录音音量做 1.0~1.25 倍脉冲缩放
            val targetScale = 1f + rms.coerceIn(0f, 1f) * 0.25f
            val pulseScale by animateFloatAsState(targetValue = targetScale, label = "rmsPulse")
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = pulseScale
                        scaleY = pulseScale
                    }
                    .size(96.dp)
                    .clip(CircleShape)
                    .glassBackground(
                        style = GlassStyle.Dense,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                        borderWidth = 0.dp
                    )
                    .clickable { recognizer.finish() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = stringResource(R.string.ai_voice_tap_to_finish),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ai_voice_tap_to_finish),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            // 流式中间结果,自动滚到底
            val partialText = (state as? AsrState.Listening)?.partialText.orEmpty()
            val scrollState = rememberScrollState()
            LaunchedEffect(partialText) {
                scrollState.animateScrollTo(scrollState.maxValue)
            }
            Text(
                text = partialText,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp, max = 120.dp)
                    .verticalScroll(scrollState)
            )
        }
    }
}
