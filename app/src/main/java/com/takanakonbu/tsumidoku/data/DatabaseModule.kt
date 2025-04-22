package com.takanakonbu.tsumidoku.data

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt の DI モジュール
 * データベース関連 (AppDatabase, BookDao) のインスタンスを提供する方法を定義する
 */
@Module
@InstallIn(SingletonComponent::class) // アプリケーション全体で単一のインスタンスを提供
object DatabaseModule {

    /**
     * AppDatabase のシングルトンインスタンスを提供する
     * @param context アプリケーションコンテキスト (Hiltが自動で注入)
     * @return AppDatabase のインスタンス
     */
    @Singleton // アプリ内で常に同じインスタンスを返すようにする
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getDatabase(context)
    }

    /**
     * BookDao のインスタンスを提供する
     * AppDatabase に依存するため、引数で AppDatabase を受け取る (Hiltが自動で注入)
     * @param appDatabase AppDatabase のインスタンス
     * @return BookDao のインスタンス
     */
    @Provides
    fun provideBookDao(appDatabase: AppDatabase): BookDao {
        return appDatabase.bookDao()
    }
}