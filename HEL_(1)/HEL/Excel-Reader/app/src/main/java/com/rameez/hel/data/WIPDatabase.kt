package com.rameez.hel.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.rameez.hel.data.model.WIPModel
import com.rameez.hel.data.model.ArticleModel
import com.rameez.hel.utils.ApplicationClass
import com.rameez.hel.utils.MIGRATION_1_2
import com.rameez.hel.utils.MIGRATION_2_3
import com.rameez.hel.utils.MIGRATION_3_4
import com.rameez.hel.utils.MIGRATION_4_5
import com.rameez.hel.utils.MIGRATION_5_6

@Database(entities = [WIPModel::class, ArticleModel::class], version = 6)
@TypeConverters(Converters::class)
abstract class WIPDatabase : RoomDatabase() {
    abstract fun wipDao(): WIPDao

    companion object {
        private var INSTANCE: WIPDatabase? = null
        fun getDatabase(): WIPDatabase? {
            if (INSTANCE == null) {
                synchronized(this) {
                    INSTANCE = Room.databaseBuilder(
                        ApplicationClass.application.baseContext,
                        WIPDatabase::class.java,
                        "wips_database"
                    )
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6)
                        .build()
                }
            }
            return INSTANCE
        }
    }
}
