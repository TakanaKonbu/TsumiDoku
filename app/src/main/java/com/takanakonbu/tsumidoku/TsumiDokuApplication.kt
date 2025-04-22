package com.takanakonbu.tsumidoku

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * アプリケーションクラス
 * Hilt のエントリーポイントとして @HiltAndroidApp アノテーションを付与する
 */
@HiltAndroidApp
class TsumiDokuApplication : Application() {
    // 通常、ApplicationクラスのonCreate()などで初期化処理を行うが、
    // Hiltを使う場合は、基本的なセットアップはアノテーションが行うため、
    // このクラスには特別な記述は不要なことが多い。
}