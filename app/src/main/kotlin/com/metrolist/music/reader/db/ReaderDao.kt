package com.metrolist.music.reader.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReaderDao {
    @Query("SELECT * FROM reader_books ORDER BY addedAt DESC")
    fun books(): Flow<List<ReaderBookEntity>>

    @Query("SELECT * FROM reader_books WHERE id = :bookId LIMIT 1")
    fun book(bookId: String): Flow<ReaderBookEntity?>

    @Query("SELECT * FROM reader_pages WHERE bookId = :bookId ORDER BY pageIndex")
    fun pages(bookId: String): Flow<List<ReaderPageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: ReaderBookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<ReaderPageEntity>)

    @Query("UPDATE reader_books SET currentPage = :page WHERE id = :bookId")
    suspend fun updateProgress(bookId: String, page: Int)

    @Query("DELETE FROM reader_pages WHERE bookId = :bookId")
    suspend fun deletePages(bookId: String)

    @Query("DELETE FROM reader_books WHERE id = :bookId")
    suspend fun deleteBook(bookId: String)
}
