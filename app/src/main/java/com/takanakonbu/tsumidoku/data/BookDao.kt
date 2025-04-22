package com.takanakonbu.tsumidoku.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * 書籍テーブル (books) へのデータアクセスオブジェクト (DAO)
 */
@Dao
interface BookDao {

    /**
     * 全ての書籍を追加日時の降順で取得する Flow
     * Flow を使うことで、データベースの変更が自動的に通知される
     */
    @Query("SELECT * FROM books ORDER BY added_date DESC")
    fun getAllBooks(): Flow<List<Book>>

    /**
     * 指定されたIDの書籍を取得する Flow (見つからない場合は null)
     */
    @Query("SELECT * FROM books WHERE id = :id")
    fun getBookById(id: String): Flow<Book?>

    /**
     * 新しい書籍を挿入する (IDが競合した場合は置き換える)
     * Coroutineで実行するため suspend fun
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: Book)

    /**
     * 既存の書籍情報を更新する
     * Coroutineで実行するため suspend fun
     */
    @Update
    suspend fun updateBook(book: Book)

    /**
     * 指定された書籍を削除する
     * Coroutineで実行するため suspend fun
     */
    @Delete
    suspend fun deleteBook(book: Book)

    // 必要であれば他のクエリメソッドを追加
    // 例: タイトルで検索する
    // @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' ORDER BY added_date DESC")
    // fun searchBooksByTitle(query: String): Flow<List<Book>>
}