package com.takanakonbu.tsumidoku.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takanakonbu.tsumidoku.data.Book // Book エンティティ
import com.takanakonbu.tsumidoku.data.BookRepository // 作成したリポジトリインターフェース
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map // 必要に応じて map を使う
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 書籍リスト画面 (BookListScreen) のための ViewModel
 *
 * @param bookRepository 書籍データにアクセスするためのリポジトリ (Hiltが注入)
 */
@HiltViewModel
class BookListViewModel @Inject constructor(
    private val bookRepository: BookRepository
) : ViewModel() {

    // UI状態を保持する StateFlow
    // repository.getAllBooks() が Flow<List<Book>> を返すので、
    // それを viewModelScope で収集し、StateFlow<List<Book>> に変換する
    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(
            scope = viewModelScope, // ViewModel の Coroutine スコープ
            // started = SharingStarted.Lazily, // 画面が表示されてから収集を開始する場合
            started = SharingStarted.WhileSubscribed(5000L), // 画面がサブスクライブしている間 + 5秒間収集を維持
            initialValue = emptyList() // 初期値は空リスト
        )

    // ----- 書籍削除処理を追加する場合の例 -----
    /**
     * 指定された書籍を削除する
     * @param book 削除対象の書籍オブジェクト
     */
    fun deleteBook(book: Book) {
        // viewModelScope を使って非同期でデータベース操作を実行
        viewModelScope.launch {
            bookRepository.deleteBook(book)
            // 必要であれば削除成功/失敗などのUIイベント通知処理を追加
        }
    }


}