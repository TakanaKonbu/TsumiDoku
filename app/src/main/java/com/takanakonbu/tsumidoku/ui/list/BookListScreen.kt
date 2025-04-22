package com.takanakonbu.tsumidoku.ui.list // パッケージ名は適宜調整

import android.graphics.BitmapFactory // 書影表示に必要
import androidx.compose.foundation.Image // 書影表示に必要
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card // リスト項目をカードにする場合
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel // hiltViewModel() のため
import androidx.lifecycle.compose.collectAsStateWithLifecycle // collectAsStateWithLifecycle のため
import com.takanakonbu.tsumidoku.R // R.string.app_name など
import com.takanakonbu.tsumidoku.data.Book
import com.takanakonbu.tsumidoku.data.BookStatus
import androidx.compose.runtime.remember
import com.takanakonbu.tsumidoku.ui.theme.PrimaryColor

@OptIn(ExperimentalMaterial3Api::class) // TopAppBar などに必要
@Composable
fun BookListScreen(
    // Navigationを追加する場合、ここに NavController やコールバック関数を追加
    viewModel: BookListViewModel = hiltViewModel() // Hilt から ViewModel を取得
) {
    // ViewModel から書籍リストの状態を収集
    val books by viewModel.books.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(id = R.string.app_name)) }, // アプリ名をタイトルに
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryColor, // 色を設定
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: 書籍追加ダイアログ表示処理 */ },
                containerColor = PrimaryColor, // 背景色をテーマから取得
                contentColor = contentColorFor(backgroundColor = MaterialTheme.colorScheme.secondary) // アイコンの色をテーマから取得
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Book")
            }
        }
    ) { innerPadding -> // Scaffold がコンテンツのために提供するパディング

        if (books.isEmpty()) {
            // データがない場合の表示 (任意)
            Column(
                modifier = Modifier
                    .padding(innerPadding) // Scaffold のパディングを適用
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("まだ書籍が登録されていません。")
            }
        } else {
            // 書籍リスト表示
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding) // Scaffold のパディングを適用
                    .fillMaxSize()
                    .padding(horizontal = 8.dp) // 左右に少しパディング
            ) {
                items(
                    items = books,
                    key = { book -> book.id } // 各アイテムに一意なキーを指定 (パフォーマンス向上)
                ) { book ->
                    BookItem(
                        book = book,
                        onDeleteClick = { viewModel.deleteBook(book) }, // 削除ボタンクリック時の処理
                        onItemClick = { /* TODO: 書籍編集ダイアログ表示処理 */ }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class) // Card のため
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

// BookStatus を表示用文字列に変換するヘルパー関数 (ViewModel 内や別ファイルでも良い)
@Composable
fun statusToString(status: BookStatus): String {
    return when (status) {
        BookStatus.UNREAD -> "未読"
        BookStatus.READING -> "読書中"
        BookStatus.READ -> "読了"
    }
}
