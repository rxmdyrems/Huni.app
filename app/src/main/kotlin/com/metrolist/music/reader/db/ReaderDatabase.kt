package com.metrolist.music.reader.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ReaderBookEntity::class, ReaderPageEntity::class], version = 1, exportSchema = false)
abstract class ReaderDatabase : RoomDatabase() {
    abstract fun readerDao(): ReaderDao
}
