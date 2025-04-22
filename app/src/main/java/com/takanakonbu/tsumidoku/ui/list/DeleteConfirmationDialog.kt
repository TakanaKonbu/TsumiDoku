package com.takanakonbu.tsumidoku.ui.list

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning // アイコンの例
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 削除確認用ダイアログ
 * @param dialogTitle ダイアログのタイトル
 * @param dialogText ダイアログの本文（削除対象の書籍名などを含む）
 * @param icon ダイアログに表示するアイコン（任意）
 * @param onConfirm 削除を確定したときの処理
 * @param onDismiss ダイアログを閉じたときの処理 (キャンセル含む)
 */
@Composable
fun DeleteConfirmationDialog(
    dialogTitle: String,
    dialogText: String,
    icon: ImageVector = Icons.Filled.Warning, // デフォルトは警告アイコン
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss, // ダイアログ外タップで閉じる
        icon = { Icon(icon, contentDescription = null) },
        title = { Text(text = dialogTitle) },
        text = { Text(text = dialogText) },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm() // 確定処理を実行
                    onDismiss() // ダイアログを閉じる
                }
            ) {
                Text("削除") // ボタンテキスト
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss // ダイアログを閉じるだけ
            ) {
                Text("キャンセル") // ボタンテキスト
            }
        }
    )
}