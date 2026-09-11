package com.shifenmiao.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.shifenmiao.database.altitude.dao.AltitudeRecordDao
import com.shifenmiao.database.altitude.entity.AltitudeRecordEntity
import com.shifenmiao.database.aidetect.dao.AiDetectRecordDao
import com.shifenmiao.database.aidetect.entity.AiDetectRecordEntity
import com.shifenmiao.database.bookkeeping.dao.BookkeepingCategoryDao
import com.shifenmiao.database.bookkeeping.dao.BookkeepingRecordDao
import com.shifenmiao.database.bookkeeping.entity.BookkeepingCategoryEntity
import com.shifenmiao.database.bookkeeping.entity.BookkeepingRecordEntity
import com.shifenmiao.database.blessing.dao.BlessingRecordDao
import com.shifenmiao.database.blessing.dao.BlessingTabConfigDao
import com.shifenmiao.database.blessing.dao.BlessingWishDao
import com.shifenmiao.database.blessing.entity.BlessingRecordEntity
import com.shifenmiao.database.blessing.entity.BlessingTabConfigEntity
import com.shifenmiao.database.blessing.entity.BlessingWishEntity
import com.shifenmiao.database.data_draft.dao.DataDraftDao
import com.shifenmiao.database.data_draft.entity.DataDraftEntity
import com.shifenmiao.database.decision_wheel.dao.WheelDao
import com.shifenmiao.database.decision_wheel.entity.WheelEntity
import com.shifenmiao.database.decision_wheel.entity.WheelHistoryEntity
import com.shifenmiao.database.decision_wheel.entity.WheelOptionEntity
import com.shifenmiao.database.docconvert.dao.DocConvertTaskDao
import com.shifenmiao.database.docconvert.entity.DocConvertTaskEntity
import com.shifenmiao.database.habit.dao.HabitCheckInDao
import com.shifenmiao.database.habit.dao.HabitDao
import com.shifenmiao.database.habit.entity.HabitCheckInEntity
import com.shifenmiao.database.habit.entity.HabitEntity
import com.shifenmiao.database.household.dao.HouseholdItemDao
import com.shifenmiao.database.household.dao.HouseholdLocationDao
import com.shifenmiao.database.household.entity.HouseholdItemEntity
import com.shifenmiao.database.household.entity.HouseholdLocationEntity
import com.shifenmiao.database.idphoto.dao.IdPhotoSizeDao
import com.shifenmiao.database.idphoto.entity.IdPhotoSizeEntity
import com.shifenmiao.database.lifetime.dao.CountdownEventDao
import com.shifenmiao.database.lifetime.dao.FrequencyEventDao
import com.shifenmiao.database.lifetime.dao.MilestoneAiInsightDao
import com.shifenmiao.database.lifetime.dao.PersonalMilestoneDao
import com.shifenmiao.database.lifetime.entity.CountdownEventEntity
import com.shifenmiao.database.lifetime.entity.FrequencyEventEntity
import com.shifenmiao.database.lifetime.entity.MilestoneAiInsightEntity
import com.shifenmiao.database.lifetime.entity.PersonalMilestoneEntity
import com.shifenmiao.database.marktodo.MarkTodoTypeConverters
import com.shifenmiao.database.marktodo.dao.MarkTodoCategoryDao
import com.shifenmiao.database.marktodo.dao.MarkTodoDashboardDao
import com.shifenmiao.database.marktodo.dao.MarkTodoTaskDao
import com.shifenmiao.database.marktodo.entity.MarkTodoCategoryEntity
import com.shifenmiao.database.marktodo.entity.MarkTodoTaskEntity
import com.shifenmiao.database.ocr.dao.PaddleOcrTaskDao
import com.shifenmiao.database.ocr.entity.PaddleOcrTaskEntity
import com.shifenmiao.database.period.dao.PeriodRecordDao
import com.shifenmiao.database.period.entity.PeriodRecordEntity
import com.shifenmiao.database.poem.dao.PoemDao
import com.shifenmiao.database.poem.entity.PoemEntity
import com.shifenmiao.database.recent_access.dao.RecentAccessDao
import com.shifenmiao.database.recent_access.entity.RecentAccessEntity
import com.shifenmiao.database.recordcenter.dao.HealthRecordDao
import com.shifenmiao.database.recordcenter.entity.HealthRecordEntity
import com.shifenmiao.database.schedule.dao.ScheduleEventDao
import com.shifenmiao.database.schedule.dao.ScheduleProviderBindingDao
import com.shifenmiao.database.schedule.dao.ScheduleSyncStateDao
import com.shifenmiao.database.schedule.entity.ScheduleEventEntity
import com.shifenmiao.database.schedule.entity.ScheduleProviderBindingEntity
import com.shifenmiao.database.schedule.entity.ScheduleSyncStateEntity
import com.shifenmiao.database.speedtest.dao.SpeedTestConfigDao
import com.shifenmiao.database.speedtest.dao.SpeedTestRecordDao
import com.shifenmiao.database.speedtest.entity.SpeedTestConfigEntity
import com.shifenmiao.database.speedtest.entity.SpeedTestRecordEntity
import com.shifenmiao.database.teleprompter.dao.TeleprompterScriptDao
import com.shifenmiao.database.teleprompter.entity.TeleprompterScriptEntity
import com.shifenmiao.database.transfer.ChatMessageDao
import com.shifenmiao.database.transfer.ChatMessageEntity
import com.shifenmiao.database.transfer.ChatSessionDao
import com.shifenmiao.database.transfer.ChatSessionEntity
import com.shifenmiao.database.watermark.dao.WatermarkTemplateDao
import com.shifenmiao.database.watermark.entity.WatermarkTemplateEntity
import com.shifenmiao.database.xiangqi.dao.XiangqiAiTaskDao
import com.shifenmiao.database.xiangqi.dao.XiangqiGameDao
import com.shifenmiao.database.xiangqi.dao.XiangqiPlyDao
import com.shifenmiao.database.xiangqi.entity.XiangqiAiTaskEntity
import com.shifenmiao.database.xiangqi.entity.XiangqiGameEntity
import com.shifenmiao.database.xiangqi.entity.XiangqiPlyEntity
import com.t8rin.imagetoolbox.core.utils.LocaleUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Feature-only database.
 *
 * IMPORTANT: This is intentionally separated from [AppDatabase] so that feature schemas can evolve
 * independently without affecting the main "one_box" database.
 */
