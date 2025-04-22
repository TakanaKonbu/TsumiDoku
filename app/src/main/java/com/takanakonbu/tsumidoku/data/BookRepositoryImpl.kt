package com.takanakonbu.tsumidoku.data

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * BookRepositoryインターフェースの実装クラス
 * BookDaoを使って実際のデータ操作を行う
 */
@Singleton // アプリ内で単一のインスタンスになるように指定
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao // Hiltが BookDao を自動で注入してくれる
) : BookRepository {

    override fun getAllBooks(): Flow<List<Book>> {
        return bookDao.getAllBooks()
    }

    override fun getBookById(id: String): Flow<Book?> {
        return bookDao.getBookById(id)
    }

    override suspend fun insertBook(book: Book) {
        bookDao.insertBook(book)
    }

    override suspend fun updateBook(book: Book) {
        bookDao.updateBook(book)
    }

    override suspend fun deleteBook(book: Book) {
        bookDao.deleteBook(book)
    }
}