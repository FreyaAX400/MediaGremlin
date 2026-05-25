package com.freya.mediagremlin.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Post::class, Source::class, Tag::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun postDao(): PostDao
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE posts ADD COLUMN isTextOnly INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE posts ADD COLUMN urlNormalized TEXT NOT NULL DEFAULT ''")
                db.execSQL("UPDATE posts SET urlNormalized = url")
                // Remove all duplicates (including saved ones if they have duplicate URLs) before adding unique index
                db.execSQL("DELETE FROM posts WHERE rowid NOT IN (SELECT MAX(rowid) FROM posts GROUP BY urlNormalized)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_posts_urlNormalized ON posts(urlNormalized)")
            }
        }

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mediagremlin.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also { INSTANCE = it }
            }
    }
}