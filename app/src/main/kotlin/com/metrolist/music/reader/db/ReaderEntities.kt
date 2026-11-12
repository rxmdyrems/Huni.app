package com.metrolist.music.reader.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reader_books")
data class ReaderBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceName: String,
    val addedAt: Long = System.currentTimeMillis(),
    val currentPage: Int = 0,
)

@Entity(tableName = "reader_pages", primaryKeys = ["bookId", "pageIndex"])
data class ReaderPageEntity(
    val bookId: String,
    val pageIndex: Int,
    val filePath: String,
)
