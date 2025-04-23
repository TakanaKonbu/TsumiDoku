package com.takanakonbu.tsumidoku.ui.edit // パッケージ名は適宜調整

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.takanakonbu.tsumidoku.data.Book // Book データクラス
import com.takanakonbu.tsumidoku.data.BookStatus // BookStatus Enum
import com.takanakonbu.tsumidoku.ui.theme.PrimaryColor

/**
 * 書籍編集用ダイアログの Composable
 *
 * @param book 編集対象の書籍データ
 * @param onDismissRequest ダイアログを閉じるよう要求されたときの処理
 * @param onSaveClick 「保存」ボタンがクリックされたときの処理 (更新後のタイトル, 著者名, メモ, ステータス, 新しい画像URI を渡す)
 */
@Composable
fun EditBookDialog(
    book: Book, // 編集対象の書籍を受け取る
    onDismissRequest: () -> Unit,
    onSaveClick: (title: String, author: String, memo: String, status: BookStatus, newImageUri: Uri?) -> Unit
) {
    // --- ダイアログ内の編集状態を保持 ---
    // remember のキーに book.id を指定し、編集対象が変わったら状態をリセットする
    // ただしダイアログ表示中に book が外部から変わることは通常ないので、単純な remember でも可
    var title by remember(book.id) { mutableStateOf(book.title) }
    var author by remember(book.id) { mutableStateOf(book.author) }
    var memo by remember(book.id) { mutableStateOf(book.memo ?: "") } // null なら空文字
    var status by remember(book.id) { mutableStateOf(book.status) }
    var newSelectedImageUri by remember(book.id) { mutableStateOf<Uri?>(null) } // 新しく選択されたURI
    var currentImageByteArray by remember(book.id) { mutableStateOf(book.coverImage) } // 現在表示する画像データ

    var isTitleError by remember { mutableStateOf(false) }
    var isAuthorError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    // フォトピッカーランチャー
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            newSelectedImageUri = uri // 新しく選択されたURIを保持
            currentImageByteArray = null // 新しいURIが選択されたら、既存のByteArrayはクリア (プレビュー用)
        }
    )
    // ------------------------------------

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(), // 高さはコンテンツに合わせる
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(
                containerColor = Color.White // ここで背景色を白に指定
            )
        ) {
            // 長くなる可能性があるので縦スクロール可能にする
            Column(modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()) // 縦スクロール
            ) {
                Text("書籍の編集", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                // --- タイトル編集欄 ---
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        isTitleError = it.isBlank()
                    },
                    label = { Text("タイトル *") },
                    isError = isTitleError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isTitleError) {
                    Text("タイトルは必須です", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- 著者名編集欄 ---
                OutlinedTextField(
                    value = author,
                    onValueChange = {
                        author = it
                        isAuthorError = it.isBlank()
                    },
                    label = { Text("著者名 *") },
                    isError = isAuthorError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isAuthorError) {
                    Text("著者名は必須です", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // --- ステータス選択 ---
                Text(
                    text = "ステータス *",
                    style = MaterialTheme.typography.bodyMedium
                )
// Column を使って縦に並べる
                Column(
                    modifier = Modifier
                        .selectableGroup()
                        .fillMaxWidth()
                ) {
                    BookStatus.entries.forEach { bookStatus ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(30.dp)
                                .selectable(
                                    selected = (bookStatus == status), // 現在選択されているか
                                    onClick = { status = bookStatus }, // クリックで状態を更新
                                    role = Role.RadioButton
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (bookStatus == status),
                                onClick = null, // Row の selectable で処理
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryColor // 選択時の色を赤に設定
                                )
                            )
                            Text(
                                text = statusToString(bookStatus), // 表示文字列
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                // --------------------

                // --- メモ編集欄 ---
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                // ----------------

                // --- 書影編集エリア ---
                Text("書影 (任意)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(100.dp, 150.dp)
                        .border(1.dp, Color.Gray)
                        .clickable { // フォトピッカー起動
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 表示する画像ソースを決定
                    // 1. 新しく選択されたURIがあれば、それを表示試行
                    // 2. 新しいURIがなく、既存のByteArrayがあればそれを表示
                    // 3. どちらもなければアイコン表示
                    var bitmapToShow: Bitmap? = null
                    if (newSelectedImageUri != null) {
                        // 新しいURIからBitmap読み込み
                        bitmapToShow = remember(newSelectedImageUri) { // キーをURIにする
                            uriToBitmap(context, newSelectedImageUri)
                        }
                    } else if (currentImageByteArray != null) {
                        // 既存のByteArrayからBitmap読み込み
                        bitmapToShow = remember(currentImageByteArray) { // キーをByteArrayにする
                            byteArrayToBitmap(currentImageByteArray)
                        }
                    }

                    if (bitmapToShow != null) {
                        Image(
                            bitmap = bitmapToShow.asImageBitmap(),
                            contentDescription = "書影",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        // 表示する画像がない場合
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = "書影を選択または変更",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                // --------------------

                // --- ボタン ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("キャンセル")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            isTitleError = title.isBlank()
                            isAuthorError = author.isBlank()
                            if (!isTitleError && !isAuthorError) {
                                // 更新後の情報をコールバックで渡す
                                onSaveClick(title, author, memo, status, newSelectedImageUri)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryColor
                        )
                    ) {
                        Text("保存")
                    }
                }
                // ----------------
            }
        }
    }
}

// --- ヘルパー関数 ---

// BookStatus を表示用文字列に変換 (BookListScreen と重複するが一旦ここに置く)
@Composable
private fun statusToString(status: BookStatus): String {
    return when (status) {
        BookStatus.UNREAD -> "未読"
        BookStatus.READING -> "読書中"
        BookStatus.READ -> "読破"
    }
}

// URIからBitmapを読み込むヘルパー (AddBookDialogと共通化可能)
private fun uriToBitmap(context: Context, uri: Uri?): Bitmap? {
    if (uri == null) return null
    return try {
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// ByteArrayからBitmapを読み込むヘルパー
private fun byteArrayToBitmap(byteArray: ByteArray?): Bitmap? {
    if (byteArray == null || byteArray.isEmpty()) return null
    return try {
        BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

