package com.takanakonbu.tsumidoku.ui.list

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterList
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
import com.takanakonbu.tsumidoku.ui.edit.EditBookDialog
import com.takanakonbu.tsumidoku.ui.theme.PrimaryColor
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookListScreen(
    viewModel: BookListViewModel = hiltViewModel()
) {
    val books by viewModel.books.collectAsStateWithLifecycle()
    val currentSortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val currentFilterStatus by viewModel.filterStatus.collectAsStateWithLifecycle()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf<Book?>(null) }
    var bookToDelete by remember { mutableStateOf<Book?>(null) }

    var showSortMenu by remember { mutableStateOf(false) }
    var showFilterMenu by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor,
                    titleContentColor = Color.White
                ),
                actions = {
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "フィルター",
                                tint = Color.White
                            )
                        }
                        FilterDropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false },
                            currentFilter = currentFilterStatus,
                            onFilterSelected = { status ->
                                viewModel.changeFilterStatus(status)
                                showFilterMenu = false
                            }
                        )
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "ソート",
                                tint = Color.White
                            )
                        }
                        SortDropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            currentSortOrder = currentSortOrder,
                            onSortOrderSelected = { sortOrder ->
                                viewModel.changeSortOrder(sortOrder)
                                showSortMenu = false
                                val sortText = sortOrderToString(sortOrder) // ★ Composable 外で呼び出しOK
                                scope.launch {
                                    snackbarHostState.showSnackbar("ソート順: $sortText")
                                }
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "書籍を追加")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        // --- ダイアログ表示 (変更なし) ---
        if (showAddDialog) {
            AddBookDialog(
                onDismissRequest = { showAddDialog = false },
                onAddClick = { title, author, memo, imageUri ->
                    viewModel.addBook(title, author, memo, imageUri)
                    showAddDialog = false
                }
            )
        }
        editingBook?.let { bookToEdit ->
            EditBookDialog(
                book = bookToEdit,
                onDismissRequest = { editingBook = null },
                onSaveClick = { title, author, memo, status, newImageUri ->
                    viewModel.updateBook(
                        id = bookToEdit.id,
                        title = title,
                        author = author,
                        memo = memo,
                        status = status,
                        newImageUri = newImageUri
                    )
                    editingBook = null
                }
            )
        }
        bookToDelete?.let { book ->
            DeleteConfirmationDialog(
                dialogTitle = "削除確認",
                dialogText = "『${book.title}』を削除しますか？",
                onConfirm = {
                    viewModel.deleteBook(book)
                },
                onDismiss = { bookToDelete = null }
            )
        }

        Column(modifier = Modifier.padding(innerPadding)) {
            if (books.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val message = if (currentFilterStatus != null) {
                        // ★ statusToString を直接呼び出す
                        "「${statusToString(currentFilterStatus!!)}」の書籍はありません。"
                    } else {
                        "まだ書籍が登録されていません。"
                    }
                    Text(message)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp)
                ) {
                    items(
                        items = books,
                        key = { book -> book.id }
                    ) { book ->
                        BookItem(
                            book = book,
                            onDeleteClick = { bookToDelete = book },
                            onItemClick = { editingBook = book }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentSortOrder: SortOrder,
    onSortOrderSelected: (SortOrder) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        SortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = sortOrderToString(sortOrder), // ★ Composable 内での呼び出しOK
                        fontWeight = if (sortOrder == currentSortOrder) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onSortOrderSelected(sortOrder) }
            )
        }
    }
}

@Composable
fun FilterDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    currentFilter: BookStatus?,
    onFilterSelected: (BookStatus?) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        DropdownMenuItem(
            text = {
                Text(
                    text = "すべて",
                    fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = { onFilterSelected(null) }
        )
        BookStatus.entries.forEach { status ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = statusToString(status), // ★ Composable 内での呼び出しOK
                        fontWeight = if (status == currentFilter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onFilterSelected(status) }
            )
        }
    }
}

