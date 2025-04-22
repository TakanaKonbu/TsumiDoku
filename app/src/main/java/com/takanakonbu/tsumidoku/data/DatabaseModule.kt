package com.takanakonbu.tsumidoku.data

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt の DI モジュール
 * データベース関連とリポジトリのインスタンスを提供する方法を定義する
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DatabaseModule {

    // BookRepositoryImpl は @Inject constructor を持つため Hilt は生成方法を知っている
    // この @Binds により、BookRepository (インターフェース) が要求されたときに
    // Hilt が BookRepositoryImpl のインスタンスを注入するようになる
    @Singleton
    @Binds
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    // @Provides を含むメソッドは static であるか、
    // 非抽象モジュール (object) に属する必要があるため companion object を使用
    companion object {
        @Singleton
        @Provides
        fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
            return AppDatabase.getDatabase(context)
        }

        // AppDatabase に依存
        @Provides
        fun provideBookDao(appDatabase: AppDatabase): BookDao {
            return appDatabase.bookDao()
        }
    }
}