package com.takanakonbu.tsumidoku.data

import kotlinx.coroutines.flow.Flow

/**
 * 書籍データへのアクセスを提供するリポジトリのインターフェース
 */
interface BookRepository {

    /** 全ての書籍を取得する Flow */
    fun getAllBooks(): Flow<List<Book>>

    /** IDを指定して書籍を取得する Flow */
    fun getBookById(id: String): Flow<Book?>

    /** 書籍を挿入（または更新）する */
    suspend fun insertBook(book: Book)

    /** 書籍を更新する */
    suspend fun updateBook(book: Book)

    /** 書籍を削除する */
    suspend fun deleteBook(book: Book)
}