// SortOrder を表示用文字列に変換するヘルパー関数
fun sortOrderToString(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.ADDED_DATE_DESC -> "追加日が新しい順"
        SortOrder.ADDED_DATE_ASC -> "追加日が古い順"
        SortOrder.TITLE_ASC -> "タイトル昇順 (あ→ん)"
        SortOrder.TITLE_DESC -> "タイトル降順 (ん→あ)"
        SortOrder.AUTHOR_ASC -> "著者名昇順 (あ→ん)"
        SortOrder.AUTHOR_DESC -> "著者名降順 (ん→あ)"
    }
}

@Composable
fun BookItem(
    book: Book,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRead = book.status == BookStatus.READ // [cite: 361]
    val isReading = book.status == BookStatus.READING // ★ 読書中かどうかの状態を追加
    val textColor = if (isRead) Color.Gray else LocalContentColor.current // [cite: 361]
    val textDecoration = if (isRead) TextDecoration.LineThrough else null // [cite: 362]

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp), // [cite: 363]
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRead) 1.dp else 2.dp), // [cite: 363]
        onClick = onItemClick, // [cite: 363]
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF), // [cite: 364]
            contentColor = LocalContentColor.current // [cite: 364]
        )
    ) {
        Row(
            modifier = Modifier.padding(8.dp), // [cite: 365]
            verticalAlignment = Alignment.CenterVertically // [cite: 365]
        ) {
            book.coverImage?.let { imageData -> // [cite: 366]
                val bitmap = remember(imageData) {
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                } // [cite: 366]
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(), // [cite: 367]
                        contentDescription = "Book Cover", // [cite: 367]
                        modifier = Modifier.size(60.dp, 80.dp), // [cite: 367]
                        contentScale = ContentScale.Crop, // [cite: 368]
                        alpha = if (isRead) 0.6f else 1.0f // [cite: 368]
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // [cite: 369]
                }
            }

            Column(modifier = Modifier.weight(1f)) { // [cite: 369]
                Text(
                    text = book.title, // [cite: 370]
                    style = MaterialTheme.typography.titleMedium, // [cite: 370]
                    fontWeight = FontWeight.Bold, // [cite: 370]
                    maxLines = 1, // [cite: 370]
                    overflow = TextOverflow.Ellipsis, // [cite: 371]
                    color = textColor, // [cite: 371]
                    textDecoration = textDecoration // [cite: 371]
                )
                Text(
                    text = book.author, // [cite: 372]
                    style = MaterialTheme.typography.bodyMedium, // [cite: 372]
                    maxLines = 1, // [cite: 372]
                    overflow = TextOverflow.Ellipsis, // [cite: 372]
                    color = textColor, // [cite: 373]
                    textDecoration = textDecoration // [cite: 373]
                )
                Text(
                    // ★★★ statusToString を直接呼び出す ★★★
                    text = "ステータス: ${statusToString(book.status)}", // [cite: 374]
                    style = MaterialTheme.typography.bodySmall, // [cite: 374]
                    color = textColor, // [cite: 374]
                    textDecoration = textDecoration // [cite: 375]
                )
            }

            // ★★★ ここから変更 ★★★
            // ステータスが「読書中」の場合、本のアイコンを表示
            if (isReading) {
                Icon(
                    imageVector = Icons.Filled.Book, // 本のアイコン
                    contentDescription = "読書中",
                    tint = PrimaryColor // 任意の色を指定
                )
                Spacer(modifier = Modifier.width(4.dp)) // アイコンと削除ボタンの間にスペースを追加
            }
            // ★★★ 変更ここまで ★★★

            IconButton(onClick = onDeleteClick) { // [cite: 376]
                Icon(
                    imageVector = Icons.Filled.Delete, // [cite: 377]
                    contentDescription = "Delete Book", // [cite: 377]
                    tint = if (isRead) Color.Gray else PrimaryColor // [cite: 377]
                )
            }
        }
    }
}

// ★★★ @Composable アノテーションを削除 ★★★
fun statusToString(status: BookStatus): String {
    return when (status) {
        BookStatus.UNREAD -> "未読"
        BookStatus.READING -> "読書中"
        BookStatus.READ -> "読破"
    }
}