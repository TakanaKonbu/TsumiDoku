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
import androidx.compose.material.icons.filled.Videocam // ★ 変更なし
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
// ★ インタースティシャル広告用のインポートを追加 ★
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
    // --- State定義 (ViewModelから) ---
    val books by viewModel.books.collectAsStateWithLifecycle() // 書籍リスト
    val currentSortOrder by viewModel.sortOrder.collectAsStateWithLifecycle() // 現在のソート順
    val currentFilterStatus by viewModel.filterStatus.collectAsStateWithLifecycle() // 現在のフィルターステータス

    // --- ダイアログ表示状態 ---
    var showAddDialog by remember { mutableStateOf(false) } // 書籍追加ダイアログ
    var editingBook by remember { mutableStateOf<Book?>(null) } // 編集対象の書籍 (nullなら非表示)
    var bookToDelete by remember { mutableStateOf<Book?>(null) } // 削除対象の書籍 (nullなら非表示)

    // --- ドロップダウンメニュー表示状態 ---
    var showSortMenu by remember { mutableStateOf(false) } // ソートメニュー
    var showFilterMenu by remember { mutableStateOf(false) } // フィルターメニュー

    // --- リワード広告関連 ---
    var showConfirmAdDialog by remember { mutableStateOf(false) } // リワード広告表示確認ダイアログ
    var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) } // 読み込んだリワード広告インスタンス
    var isLoadingRewardedAd by remember { mutableStateOf(false) } // リワード広告読み込み中フラグ

    // --- ★ インタースティシャル広告関連の State ★ ---
    var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) } // 読み込んだインタースティシャル広告インスタンス
    var isLoadingInterstitialAd by remember { mutableStateOf(false) } // インタースティシャル広告読み込み中フラグ
    // ---------------------------------------------

    // --- その他 ---
    val snackbarHostState = remember { SnackbarHostState() } // Snackbar表示用
    val scope = rememberCoroutineScope() // Coroutineスコープ
    val context = LocalContext.current // 現在のContext

    // --- Activityを取得するヘルパー関数 ---
    fun Context.getActivity(): Activity? = when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.getActivity()
        else -> null
    }
    val activity = context.getActivity() // 現在のActivity (広告表示に必要)

    // --- リワード広告を読み込む関数 ---
    fun loadRewardedAd() {
        // すでに読み込み済み、読み込み中、Activityがない場合は何もしない
        if (rewardedAd != null || isLoadingRewardedAd || activity == null) return

        isLoadingRewardedAd = true // 読み込み開始
        val adRequest = AdRequest.Builder().build()
        // 広告ユニットID (テスト用と本番用を使い分ける)
        val adUnitId = "ca-app-pub-3940256099942544/5224354917" // テスト用ID
        // val adUnitId = "ca-app-pub-2836653067032260/4397862232" // TODO: 本番用IDに置き換え

        println("Attempting to load Rewarded Ad...") // ログ追加

        RewardedAd.load(activity, adUnitId, adRequest,
            object : RewardedAdLoadCallback() {
                // 読み込み失敗時の処理
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingRewardedAd = false // 読み込み終了
                    rewardedAd = null // 広告インスタンスをnullに
                    println("Rewarded Ad failed to load: ${adError.message}")
                    // scope.launch { snackbarHostState.showSnackbar("リワード広告の読み込み失敗: ${adError.message}") }
                }

                // 読み込み成功時の処理
                override fun onAdLoaded(ad: RewardedAd) {
                    isLoadingRewardedAd = false // 読み込み終了
                    rewardedAd = ad // 広告インスタンスを保持
                    println("Rewarded Ad loaded successfully")

                    // フルスクリーンコンテンツのコールバックを設定
                    rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        // 広告が閉じられたときの処理
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null // 参照を破棄
                            loadRewardedAd() // 次の広告を読み込む
                            println("Rewarded Ad dismissed.")
                        }
                        // 広告表示失敗時の処理
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            rewardedAd = null // 参照を破棄
                            println("Rewarded Ad failed to show: ${adError.message}")
                            // scope.launch { snackbarHostState.showSnackbar("リワード広告の表示に失敗") }
                            loadRewardedAd() // 次の広告を試す
                        }
                        // 広告が表示されたときの処理
                        override fun onAdShowedFullScreenContent() {
                            println("Rewarded Ad showed fullscreen content.")
                        }
                    }
                }
            })
    }

    // --- リワード広告を表示する関数 ---
    fun showRewardAd() {
        if (activity == null) {
            println("Cannot show Rewarded Ad: Activity is null")
            return
        }
        if (rewardedAd != null) {
            // 広告を表示し、ユーザーがリワードを獲得したときのリスナーを設定
            rewardedAd?.show(activity, OnUserEarnedRewardListener { rewardItem ->
                val rewardAmount = rewardItem.amount
                val rewardType = rewardItem.type
                println("User earned the reward: $rewardAmount $rewardType")
                // リワード獲得 -> 書籍追加ダイアログ表示
                showAddDialog = true
                // 使用済みの広告は破棄
                rewardedAd = null
                loadRewardedAd() // 次の広告を読み込む
            })
        } else if (isLoadingRewardedAd) {
            scope.launch { snackbarHostState.showSnackbar("広告を準備中です…") }
        } else {
            scope.launch { snackbarHostState.showSnackbar("広告の準備ができていません。再試行します。") }
            loadRewardedAd() // 読み込みを再試行
        }
    }

    // --- ★ インタースティシャル広告を読み込む関数 ★ ---
    fun loadInterstitialAd() {
        // すでに読み込み済み、読み込み中、Activityがない場合は何もしない
        if (interstitialAd != null || isLoadingInterstitialAd || activity == null) return

        isLoadingInterstitialAd = true // 読み込み開始
        val adRequest = AdRequest.Builder().build()
        // 広告ユニットID (ユーザー提供の本番ID)
        val adUnitId = "ca-app-pub-2836653067032260/9765723076"
//         val adUnitId = "ca-app-pub-3940256099942544/1033173712" // テスト用ID

        println("Attempting to load Interstitial Ad...") // ログ追加

        InterstitialAd.load(activity, adUnitId, adRequest,
            object : InterstitialAdLoadCallback() {
                // 読み込み失敗時の処理
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    isLoadingInterstitialAd = false // 読み込み終了
                    interstitialAd = null // 広告インスタンスをnullに
                    println("Interstitial Ad failed to load: ${adError.message}")
                    // scope.launch { snackbarHostState.showSnackbar("広告の読み込み失敗: ${adError.message}") }
                }

                // 読み込み成功時の処理
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoadingInterstitialAd = false // 読み込み終了
                    interstitialAd = ad // 広告インスタンスを保持
                    println("Interstitial Ad loaded successfully")

                    // フルスクリーンコンテンツのコールバックを設定
                    interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                        // 広告が閉じられたときの処理
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null // 参照を破棄
                            loadInterstitialAd() // 次の広告を読み込む
                            println("Interstitial Ad dismissed.")
                        }
                        // 広告表示失敗時の処理
                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            interstitialAd = null // 参照を破棄
                            println("Interstitial Ad failed to show: ${adError.message}")
                            // scope.launch { snackbarHostState.showSnackbar("広告の表示に失敗") }
                            loadInterstitialAd() // 次の広告を試す
                        }
                        // 広告が表示されたときの処理
                        override fun onAdShowedFullScreenContent() {
                            println("Interstitial Ad showed fullscreen content.")
                            // インタースティシャル広告は表示されたら参照をnullにするのが一般的
                            // interstitialAd = null // ここでnullにするか、Dismissedでするかは設計次第
                        }
                    }
                }
            })
    }
    // -------------------------------------------------

    // --- ★ インタースティシャル広告を表示する関数 ★ ---
    fun showInterstitialAd() {
        if (activity == null) {
            println("Cannot show Interstitial Ad: Activity is null")
            return
        }
        if (interstitialAd != null) {
            println("Showing Interstitial Ad...")
            interstitialAd?.show(activity)
        } else if (isLoadingInterstitialAd) {
            println("Interstitial Ad is still loading...")
            scope.launch { snackbarHostState.showSnackbar("広告準備中...") }
        } else {
            println("Interstitial Ad is not ready. Trying to load again.")
            scope.launch { snackbarHostState.showSnackbar("広告準備ができていません") }
            loadInterstitialAd() // 準備できていなければ読み込み試行
        }
    }
    // -------------------------------------------------

    // --- 画面表示/再コンポーズ時に広告を事前読み込み ---
    LaunchedEffect(Unit) {
        println("LaunchedEffect: Loading Ads")
        loadRewardedAd()
        loadInterstitialAd()
    }
    // --------------------------------------

    // --- UI描画 ---
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor, // AppBarの背景色
                    titleContentColor = Color.White // AppBarのタイトル色
                ),
                actions = { // AppBar右側のアイコンボタン
                    // フィルターボタンとドロップダウン
                    Box {
                        IconButton(onClick = { showFilterMenu = true }) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "フィルター",
                                tint = Color.White // アイコンの色
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
                    // ソートボタンとドロップダウン
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.Sort, // AutoMirroredでRTL対応
                                contentDescription = "ソート",
                                tint = Color.White // アイコンの色
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
        floatingActionButton = { // フローティングアクションボタン (書籍追加)
            FloatingActionButton(
                onClick = {
                    // 現在の書籍数をチェック
                    val currentBookCount = books.size
                    if (currentBookCount < 3) {
                        // 3冊未満なら直接AddBookDialog表示
                        showAddDialog = true
                    } else {
                        // 3冊以上ならリワード広告視聴確認ダイアログ表示
                        showConfirmAdDialog = true
                    }
                },
                containerColor = PrimaryColor, // FABの背景色
                contentColor = Color.White // FABのアイコン色
            ) {
                Icon(Icons.Filled.Add, contentDescription = "書籍を追加")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) } // Snackbar表示領域
    ) { innerPadding -> // Scaffold内のコンテンツ領域 (AppBarとFAB以外の部分)

        // --- ダイアログ表示ロジック ---
        // 書籍追加ダイアログ
        if (showAddDialog) {
            AddBookDialog(
                onDismissRequest = { showAddDialog = false }, // ダイアログ外タップなどで閉じる要求
                onAddClick = { title, author, memo, imageUri -> // 「追加」ボタンクリック時
                    println("AddBookDialog: Add clicked")
                    viewModel.addBook(title, author, memo, imageUri) // ViewModelに書籍追加を依頼
                    showAddDialog = false // ダイアログを閉じる
                    // ★ 書籍追加処理後にインタースティシャル広告を表示 ★
                    println("Triggering Interstitial Ad after adding book.")
                    showInterstitialAd()
                }
            )
        }
        // 書籍編集ダイアログ (editingBookがnullでない場合に表示)
        editingBook?.let { bookToEdit ->
            EditBookDialog(
                book = bookToEdit, // 編集対象の書籍データを渡す
                onDismissRequest = { editingBook = null }, // ダイアログを閉じる要求
                onSaveClick = { title, author, memo, status, newImageUri -> // 「保存」ボタンクリック時
                    viewModel.updateBook( // ViewModelに書籍更新を依頼
                        id = bookToEdit.id,
                        title = title,
                        author = author,
                        memo = memo,
                        status = status,
                        newImageUri = newImageUri
                    )
                    editingBook = null // ダイアログを閉じる
                }
            )
        }
        // 削除確認ダイアログ (bookToDeleteがnullでない場合に表示)
        bookToDelete?.let { book ->
            DeleteConfirmationDialog(
                dialogTitle = "削除確認",
                dialogText = "『${book.title}』を削除しますか？",
                onConfirm = { // 「削除」ボタンクリック時
                    viewModel.deleteBook(book) // ViewModelに書籍削除を依頼
                    // bookToDelete = null // onDismissで閉じるのでここでは不要
                },
                onDismiss = { bookToDelete = null } // 「キャンセル」またはダイアログ外タップ
            )
        }
        // リワード広告確認ダイアログ (showConfirmAdDialogがtrueの場合に表示)
        if (showConfirmAdDialog) {
            ConfirmRewardAdDialog(
                onConfirm = { // 「はい」ボタンクリック時
                    showConfirmAdDialog = false // ダイアログを閉じる
                    showRewardAd() // リワード広告表示処理を呼び出す
                },
                onDismiss = { // 「キャンセル」またはダイアログ外タップ
                    showConfirmAdDialog = false // ダイアログを閉じるだけ
                }
            )
        }
        // ---------------------

        // --- 書籍リスト表示 ---
        Column(modifier = Modifier.padding(innerPadding)) {
            if (books.isEmpty()) { // リストが空の場合
                Column(
                    modifier = Modifier
                        .fillMaxSize(), // 画面全体に広がる
                    horizontalAlignment = Alignment.CenterHorizontally, // 水平中央揃え
                    verticalArrangement = Arrangement.Center // 垂直中央揃え
                ) {
                    // フィルターがかかっているかでメッセージを出し分け
                    val message = if (currentFilterStatus != null) {
                        "「${statusToString(currentFilterStatus!!)}」の書籍はありません。"
                    } else {
                        "まだ書籍が登録されていません。"
                    }
                    Text(message)
                }
            } else { // リストに書籍がある場合
                LazyColumn( // スクロール可能なリスト
                    modifier = Modifier
                        .fillMaxSize() // 画面全体に広がる
                        .padding(horizontal = 8.dp) // 左右にパディング
                ) {
                    // booksリストの各要素に対してBookItemを表示
                    items(
                        items = books,
                        key = { book -> book.id } // 各アイテムにユニークなキーを指定 (パフォーマンス向上)
                    ) { book ->
                        BookItem( // 各書籍アイテムのComposable
                            book = book,
                            onDeleteClick = { bookToDelete = book }, // 削除アイコンクリック時の処理
                            onItemClick = { editingBook = book } // アイテム全体クリック時の処理
                        )
                    }
                }
            }
        }
    }
}

