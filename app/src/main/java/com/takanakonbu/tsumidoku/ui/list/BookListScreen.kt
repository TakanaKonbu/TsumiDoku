package com.takanakonbu.tsumidoku.ui.list

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takanakonbu.tsumidoku.R
import com.takanakonbu.tsumidoku.data.Book
import com.takanakonbu.tsumidoku.data.BookStatus
import com.takanakonbu.tsumidoku.ui.add.AddBookDialog
import com.takanakonbu.tsumidoku.ui.edit.EditBookDialog // EditBookDialog をインポート

import com.takanakonbu.tsumidoku.ui.theme.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    viewModel: BookListViewModel = hiltViewModel()
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    var showAddDialog by remember { mutableStateOf(false) }

    // --- 編集対象の書籍を保持する状態 ---
    var editingBook by remember { mutableStateOf<Book?>(null) }
    // ---------------------------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor,
                    titleContentColor = Color.White // MaterialTheme.colorScheme.onPrimary の方が良いかも
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryColor,
                contentColor = Color.White // contentColorFor(PrimaryColor) の方が良いかも
            ) {
                Icon(Icons.Filled.Add, contentDescription = "書籍を追加")
            }
        }
    ) { innerPadding ->

        // --- 追加ダイアログ表示 ---
        if (showAddDialog) {
            AddBookDialog(
                onDismissRequest = { showAddDialog = false },
                onAddClick = { title, author, memo, imageUri ->
                    viewModel.addBook(title, author, memo, imageUri)
                    showAddDialog = false
                }
            )
        }
        // -------------------------

        // --- 編集ダイアログ表示 ---
        editingBook?.let { bookToEdit -> // editingBook が null でなければ表示
            EditBookDialog(
                book = bookToEdit,
                onDismissRequest = { editingBook = null },
                onSaveClick = { title, author, memo, status, newImageUri ->
                    // ViewModel の updateBook 関数を呼び出す
                    viewModel.updateBook(
                        id = bookToEdit.id, // ★ IDを渡す
                        title = title,
                        author = author,
                        memo = memo,
                        status = status,
                        newImageUri = newImageUri
                    )
                    editingBook = null // 編集対象を null に戻して閉じる
                }
            )
        }
        // -------------------------

        // --- リスト表示 ---
        if (books.isEmpty()) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("まだ書籍が登録されていません。")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(horizontal = 8.dp)
            ) {
                // --- items を itemsIndexed に変更 ---
                itemsIndexed( // index を取得できるように変更
                    items = books,
                    key = { index, book -> book.id } // key の指定方法も変更
                ) { index, book -> // index と book を受け取る

                    // --- 区切り線を追加する条件判定 ---
                    val isFirstReadBook = index > 0 && // 最初のアイテムではない
                            book.status == BookStatus.READ && // 現在のアイテムが「読破」
                            books[index - 1].status != BookStatus.READ // 前のアイテムが「読破」ではない

                    if (isFirstReadBook) {
                        // 条件に合致したら区切り線を描画
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp), // 上下に余白
                            thickness = 1.dp, // 線の太さ
                            color = MaterialTheme.colorScheme.outlineVariant // 線の色
                        )
                    }
                    // ----------------------------------

                    // BookItem は変更なしでそのまま表示
                    BookItem(
                        book = book,
                        onDeleteClick = { viewModel.deleteBook(book) },
                        onItemClick = { editingBook = book }
                    )
                }
                // ----------------------------------
            }
        }
        // -----------------
    }
}

// --- BookItem と statusToString は変更なし ---
@Composable
fun BookItem(
    book: Book,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // --- 読了済みのスタイルを定義 ---
    val isRead = book.status == BookStatus.READ
    val textColor = if (isRead) Color.Gray else LocalContentColor.current // 読了ならグレー、それ以外はデフォルト
    val textDecoration = if (isRead) TextDecoration.LineThrough else null // 読了なら打ち消し線
    // ------------------------------

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRead) 1.dp else 2.dp), // 読了済みの Elevation を少し下げる (任意)
        onClick = onItemClick,
        // colors = CardDefaults.cardColors(...) // 必要なら背景色も変更
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- 書影表示 ---
            book.coverImage?.let { imageData ->
                val bitmap = remember(imageData) {
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Book Cover",
                        modifier = Modifier.size(60.dp, 80.dp),
                        contentScale = ContentScale.Crop,
                        alpha = if (isRead) 0.6f else 1.0f // 読了済みの画像を少し薄くする (任意)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
            // ---------------

            // --- 書籍情報 (スタイルを適用) ---
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor, // ← 色を適用
                    textDecoration = textDecoration // ← 打ち消し線を適用
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor, // ← 色を適用
                    textDecoration = textDecoration // ← 打ち消し線を適用
                )
                Text(
                    text = "ステータス: ${statusToString(book.status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor, // ← 色を適用
                    textDecoration = textDecoration // ← 打ち消し線を適用
                )
            }
            // ---------------------------------

            // --- 削除ボタン (変更なし) ---
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Book",
                    // tint = MaterialTheme.colorScheme.error // 元のコードの PrimaryColor でも良い
                    tint = if (isRead) Color.Gray else MaterialTheme.colorScheme.error // 読了ならボタンもグレーに (任意)
                )
            }
            // --------------------------
        }
    }
}

@Composable
fun statusToString(status: BookStatus): String {
    return when (status) {
        BookStatus.UNREAD -> "未読"
        BookStatus.READING -> "読書中"
        BookStatus.READ -> "読破"
    }
}