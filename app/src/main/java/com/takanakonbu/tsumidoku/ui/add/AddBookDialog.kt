package com.takanakonbu.tsumidoku.ui.add // パッケージ名は適宜調整

import android.graphics.Bitmap // Bitmapプレビュー用
import android.graphics.ImageDecoder // Bitmapプレビュー用 (API 28+)
import android.net.Uri // URIを扱うため
import androidx.activity.compose.rememberLauncherForActivityResult // ランチャーのため
import androidx.activity.result.PickVisualMediaRequest // フォトピッカーリクエスト
import androidx.activity.result.contract.ActivityResultContracts // フォトピッカーコントラクト
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // 画像選択領域をクリック可能に
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext // Context取得のため
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog // ダイアログ表示のため

/**
 * 書籍追加用ダイアログの Composable
 *
 * @param onDismissRequest ダイアログを閉じるよう要求されたときの処理
 * @param onAddClick 「追加」ボタンがクリックされたときの処理 (入力されたタイトル, 著者名, メモ, 選択された画像のURIを渡す)
 */
@Composable
fun AddBookDialog(
    onDismissRequest: () -> Unit,
    onAddClick: (title: String, author: String, memo: String, imageUri: Uri?) -> Unit // imageUri を追加
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var isTitleError by remember { mutableStateOf(false) }
    var isAuthorError by remember { mutableStateOf(false) }

    // --- 画像選択関連 ---
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current

    // フォトピッカーを起動するためのランチャーを準備
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            selectedImageUri = uri
        }
    )
    // --------------------

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("書籍の追加", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                // タイトル入力欄
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

                // 著者名入力欄
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

                // メモ入力欄 (任意入力)
                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    label = { Text("メモ") },
                    modifier = Modifier.fillMaxWidth().height(100.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // --- 書影選択エリア ---
                Text("書影 (任意)", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally) // ★ 追加
                        .size(100.dp, 150.dp)
                        .border(1.dp, Color.Gray)
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri == null) {
                        Icon(
                            imageVector = Icons.Default.AccountBox,
                            contentDescription = "書影を選択",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray
                        )
                    } else {
                        val bitmap: Bitmap? = remember(selectedImageUri) {
                            try {
                                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, selectedImageUri!!))
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                        }
                        bitmap?.let {
                            Image(
                                bitmap = it.asImageBitmap(),
                                contentDescription = "選択された書影",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } ?: run {
                            Text("表示エラー", color = Color.Gray)
                        }
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
                                onAddClick(title, author, memo, selectedImageUri)
                            }
                        }
                    ) {
                        Text("追加")
                    }
                }
                // ----------------
            }
        }
    }
}