// --- ★ リワード広告視聴確認ダイアログ Composable ★ ---
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

// --- ソート順選択ドロップダウンメニュー ---
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
        // SortOrderの各要素に対してメニューアイテムを作成
        SortOrder.entries.forEach { sortOrder ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = sortOrderToString(sortOrder), // 表示文字列
                        // 現在選択されている項目を太字にする
                        fontWeight = if (sortOrder == currentSortOrder) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onSortOrderSelected(sortOrder) } // クリック時に選択されたソート順を通知
            )
        }
    }
}

// --- フィルターステータス選択ドロップダウンメニュー ---
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
        // 「すべて」のメニューアイテム
        DropdownMenuItem(
            text = {
                Text(
                    text = "すべて",
                    fontWeight = if (currentFilter == null) FontWeight.Bold else FontWeight.Normal
                )
            },
            onClick = { onFilterSelected(null) } // nullを選択 (フィルター解除)
        )
        // BookStatusの各要素に対してメニューアイテムを作成
        BookStatus.entries.forEach { status ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = statusToString(status), // 表示文字列
                        fontWeight = if (status == currentFilter) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onFilterSelected(status) } // クリック時に選択されたステータスを通知
            )
        }
    }
}

// --- SortOrder Enum を表示用文字列に変換するヘルパー関数 ---
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

