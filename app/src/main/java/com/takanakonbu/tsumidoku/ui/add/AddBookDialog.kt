package com.takanakonbu.tsumidoku.ui.add // パッケージ名は適宜調整

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog // ダイアログ表示のため

/**
 * 書籍追加用ダイアログの Composable
 *
 * @param onDismissRequest ダイアログを閉じるよう要求されたときの処理
 * @param onAddClick 「追加」ボタンがクリックされたときの処理 (入力されたタイトル, 著者名, メモを渡す)
 */
@OptIn(ExperimentalMaterial3Api::class) // TextField などに必要
@Composable
fun AddBookDialog(
    onDismissRequest: () -> Unit,
    // 追加ボタンクリック時に、入力された文字列をコールバックで渡す
    onAddClick: (title: String, author: String, memo: String) -> Unit
) {
    // ダイアログ内の入力状態を保持 (remember を使う)
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    // 入力検証のための状態 (任意)
    var isTitleError by remember { mutableStateOf(false) }
    var isAuthorError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(), // 高さはコンテンツに合わせる
            shape = MaterialTheme.shapes.medium // 角丸など
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("書籍の追加", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                // タイトル入力欄
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        isTitleError = it.isBlank() // 空ならエラー
                    },
                    label = { Text("タイトル *") },
                    isError = isTitleError, // エラー状態
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isTitleError) {
                    Text("タイトルは必須です", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 著者名入力欄
                OutlinedTextField(
                    value = author,
                    onValueChange = {
                        author = it
                        isAuthorError = it.isBlank() // 空ならエラー
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

                // メモ入力欄 (任意入力)
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ") },
                    modifier = Modifier.fillMaxWidth().height(100.dp) // 高さを指定
                )
                Spacer(modifier = Modifier.height(16.dp))

                // TODO: 書影選択機能 (後で実装)

                // ボタン (追加、キャンセル)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End // 右寄せ
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("キャンセル")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // 入力検証
                            isTitleError = title.isBlank()
                            isAuthorError = author.isBlank()
                            if (!isTitleError && !isAuthorError) {
                                onAddClick(title, author, memo) // ViewModel に通知
                            }
                        },
                        // タイトルと著者が入力されていない場合はボタンを無効化 (任意)
                        // enabled = title.isNotBlank() && author.isNotBlank()
                    ) {
                        Text("追加")
                    }
                }
            }
        }
    }
}