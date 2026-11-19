package com.metrolist.music.reader.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderDao {
    @Query("SELECT * FROM reader_books ORDER BY lastReadAt DESC, addedAt DESC")
    fun books(): Flow<List<ReaderBookEntity>>

    @Query("SELECT * FROM reader_books WHERE id = :bookId LIMIT 1")
    fun book(bookId: String): Flow<ReaderBookEntity?>

    @Query("SELECT * FROM reader_books WHERE id = :bookId LIMIT 1")
    suspend fun bookOnce(bookId: String): ReaderBookEntity?

    @Query("SELECT * FROM reader_books WHERE storyId = :storyId ORDER BY lastReadAt DESC, addedAt DESC")
    suspend fun booksForStory(storyId: String): List<ReaderBookEntity>

    @Query("SELECT * FROM reader_books WHERE storyId = :storyId AND chapterUrl = :chapterUrl LIMIT 1")
    suspend fun chapterForStory(storyId: String, chapterUrl: String): ReaderBookEntity?

    @Query("SELECT * FROM reader_pages WHERE bookId = :bookId ORDER BY pageIndex")
    fun pages(bookId: String): Flow<List<ReaderPageEntity>>

    @Query("SELECT * FROM reader_pages WHERE bookId = :bookId ORDER BY pageIndex")
    suspend fun pagesOnce(bookId: String): List<ReaderPageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: ReaderBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<ReaderPageEntity>)

    @Query("UPDATE reader_books SET currentPage = :page, lastReadAt = :readAt WHERE id = :bookId")
    suspend fun updateProgress(bookId: String, page: Int, readAt: Long)

    @Query("UPDATE reader_books SET downloadedAt = :downloadedAt WHERE id = :bookId")
    suspend fun markDownloaded(bookId: String, downloadedAt: Long)

    @Query("UPDATE reader_pages SET filePath = :filePath WHERE bookId = :bookId AND pageIndex = :pageIndex")
    suspend fun updatePagePath(bookId: String, pageIndex: Int, filePath: String)

    @Query("DELETE FROM reader_pages WHERE bookId = :bookId")
    suspend fun deletePages(bookId: String)

    @Query("DELETE FROM reader_books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)
}
