package com.takanakonbu.tsumidoku.data

import androidx.room.TypeConverter

/**
 * Roomが直接扱えないデータ型を変換するためのコンバータークラス
 */
class Converters {
    /**
     * BookStatus Enum を String に変換してDBに保存
     */
    @TypeConverter
    fun fromBookStatus(status: BookStatus): String {
        return status.name // Enumの名前 (UNREAD, READING, READ) を文字列として保存
    }

    /**
     * DBから読み込んだ String を BookStatus Enum に変換
     */
    @TypeConverter
    fun toBookStatus(statusString: String): BookStatus {
        return BookStatus.valueOf(statusString) // 文字列からEnumに復元
    }

    // 必要に応じて他の型コンバータ（例: Date <-> Long）もここに追加できますが、
    // 今回は日時をLongで直接保存するため不要です。
    // 書影もByteArrayで直接保存するため不要です。
}