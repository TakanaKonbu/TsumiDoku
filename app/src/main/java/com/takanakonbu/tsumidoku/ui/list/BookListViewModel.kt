package com.takanakonbu.tsumidoku.ui.list

import android.app.Application // Application をインポート
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takanakonbu.tsumidoku.data.Book
import com.takanakonbu.tsumidoku.data.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers // IOディスパッチャを使うため
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext // withContext を使うため
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltViewModel
class BookListViewModel @Inject constructor(
    private val bookRepository: BookRepository,
    private val application: Application // Application (Contextを提供) を Hilt から注入
) : ViewModel() {

    val books: StateFlow<List<Book>> = bookRepository.getAllBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = emptyList()
        )

    fun addBook(title: String, author: String, memo: String, imageUri: Uri?) {
        viewModelScope.launch {
            // 画像変換処理はIO処理なので、IOディスパッチャで行う
            val coverImageByteArray: ByteArray? = if (imageUri != null) {
                withContext(Dispatchers.IO) {
                    // ヘルパー関数を呼び出して ByteArray を取得
                    createThumbnailByteArray(application, imageUri, 300, 450) // 例: 300x450 にリサイズ
                }
            } else {
                null
            }

            val newBook = Book(
                title = title,
                author = author,
                memo = memo.ifBlank { null },
                coverImage = coverImageByteArray // 変換後の ByteArray を設定
            )
            bookRepository.insertBook(newBook)
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
    private fun createThumbnailByteArray(
        context: Context,
        uri: Uri,
        reqWidth: Int,
        reqHeight: Int,
        quality: Int = 80 // JPEG品質 (デフォルト80)
    ): ByteArray? {
        return try {
            // 1. まず画像のサイズだけを読み込む
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true // Bitmapをメモリに読み込まず、サイズ情報だけ取得
            }
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

            // 2. 適切なサンプリングサイズを計算 (メモリ節約のため)
            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)

            // 3. サンプリングサイズを設定してBitmapを読み込む
            options.inJustDecodeBounds = false
            val bitmap = BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)

            // 4. 必要であればさらにリサイズ (オプション)
            // ここでは読み込み時のサンプリングである程度小さくなっている想定だが、
            // より正確なサイズにしたい場合や、アスペクト比を維持したい場合は追加処理を行う
            // val scaledBitmap = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true) // 例

            // 5. ByteArrayOutputStreamを使ってJPEG形式でByteArrayに変換
            ByteArrayOutputStream().use { stream ->
                bitmap?.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                stream.toByteArray()
            }
        } catch (e: Exception) {
            e.printStackTrace() // エラーログ出力
            null // エラー時はnullを返す
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
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
    // --- ヘルパー関数ここまで ---
}