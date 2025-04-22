package com.takanakonbu.tsumidoku.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/**
 * アプリケーションの Room データベースクラス
 */
@Database(
    entities = [Book::class], // このデータベースに含まれるエンティティのリスト
    version = 1,              // データベースのバージョン (スキーマ変更時に上げる)
    exportSchema = false      // スキーマ情報をファイルに出力しない (通常は false でOK)
)
@TypeConverters(Converters::class) // データベース全体で使用する型コンバータ
abstract class AppDatabase : RoomDatabase() {

    /**
     * BookDao のインスタンスを提供する抽象メソッド
     * Room ライブラリがこの実装を自動生成する
     */
    abstract fun bookDao(): BookDao

    // --- Singleton パターン ---
    // アプリ全体でデータベースインスタンスを一つだけ生成するための仕組み
    companion object {
        // @Volatile: 他のスレッドからの変更が即座に見えるようにする
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            // INSTANCE が null なら同期を取りながらデータベースを生成
            // そうでなければ既存の INSTANCE を返す
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext, // アプリケーションコンテキスト
                    AppDatabase::class.java,    // このデータベースクラス
                    "tsumidoku_database"        // データベースファイル名
                )
                    // .fallbackToDestructiveMigration() // スキーマ変更時にデータを破棄して再生成する場合 (開発初期に便利)
                    // .addMigrations(...) // マイグレーションを定義する場合
                    .build()
                INSTANCE = instance
                instance // 生成したインスタンスを返す
            }
        }
    }
    // --- Singleton パターン ここまで ---
}