package com.takanakonbu.tsumidoku.ui.list

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takanakonbu.tsumidoku.data.Book
import com.takanakonbu.tsumidoku.data.BookRepository
import com.takanakonbu.tsumidoku.data.BookStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.Collator
import java.util.Locale
import javax.inject.Inject

enum class SortOrder {
    ADDED_DATE_DESC,
    ADDED_DATE_ASC,
    TITLE_ASC,
    TITLE_DESC,
    AUTHOR_ASC,
    AUTHOR_DESC
}

@HiltViewModel
class BookListViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val application: Application
) : ViewModel() {

    private val _sortOrder = MutableStateFlow(SortOrder.ADDED_DATE_DESC)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _filterStatus = MutableStateFlow<BookStatus?>(null)
    val filterStatus: StateFlow<BookStatus?> = _filterStatus

    private val collator = Collator.getInstance(Locale.JAPANESE)

    val books: StateFlow<List<Book>> = combine(
        bookRepository.getAllBooks(),
        _sortOrder,
        _filterStatus
    ) { bookList, sort, filter ->

        // 1. フィルター処理
        val filteredList = if (filter == null) {
            bookList
        } else {
            bookList.filter { it.status == filter }
        }

        // ★★★ ここから変更 ★★★

        // 2. ステータスでリストを分割 (読破を下に持っていくため)
        // partition は Pair<List<Book>, List<Book>> を返す
        // Pair.first が条件に合うもの (読破以外)、Pair.second が条件に合わないもの (読破)
        val (nonReadBooks, readBooks) = filteredList.partition { it.status != BookStatus.READ }

        // 3. 各リストを選択された順序でソートする関数を定義
        val sortList: (List<Book>) -> List<Book> = { listToSort ->
            when (sort) {
                SortOrder.ADDED_DATE_DESC -> listToSort.sortedByDescending { it.addedDate }
                SortOrder.ADDED_DATE_ASC -> listToSort.sortedBy { it.addedDate }
                SortOrder.TITLE_ASC -> listToSort.sortedWith(compareBy(collator) { it.title })
                SortOrder.TITLE_DESC -> listToSort.sortedWith(compareByDescending(collator) { it.title })
                SortOrder.AUTHOR_ASC -> listToSort.sortedWith(compareBy(collator) { it.author })
                SortOrder.AUTHOR_DESC -> listToSort.sortedWith(compareByDescending(collator) { it.author })
            }
        }

        // 4. 各リストをソート
        val sortedNonReadBooks = sortList(nonReadBooks)
        val sortedReadBooks = sortList(readBooks)

        // 5. ソート済みのリストを結合 (読破以外 + 読破)
        sortedNonReadBooks + sortedReadBooks

        // ★★★ 変更ここまで ★★★

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = emptyList()
    )

    fun changeSortOrder(newSortOrder: SortOrder) {
        _sortOrder.value = newSortOrder
    }

    fun changeFilterStatus(newFilterStatus: BookStatus?) {
        _filterStatus.value = newFilterStatus
    }

    // --- 以降の addBook, updateBook, deleteBook, ヘルパー関数は変更なし ---
    // (省略)
    fun addBook(title: String, author: String, memo: String, imageUri: Uri?) {
        viewModelScope.launch {
            val coverImageByteArray: ByteArray? = if (imageUri != null) {
                withContext(Dispatchers.IO) {
                    createThumbnailByteArray(application, imageUri, 300, 450)
                }
            } else {
                null
            }

            val newBook = Book(
                title = title,
                author = author,
                memo = memo.ifBlank { null },
                coverImage = coverImageByteArray
            )
            bookRepository.insertBook(newBook)
        }
    }

    fun updateBook(
        id: String,
        title: String,
        author: String,
        memo: String,
        status: BookStatus,
        newImageUri: Uri?
    ) {
        viewModelScope.launch {
            val currentBook = books.value.find { it.id == id }
            if (currentBook == null) {
                println("WARN: Book with id $id not found for update.")
                return@launch
            }

            val coverImageByteArray: ByteArray? = if (newImageUri != null) {
                withContext(Dispatchers.IO) {
                    createThumbnailByteArray(application, newImageUri, 300, 450)
                }
            } else {
                currentBook.coverImage
            }

            val readDate: Long? = if (currentBook.status != BookStatus.READ && status == BookStatus.READ) {
                System.currentTimeMillis()
            } else if (status != BookStatus.READ) {
                null
            } else {
                currentBook.readDate
            }

            val updatedBook = Book(
                id = id,
                title = title,
                author = author,
                memo = memo.ifBlank { null },
                status = status,
                coverImage = coverImageByteArray,
                addedDate = currentBook.addedDate,
                readDate = readDate
            )
            bookRepository.updateBook(updatedBook)
        }
    }

    fun deleteBook(book: Book) {
        viewModelScope.launch {
            bookRepository.deleteBook(book)
        }
    }

    private fun createThumbnailByteArray(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int,
        quality: Int = 80
    ): ByteArray? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)
            ByteArrayOutputStream().use { stream ->
                bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

}