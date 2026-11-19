package com.metrolist.music.reader.db

import androidx.room.Entity
import androidx.room.ColumnInfo
import androidx.room.PrimaryKey

@Entity(tableName = "reader_books")
data class ReaderBookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val sourceName: String,
    val referer: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val currentPage: Int = 0,
    val storyId: String? = null,
    val storyTitle: String? = null,
    val chapterUrl: String? = null,
    val chapterName: String? = null,
    val mangaUrl: String? = null,
    val sourcePackage: String? = null,
    val sourceId: Long? = null,
    val coverUrl: String? = null,
    @ColumnInfo(defaultValue = "0") val lastReadAt: Long = addedAt,
    val downloadedAt: Long? = null,
)

@Entity(tableName = "reader_pages", primaryKeys = ["bookId", "pageIndex"])
data class ReaderPageEntity(
    val bookId: String,
    val pageIndex: Int,
    val filePath: String,
)
