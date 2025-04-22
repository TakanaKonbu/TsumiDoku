package com.takanakonbu.tsumidoku.ui.list

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.* // remember, mutableStateOf, getValue, setValue をインポート
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.takanakonbu.tsumidoku.R
import com.takanakonbu.tsumidoku.data.Book
import com.takanakonbu.tsumidoku.data.BookStatus
// remember は既にインポートされている
import com.takanakonbu.tsumidoku.ui.add.AddBookDialog // 作成したダイアログをインポート
import com.takanakonbu.tsumidoku.ui.theme.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    viewModel: BookListViewModel = hiltViewModel()
) {
    val books by viewModel.books.collectAsStateWithLifecycle()

    // --- ダイアログ表示状態管理 ---
    var showAddDialog by remember { mutableStateOf(false) }
    // --------------------------

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer // onPrimary の方が適切かも？
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                // --- FABクリックでダイアログ表示 ---
                onClick = { showAddDialog = true },
                // ------------------------------
                containerColor = PrimaryColor,
                contentColor = Color.White // FABの背景色に合わせたコンテンツ色
            ) {
                Icon(
                    Icons.Filled.Add, contentDescription = "書籍を追加"
                )
            }
        }
    ) { innerPadding ->

        // --- ダイアログの表示制御 ---
        if (showAddDialog) {
            AddBookDialog(
                onDismissRequest = { showAddDialog = false }, // ダイアログ外タップなどで閉じる
                onAddClick = { title, author, memo ->
                    viewModel.addBook(title, author, memo) // ViewModelに関数を呼び出す
                    showAddDialog = false // ダイアログを閉じる
                }
            )
        }
        // --------------------------

        // --- リスト表示 (変更なし) ---
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
                items(
                    items = books,
                    key = { book -> book.id }
                ) { book ->
                    BookItem(
                        book = book,
                        onDeleteClick = { viewModel.deleteBook(book) },
                        onItemClick = { /* TODO: 書籍編集ダイアログ表示処理 */ }
                    )
                }
            }
        }
        // --------------------------
    }
}

// BookItem と statusToString は変更なし

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookItem(
    book: Book,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onItemClick // カード全体をクリック可能に
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 書影表示 (もしあれば)
            book.coverImage?.let { imageData ->
                val bitmap = remember(imageData) { // imageData が変わった時だけ再計算
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Book Cover",
                        modifier = Modifier.size(60.dp, 80.dp), // サイズ指定
                        contentScale = ContentScale.Crop // 表示方法
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            // 書籍情報 (タイトル、著者、ステータス)
            Column(modifier = Modifier.weight(1f)) { // 残りのスペースを埋める
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1, // 1行に制限
                    overflow = TextOverflow.Ellipsis // はみ出たら ... で表示
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ステータス: ${statusToString(book.status)}", // ステータス表示
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // 削除ボタン
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Book",
                    tint = MaterialTheme.colorScheme.error // エラー色などを使う
                )
            }
        }
    }
}

@Composable
fun statusToString(status: BookStatus): String {
    return when (status) {
        BookStatus.UNREAD -> "未読"
        BookStatus.READING -> "読書中"
        BookStatus.READ -> "読了"
    }
}