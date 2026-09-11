package com.wanbaohe.speedtest.data

import com.shifenmiao.database.speedtest.dao.SpeedTestRecordDao
import com.shifenmiao.database.speedtest.entity.SpeedTestRecordEntity
import com.shifenmiao.interfaces.singleton.AppContext
import com.wanbaohe.speedtest.R
import com.wanbaohe.speedtest.domain.SpeedTestPhase
import com.wanbaohe.speedtest.domain.SpeedTestRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class SpeedTestRepositoryImpl @Inject constructor(
    private val dao: SpeedTestRecordDao,
    @Named("SpeedTestOkHttpClient") private val okHttpClient: OkHttpClient
) : SpeedTestRepository {

    override fun startTest(config: SpeedTestConfig, networkType: String): Flow<SpeedTestPhase> =
        flow {
            emit(SpeedTestPhase.MeasuringLatency)

            // ── Step 1: HEAD 请求测延迟 ──────────────────────────────────
            val latencyMs = measureLatency(config.testUrl)

            emit(SpeedTestPhase.Downloading(liveMbps = 0f, progress = 0f, latencyMs = latencyMs))

            // ── Step 2: 下载测速 ─────────────────────────────────────────
            val request = Request.Builder().url(config.testUrl).build()
            val startTime = monotonicMillis()
            val maxDurationMs = config.durationSeconds.coerceAtLeast(1) * 1000L
            var totalBytes = 0L
            var lastEmitTime = startTime
            var lastBytes = 0L

            val finalMbps = okHttpClient.newCall(request).useCancellable { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val buffer = ByteArray(8 * 1024)
                val stream = response.body.byteStream()
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val read = stream.read(buffer)
                    if (read == -1) break
                    totalBytes += read
                    val now = monotonicMillis()
                    val elapsed = now - startTime
                    // 每 500ms 发射一次实时速度
                    if (now - lastEmitTime >= 500L) {
                        val intervalBytes = totalBytes - lastBytes
                        val intervalSec = (now - lastEmitTime) / 1000.0
                        val mbps = ((intervalBytes * 8) / 1_000_000.0 / intervalSec).toFloat()
                        val progress = (elapsed.toFloat() / maxDurationMs).coerceIn(0f, 1f)
                        emit(SpeedTestPhase.Downloading(mbps, progress, latencyMs))
                        lastEmitTime = now
                        lastBytes = totalBytes
                    }
                    if (elapsed >= maxDurationMs) break
                }
                // 计算整体均值作为最终速度
                val totalElapsedSec = (monotonicMillis() - startTime).coerceAtLeast(1) / 1000.0
                ((totalBytes * 8) / 1_000_000.0 / totalElapsedSec).toFloat()
            }

            val record = SpeedTestRecord(
                networkType = networkType,
                downloadMbps = finalMbps,
                latencyMs = latencyMs,
                recordedAt = System.currentTimeMillis()
            )
            emit(SpeedTestPhase.Done(record))
        }.catch {
            emit(SpeedTestPhase.Error(it.message ?: AppContext.getString(R.string.speed_test_error_failed)))
        }.flowOn(Dispatchers.IO)

    /** HEAD 请求测延迟（ms），失败返回 -1 */
    private suspend fun measureLatency(url: String): Int = try {
        val req = Request.Builder().url(url).head().build()
        val start = monotonicMillis()
        okHttpClient.newCall(req).useCancellable { response ->
            if (response.isSuccessful) (monotonicMillis() - start).toInt() else -1
        }
    } catch (_: Exception) {
        currentCoroutineContext().ensureActive()
        -1
    }

    private fun monotonicMillis() = System.nanoTime() / 1_000_000L

    /** Cancellation must close the socket even while execute/read is blocking. */
    private suspend fun <T> Call.useCancellable(block: suspend (Response) -> T): T = coroutineScope {
        val cancellation = launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
            try {
                awaitCancellation()
            } finally {
                this@useCancellable.cancel()
            }
        }
        try {
            execute().use { block(it) }
        } finally {
            cancellation.cancel()
        }
    }

    override suspend fun saveRecord(record: SpeedTestRecord) {
        dao.insert(
            SpeedTestRecordEntity(
                networkType = record.networkType,
                downloadMbps = record.downloadMbps,
                latencyMs = record.latencyMs,
                recordedAt = record.recordedAt
            )
        )
    }

    override fun getHistory(): Flow<List<SpeedTestRecord>> =
        dao.getAll().map { list ->
            list.map { e ->
                SpeedTestRecord(
                    id = e.id,
                    networkType = e.networkType,
                    downloadMbps = e.downloadMbps,
                    latencyMs = e.latencyMs,
                    recordedAt = e.recordedAt
                )
            }
        }

    override suspend fun clearHistory() = dao.clearAll()
}