@Database(
    entities = [
        MarkTodoCategoryEntity::class,
        MarkTodoTaskEntity::class,
        FrequencyEventEntity::class,
        PersonalMilestoneEntity::class,
        CountdownEventEntity::class,
        MilestoneAiInsightEntity::class,
        WatermarkTemplateEntity::class,
        IdPhotoSizeEntity::class,
        PaddleOcrTaskEntity::class,
        DocConvertTaskEntity::class,
        ScheduleEventEntity::class,
        ScheduleProviderBindingEntity::class,
        ScheduleSyncStateEntity::class,
        AltitudeRecordEntity::class,
        SpeedTestRecordEntity::class,
        SpeedTestConfigEntity::class,
        BookkeepingCategoryEntity::class,
        BookkeepingRecordEntity::class,
        TeleprompterScriptEntity::class,
        DataDraftEntity::class,
        XiangqiGameEntity::class,
        XiangqiPlyEntity::class,
        XiangqiAiTaskEntity::class,
        WheelEntity::class,
        WheelOptionEntity::class,
        WheelHistoryEntity::class,
        ChatMessageEntity::class,
        ChatSessionEntity::class,
        RecentAccessEntity::class,
        BlessingRecordEntity::class,
        BlessingWishEntity::class,
        BlessingTabConfigEntity::class,
        HabitEntity::class,
        HabitCheckInEntity::class,
        PoemEntity::class,
        AiDetectRecordEntity::class,
        HealthRecordEntity::class,
        HouseholdLocationEntity::class,
        HouseholdItemEntity::class,
        PeriodRecordEntity::class,
    ],
    version = 9,
    exportSchema = true
)
@TypeConverters(MarkTodoTypeConverters::class)
abstract class FeatureDatabase : RoomDatabase() {

