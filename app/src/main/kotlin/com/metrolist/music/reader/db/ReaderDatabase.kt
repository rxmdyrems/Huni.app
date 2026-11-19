package com.metrolist.music.reader.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [ReaderBookEntity::class, ReaderPageEntity::class], version = 3, exportSchema = false)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reader_books ADD COLUMN referer TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE reader_books ADD COLUMN storyId TEXT")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN storyTitle TEXT")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN chapterUrl TEXT")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN chapterName TEXT")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN mangaUrl TEXT")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN sourcePackage TEXT")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN sourceId INTEGER")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN coverUrl TEXT")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN lastReadAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE reader_books ADD COLUMN downloadedAt INTEGER")
            }
        }
    }
}
