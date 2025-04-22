package com.takanakonbu.tsumidoku.ui.list

import android.net.Uri // Uri をインポート
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takanakonbu.tsumidoku.data.Book
import com.takanakonbu.tsumidoku.data.BookRepository
import com.takanakonbu.tsumidoku.data.BookStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BookListViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    /**
     * 新しい書籍をデータベースに追加する
     * @param title 書籍タイトル
     * @param author 著者名
     * @param memo メモ (空の場合あり)
     * @param imageUri 選択された書影のURI (nullの場合あり)
     */
    fun addBook(title: String, author: String, memo: String, imageUri: Uri?) {
        viewModelScope.launch {
            // TODO: imageUri から ByteArray への変換処理をここに追加する (次のステップ)
            val coverImageByteArray: ByteArray? = null // 現時点では null を設定

            val newBook = Book(
                title = title,
                author = author,
                memo = memo.ifBlank { null },
                coverImage = coverImageByteArray,
            )
            bookRepository.insertBook(newBook)
        }
    }

    /**
     * 指定された書籍を削除する
     * @param book 削除対象の書籍オブジェクト
     */
    fun deleteBook(book: Book) {
        viewModelScope.launch {
            bookRepository.deleteBook(book)
        }
    }
}