    abstract fun paddleOcrTaskDao(): PaddleOcrTaskDao
    abstract fun docConvertTaskDao(): DocConvertTaskDao

    abstract fun markTodoCategoryDao(): MarkTodoCategoryDao

    abstract fun markTodoTaskDao(): MarkTodoTaskDao

    abstract fun markTodoDashboardDao(): MarkTodoDashboardDao

    abstract fun scheduleEventDao(): ScheduleEventDao

    abstract fun scheduleProviderBindingDao(): ScheduleProviderBindingDao

    abstract fun scheduleSyncStateDao(): ScheduleSyncStateDao

    abstract fun frequencyEventDao(): FrequencyEventDao

    abstract fun personalMilestoneDao(): PersonalMilestoneDao

    abstract fun countdownEventDao(): CountdownEventDao

    abstract fun milestoneAiInsightDao(): MilestoneAiInsightDao

    abstract fun watermarkTemplateDao(): WatermarkTemplateDao

    abstract fun idPhotoSizeDao(): IdPhotoSizeDao

    abstract fun altitudeRecordDao(): AltitudeRecordDao

    abstract fun speedTestRecordDao(): SpeedTestRecordDao

    abstract fun speedTestConfigDao(): SpeedTestConfigDao

    abstract fun bookkeepingCategoryDao(): BookkeepingCategoryDao

    abstract fun bookkeepingRecordDao(): BookkeepingRecordDao

    abstract fun teleprompterScriptDao(): TeleprompterScriptDao

    abstract fun dataDraftDao(): DataDraftDao

    abstract fun xiangqiGameDao(): XiangqiGameDao

    abstract fun xiangqiPlyDao(): XiangqiPlyDao

    abstract fun xiangqiAiTaskDao(): XiangqiAiTaskDao

    abstract fun wheelDao(): WheelDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun chatSessionDao(): ChatSessionDao

    abstract fun recentAccessDao(): RecentAccessDao

    abstract fun blessingRecordDao(): BlessingRecordDao
    abstract fun blessingWishDao(): BlessingWishDao
    abstract fun blessingTabConfigDao(): BlessingTabConfigDao

    abstract fun poemDao(): PoemDao

    abstract fun habitDao(): HabitDao

    abstract fun habitCheckInDao(): HabitCheckInDao

    abstract fun aiDetectRecordDao(): AiDetectRecordDao

    abstract fun healthRecordDao(): HealthRecordDao

    abstract fun householdLocationDao(): HouseholdLocationDao

    abstract fun householdItemDao(): HouseholdItemDao

    abstract fun periodRecordDao(): PeriodRecordDao

