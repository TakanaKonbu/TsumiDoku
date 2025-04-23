package com.takanakonbu.tsumidoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.takanakonbu.tsumidoku.ui.list.BookListScreen // 作成した画面をインポート
import com.takanakonbu.tsumidoku.ui.theme.TsumiDokuTheme
import dagger.hilt.android.AndroidEntryPoint // Hilt のエントリーポイントアノテーションをインポート
import com.google.android.gms.ads.MobileAds // ★ MobileAds SDKをインポート ★

@AndroidEntryPoint // Hiltを使うActivityにはこのアノテーションが必要
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ Mobile Ads SDK を初期化 ★
        // アプリの起動時に一度だけ呼び出す必要があります。
        // 初期化完了時に何か処理を行いたい場合は、{} の中にリスナーを実装します。
        MobileAds.initialize(this) {}

        enableEdgeToEdge() // Edge-to-edge表示を有効化
        setContent {
            TsumiDokuTheme { // アプリのテーマを適用
                // BookListScreen Composable を呼び出して画面を表示
                BookListScreen()
            }
        }
    }
}