// --- 書籍リストの各アイテムを表示する Composable ---
@OptIn(ExperimentalMaterial3Api::class) // CardのonClickのため
@Composable
fun BookItem(
    book: Book,
    onDeleteClick: () -> Unit,
    onItemClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // ステータスに応じた表示設定
    val isRead = book.status == BookStatus.READ // 読了済みか
    val isReading = book.status == BookStatus.READING // 読書中か
    // 読了済みなら文字色をグレー、そうでなければデフォルト
    val textColor = if (isRead) Color.Gray else LocalContentColor.current
    // 読了済みなら取り消し線
    val textDecoration = if (isRead) TextDecoration.LineThrough else null

    Card(
        modifier = modifier
            .fillMaxWidth() // 横幅いっぱい
            .padding(vertical = 4.dp), // 上下にパディング
        // 読了済みなら影を薄くする
        elevation = CardDefaults.cardElevation(defaultElevation = if (isRead) 1.dp else 2.dp),
        onClick = onItemClick, // カード全体をクリック可能に
        colors = CardDefaults.cardColors(
            containerColor = Color.White, // カードの背景色を白に固定
            contentColor = LocalContentColor.current // 内容物の色はテーマに従う
        )
    ) {
        Row( // 横並びレイアウト
            modifier = Modifier.padding(8.dp), // カード内部のパディング
            verticalAlignment = Alignment.CenterVertically // 要素を垂直方向に中央揃え
        ) {
            // 書影表示 (coverImageがnullでない場合)
            book.coverImage?.let { imageData ->
                // ByteArrayからBitmapを生成 (rememberで再生成を抑制)
                val bitmap = remember(imageData) {
                    BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
                }
                bitmap?.let { // Bitmap生成成功時
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Book Cover",
                        modifier = Modifier.size(60.dp, 80.dp), // 画像サイズ指定
                        contentScale = ContentScale.Crop, // 画像表示方法 (切り抜き)
                        alpha = if (isRead) 0.6f else 1.0f // 読了済みなら少し透過
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // 画像とテキストの間にスペース
                }
            }

            // 書籍情報 (タイトル、著者名、ステータス)
            Column(modifier = Modifier.weight(1f)) { // 残りのスペースを埋める
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.titleMedium, // テキストスタイル
                    fontWeight = FontWeight.Bold, // 太字
                    maxLines = 1, // 1行表示
                    overflow = TextOverflow.Ellipsis, // はみ出した部分は...で表示
                    color = textColor, // 読了状態に応じた色
                    textDecoration = textDecoration // 読了状態に応じた装飾
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

            // 読書中アイコン表示 (isReadingがtrueの場合)
            if (isReading) {
                Icon(
                    imageVector = Icons.Filled.Book,
                    contentDescription = "読書中",
                    tint = PrimaryColor // アイコンの色
                )
                Spacer(modifier = Modifier.width(4.dp)) // アイコンと削除ボタンの間にスペース
            }

            // 削除ボタン
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Book",
                    // 読了済みならアイコンをグレーにする
                    tint = if (isRead) Color.Gray else PrimaryColor
                )
            }
        }
    }
}

// --- BookStatus Enum を表示用文字列に変換するヘルパー関数 ---
fun statusToString(status: BookStatus): String {
    return when (status) {
        BookStatus.UNREAD -> "未読"
        BookStatus.READING -> "読書中"
        BookStatus.READ -> "読破"
    }
}

// DeleteConfirmationDialog は別ファイルにある想定
// (com.takanakonbu.tsumidoku.ui.list.DeleteConfirmationDialog.kt)