    companion object {
        private val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blessing_record` (
                        `id` TEXT NOT NULL,
                        `date` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `count` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_blessing_record_date` ON `blessing_record` (`date`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_blessing_record_date_type` ON `blessing_record` (`date`, `type`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blessing_wish` (
                        `date` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`date`, `type`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_blessing_wish_date` ON `blessing_wish` (`date`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blessing_tab_config` (
                        `date` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `subtitle` TEXT NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`date`, `type`)
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_2_3 = object : androidx.room.migration.Migration(2, 3) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `icon_key` TEXT NOT NULL DEFAULT 'waterdrop',
                        `color_argb` INTEGER,
                        `repeat_type` TEXT NOT NULL DEFAULT 'DAILY',
                        `repeat_target` INTEGER NOT NULL DEFAULT 1,
                        `weekdays_mask` INTEGER NOT NULL DEFAULT 0,
                        `remind_minutes` INTEGER,
                        `note` TEXT,
                        `stats_enabled` INTEGER NOT NULL DEFAULT 1,
                        `sort_order` INTEGER NOT NULL DEFAULT 0,
                        `is_archived` INTEGER NOT NULL DEFAULT 0,
                        `created_at` INTEGER NOT NULL DEFAULT 0,
                        `updated_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_check_in` (
                        `id` TEXT NOT NULL,
                        `habit_id` TEXT NOT NULL,
                        `date_epoch_day` INTEGER NOT NULL,
                        `checked_at` INTEGER NOT NULL DEFAULT 0,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`habit_id`) REFERENCES `habit`(`id`)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_check_in_habit_id_date_epoch_day` ON `habit_check_in` (`habit_id`, `date_epoch_day`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_habit_check_in_date_epoch_day` ON `habit_check_in` (`date_epoch_day`)")
            }
        }

        // 诗词模块:建表即含 pinyin/translation(v4 未发布过,原 4→5 的加列并入建表)
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `poem` (
                        `id` INTEGER NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `author` TEXT NOT NULL,
                        `dynasty` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `aiInsight` TEXT,
                        `pinyin` TEXT,
                        `translation` TEXT,
                        `isFavorite` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poem_createdAt` ON `poem` (`createdAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_poem_isFavorite` ON `poem` (`isFavorite`)")
            }
        }

        // AI 检测助手:检测历史表
        private val MIGRATION_4_5 = object : androidx.room.migration.Migration(4, 5) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `ai_detect_record` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `type` TEXT NOT NULL,
                        `inputSummary` TEXT NOT NULL,
                        `probability` REAL NOT NULL,
                        `verdict` TEXT NOT NULL,
                        `detailJson` TEXT NOT NULL DEFAULT '',
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_ai_detect_record_createdAt` ON `ai_detect_record` (`createdAt`)")
            }
        }

