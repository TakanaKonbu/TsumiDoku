package com.takanakonbu.tsumidoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.takanakonbu.tsumidoku.ui.list.BookListScreen // 作成した画面をインポート
import com.takanakonbu.tsumidoku.ui.theme.TsumiDokuTheme
import dagger.hilt.android.AndroidEntryPoint // Hilt のエントリーポイントアノテーションをインポート

@AndroidEntryPoint // Hiltを使うActivityにはこのアノテーションが必要
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TsumiDokuTheme {
                // BookListScreen Composable を呼び出す
                BookListScreen()
            }
        }
    }
}

