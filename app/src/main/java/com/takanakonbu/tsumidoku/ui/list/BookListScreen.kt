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
import androidx.compose.runtime.*
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
                items(
                    items = books,
                    key = { book -> book.id }
                ) { book ->
                    BookItem(
                        book = book,
                        onDeleteClick = { viewModel.deleteBook(book) },
                        // --- onItemClick で編集対象をセット ---
                        onItemClick = { editingBook = book }
                        // -----------------------------------
                    )
                }
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
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onItemClick // ここが呼ばれる
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            book.coverImage?.let { imageData ->
                val bitmap = remember(imageData) {
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                }
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Book Cover",
                        modifier = Modifier.size(60.dp, 80.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "ステータス: ${statusToString(book.status)}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Book",
                    tint = MaterialTheme.colorScheme.error
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