        // 记录中心(健康记录):健康记录表
        private val MIGRATION_5_6 = object : androidx.room.migration.Migration(5, 6) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `health_record` (
                        `id` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `happened_at` INTEGER NOT NULL,
                        `fields_json` TEXT NOT NULL,
                        `note` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_record_happened_at` ON `health_record` (`happened_at`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_record_type` ON `health_record` (`type`)")
            }
        }

        // 家庭物品管理:位置(自关联层级)+ 物品表,SQL 与 schemas/.../7.json 的 createSql 一致
        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `household_location` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `parent_id` TEXT,
                        `sort_order` INTEGER NOT NULL,
                        `created_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`parent_id`) REFERENCES `household_location`(`id`)
                            ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_household_location_parent_id` ON `household_location` (`parent_id`)")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `household_item` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT,
                        `location_id` TEXT,
                        `expire_at` INTEGER,
                        `photo_path` TEXT,
                        `note` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`location_id`) REFERENCES `household_location`(`id`)
                            ON UPDATE CASCADE ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_household_item_location_id` ON `household_item` (`location_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_household_item_name` ON `household_item` (`name`)")
            }
        }

        // 家庭物品:位置表加用户自选图标列(icon_key,IconRegistry key)
        private val MIGRATION_7_8 = object : androidx.room.migration.Migration(7, 8) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `household_location` ADD COLUMN `icon_key` TEXT")
            }
        }

        // 经期记录表
        //
        // 注意：v9 尚未发布（已发布的最高 tag 1.3.9 对应 version = 4），因此本迁移是"草稿"，
        // 发版前可以继续往里追加改动。与之相关的决策转盘字段改造（weight/enabled/wheelTitle + 索引）
        // 也一并并入此处，避免为了同一个未发布版本再叠一层 9→10。
        // 先例见 commit 9506c641「poem 表迁移合并为单步(v4 未发布)」。
        private val MIGRATION_8_9 = object : androidx.room.migration.Migration(8, 9) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `period_record` (
                        `id` TEXT NOT NULL,
                        `record_date` INTEGER NOT NULL,
                        `is_period_start` INTEGER NOT NULL,
                        `is_period_end` INTEGER NOT NULL,
                        `flow_intensity` TEXT NOT NULL,
                        `symptoms` TEXT NOT NULL,
                        `mood` TEXT NOT NULL,
                        `note` TEXT,
                        `created_at` INTEGER NOT NULL,
                        `updated_at` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_period_record_record_date` ON `period_record` (`record_date`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_period_record_is_period_start` ON `period_record` (`is_period_start`)")

                // 决策转盘：选项权重与启用标记（抽后移除）
                db.execSQL("ALTER TABLE `wheel_options` ADD COLUMN `weight` REAL NOT NULL DEFAULT 1.0")
                db.execSQL("ALTER TABLE `wheel_options` ADD COLUMN `enabled` INTEGER NOT NULL DEFAULT 1")

                // 决策转盘：历史冗余转盘标题，避免转盘删除后历史无归属
                db.execSQL("ALTER TABLE `wheel_history` ADD COLUMN `wheelTitle` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    UPDATE `wheel_history` SET `wheelTitle` = COALESCE(
                        (SELECT `title` FROM `decision_wheels` WHERE `decision_wheels`.`id` = `wheel_history`.`wheelId`),
                        ''
                    )
                    """.trimIndent()
                )

                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wheel_options_wheelId` ON `wheel_options` (`wheelId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_wheel_history_wheelId` ON `wheel_history` (`wheelId`)")
            }
        }

        const val DB_NAME_PREFIX: String = "feature"

        // 语言切换后进程会冷重启（见 LocaleSwitchWatcher），Hilt @Singleton 注入的库实例
        // 因此总能对应当前语言；getInstanceOrCreate 内的懒切换是直连调用方
        // （如 FileTransferServer）在重启完成前的兜底。
        @Volatile
        private var INSTANCE: FeatureDatabase? = null

        @Volatile
        private var INSTANCE_LOCALE: String? = null

        fun dbNameForLocale(locale: String = LocaleUtils.getCurrentLocaleTag()): String {
            return "${DB_NAME_PREFIX}_${locale}.db"
        }

        fun closeInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {
                    // ignore
                } finally {
                    INSTANCE = null
                    INSTANCE_LOCALE = null
                }
            }
        }

        fun getInstanceOrCreate(@ApplicationContext context: Context): FeatureDatabase {
            val currentLocale = LocaleUtils.getCurrentLocaleTag()
            val currentDbName = dbNameForLocale(currentLocale)

            val existing = INSTANCE
            if (existing != null && INSTANCE_LOCALE == currentLocale) {
                return existing
            }

            return synchronized(this) {
                if (INSTANCE != null && INSTANCE_LOCALE == currentLocale) {
                    return@synchronized INSTANCE!!
                }

                closeInstance()

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FeatureDatabase::class.java,
                    currentDbName
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                    )
                    .fallbackToDestructiveMigration(true)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            try {
                                val inputStream =
                                    context.resources.openRawResource(R.raw.marktodo_presets)
                                val reader = BufferedReader(InputStreamReader(inputStream))
                                val sql = StringBuilder()
                                var line: String?
                                db.beginTransaction()
                                try {
                                    while (reader.readLine().also { line = it } != null) {
                                        val trimmedLine = line!!.trim()
                                        if (trimmedLine.isEmpty() || trimmedLine.startsWith("--")) {
                                            continue
                                        }
                                        sql.append(line).append("\n")
                                        if (trimmedLine.endsWith(";")) {
                                            db.execSQL(sql.toString())
                                            sql.setLength(0)
                                        }
                                    }
                                    db.setTransactionSuccessful()
                                } finally {
                                    db.endTransaction()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }

                        override fun onOpen(db: SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Ensure foreign keys are enforced.
                            db.execSQL("PRAGMA foreign_keys=ON")
                        }
                    })
                    .build()

                INSTANCE = instance
                INSTANCE_LOCALE = currentLocale
                instance
            }
        }
    }
}
