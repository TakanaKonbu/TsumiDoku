package com.takanakonbu.tsumidoku.ui.list

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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
import androidx.compose.material.icons.filled.Videocam // ★ アイコン追加
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
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

    // --- ★ 広告確認ダイアログ用の State ★ ---
    var showConfirmAdDialog by remember { mutableStateOf(false) }
    // --------------------------------------

    // --- リワード広告関連の State ---
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
    var isLoadingAd by remember { mutableStateOf(false) }
    // ------------------------------------

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // --- Activityを取得するヘルパー関数 ---
    fun Context.getActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }
    val activity = context.getActivity()

    // --- リワード広告を読み込む関数 ---
    fun loadRewardedAd() {
        if (rewardedAd == null && !isLoadingAd && activity != null) {
            isLoadingAd = true
            val adRequest = AdRequest.Builder().build()
            val adUnitId = "ca-app-pub-3940256099942544/5224354917" // <- テスト用ID
            // val adUnitId = "ca-app-pub-2836653067032260/YOUR_REWARDED_AD_UNIT_ID" // <- 本番用ID

            RewardedAd.load(activity, adUnitId, adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        isLoadingAd = false
                        rewardedAd = null
                        scope.launch {
                            snackbarHostState.showSnackbar("広告の読み込みに失敗: ${adError.message}")
                        }
                        println("Ad failed to load: ${adError.message}")
                    }

                    override fun onAdLoaded(ad: RewardedAd) {
                        isLoadingAd = false
                        rewardedAd = ad
                        println("Ad loaded successfully")

                        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                rewardedAd = null
                                loadRewardedAd()
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                rewardedAd = null
                                scope.launch {
                                    snackbarHostState.showSnackbar("広告の表示に失敗しました")
                                }
                            }

                            override fun onAdShowedFullScreenContent() {
                                println("Ad showed fullscreen content.")
                            }
                        }
                    }
                })
        }
    }

    // --- ★ リワード広告を表示する関数 ★ ---
    fun showRewardAd() {
        if (activity != null && rewardedAd != null) {
            rewardedAd?.show(activity, OnUserEarnedRewardListener { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                println("User earned the reward: $rewardAmount $rewardType")
                // リワード獲得 -> ダイアログ表示
                showAddDialog = true
                // 使用済み広告を破棄し、次を読み込む
                rewardedAd = null
                loadRewardedAd()
            })
        } else if (isLoadingAd) {
            scope.launch {
                snackbarHostState.showSnackbar("広告を準備中です…")
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("広告の準備ができていません。再試行します。")
            }
            loadRewardedAd() // 再度読み込み試行
        }
    }
    // --------------------------------------

    // --- 画面表示時に広告を事前読み込み ---
    LaunchedEffect(Unit) {
        loadRewardedAd()
    }
    // --------------------------------------

    Scaffold(
        topBar = { /* (変更なし) */
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
                                val sortText = sortOrderToString(sortOrder)
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
                onClick = {
                    // --- ★ FABクリック時のロジック変更 ★ ---
                    val currentBookCount = books.size

                    if (currentBookCount < 3) {
                        // 3冊未満なら直接AddBookDialog表示
                        showAddDialog = true
                    } else {
                        // 3冊以上なら広告視聴確認ダイアログ表示
                        showConfirmAdDialog = true
                    }
                    // ----------------------------------------
                },
                containerColor = PrimaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Filled.Add, contentDescription = "書籍を追加")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->

        // --- ダイアログ表示 ---
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
            EditBookDialog(/* (変更なし) */
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
            DeleteConfirmationDialog(/* (変更なし) */
                dialogTitle = "削除確認",
                dialogText = "『${book.title}』を削除しますか？",
                onConfirm = {
                    viewModel.deleteBook(book)
                },
                onDismiss = { bookToDelete = null }
            )
        }

        // --- ★ 広告視聴確認ダイアログの表示 ★ ---
        if (showConfirmAdDialog) {
            ConfirmRewardAdDialog(
                onConfirm = {
                    // 「はい」が押されたら確認ダイアログを閉じ、広告表示処理へ
                    showConfirmAdDialog = false
                    showRewardAd() // 広告表示関数を呼び出す
                },
                onDismiss = {
                    // 「キャンセル」が押されたらダイアログを閉じるだけ
                    showConfirmAdDialog = false
                }
            )
        }
        // --------------------------------------

        // --- リスト表示 ---
        Column(modifier = Modifier.padding(innerPadding)) {
            /* (変更なし) */
            if (books.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val message = if (currentFilterStatus != null) {
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

// --- ★ 広告視聴確認ダイアログのComposable ★ ---
@Composable
fun ConfirmRewardAdDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.Videocam, contentDescription = null) }, // 動画アイコン
        title = { Text(text = "確認") },
        text = { Text(text = "登録可能上限(3冊)を超えています。\n短い動画を見て追加登録しますか？") },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("はい")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("キャンセル")
            }
        }
    )
}
// --------------------------------------------

// --- 以下、変更なし ---

@Composable
fun SortDropdownMenu(/* (変更なし) */
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
                        text = sortOrderToString(sortOrder),
                        fontWeight = if (sortOrder == currentSortOrder) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onSortOrderSelected(sortOrder) }
            )
        }
    }
}

@Composable
fun FilterDropdownMenu(/* (変更なし) */
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
                        text = statusToString(status),
                        fontWeight = if (status == currentFilter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onFilterSelected(status) }
            )
        }
    }
}

fun sortOrderToString(sortOrder: SortOrder): String {/* (変更なし) */
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
fun BookItem(/* (変更なし) */
             book: Book,
             onDeleteClick: () -> Unit,
             onItemClick: () -> Unit,
             modifier: Modifier = Modifier
) {
    val isRead = book.status == BookStatus.READ
    val isReading = book.status == BookStatus.READING
    val textColor = if (isRead) Color.Gray else LocalContentColor.current
    val textDecoration = if (isRead) TextDecoration.LineThrough else null

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRead) 1.dp else 2.dp),
        onClick = onItemClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFFFFF),
            contentColor = LocalContentColor.current
        )
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
                        contentScale = ContentScale.Crop,
                        alpha = if (isRead) 0.6f else 1.0f
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
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    textDecoration = textDecoration
                )
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    textDecoration = textDecoration
                )
                Text(
                    text = "ステータス: ${statusToString(book.status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    textDecoration = textDecoration
                )
            }

            if (isReading) {
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = "読書中",
                    tint = PrimaryColor
                )
                Spacer(modifier = Modifier.width(4.dp))
            }

            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Book",
                    tint = if (isRead) Color.Gray else PrimaryColor
                )
            }
        }
    }
}

fun statusToString(status: BookStatus): String {/* (変更なし) */
    return when (status) {
        BookStatus.UNREAD -> "未読"
        BookStatus.READING -> "読書中"
        BookStatus.READ -> "読破"
    }
}

// DeleteConfirmationDialog は別ファイルにある想定