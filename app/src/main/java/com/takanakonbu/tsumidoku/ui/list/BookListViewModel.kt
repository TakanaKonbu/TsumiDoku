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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class BookListViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val application: Application
) : ViewModel() {

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        // --- Flow の結果を加工する map 演算子を追加 ---
        .map { bookList ->
            // ステータスが READ でないものを先に、READ のものを後に並び替える
            // 各グループ内では元の addedDate 降順を維持するように安定ソートを行う
            bookList.sortedWith(compareBy { it.status == BookStatus.READ })
            // もし読了済みの中でさらに読了日順などでソートしたい場合は、
            // .sortedWith(compareBy<Book> { it.status == BookStatus.READ }
            //     .thenByDescending { if (it.status == BookStatus.READ) it.readDate else it.addedDate } // 例
            // )
            // のように comparator を工夫する
        }
        // --------------------------------------------
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addBook(title: String, author: String, memo: String, imageUri: Uri?) {
        viewModelScope.launch {
            val coverImageByteArray: ByteArray? = if (imageUri != null) {
                withContext(Dispatchers.IO) {
                    // 正しい引数を渡して呼び出し
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
            val currentBook = books.value.find { it.id == id } ?: return@launch

            val coverImageByteArray: ByteArray? = if (newImageUri != null) {
                withContext(Dispatchers.IO) {
                    // 正しい引数を渡して呼び出し
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

    // --- ヘルパー関数: URIからリサイズされたByteArrayを作成 ---
    /**
     * 指定されたURIから画像を読み込み、指定サイズに近い形にリサイズしてJPEG形式のByteArrayを返す。
     * エラー発生時は null を返す。
     *
     * @param context コンテキスト
     * @param uri 画像のURI
     * @param reqWidth 要求する幅
     * @param reqHeight 要求する高さ
     * @param quality JPEG圧縮品質 (0-100)
     * @return リサイズされた画像のByteArray、またはエラー時にnull
     */
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
    // ここに正しい引数を定義します
    // ★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★★
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
            // 引数 uri を使用
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

            // 引数 reqWidth, reqHeight を使用
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            options.inJustDecodeBounds = false
            // 引数 uri を使用
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

            ByteArrayOutputStream().use { stream ->
                // 引数 quality を使用
                bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * BitmapFactory.Options と要求される幅・高さから適切な inSampleSize を計算するヘルパー関数
     */
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