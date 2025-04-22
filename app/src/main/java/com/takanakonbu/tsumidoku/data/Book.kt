package com.takanakonbu.tsumidoku.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import java.util.UUID

/**
 * 書籍データを表すエンティティクラス (データベースのテーブルに対応)
 */
@Entity(tableName = "books") // テーブル名を "books" に指定
@TypeConverters(Converters::class) // 上記で定義した型コンバータを使用
data class Book(
    /** 主キー: 一意な識別子 (UUID文字列) */
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    /** 書籍タイトル (必須) */
    @ColumnInfo(name = "title") // カラム名を指定 (フィールド名と同じなら省略可)
    val title: String,

    /** 著者名 (必須) */
    @ColumnInfo(name = "author")
    val author: String,

    /** 書影 (表紙画像データ、任意) */
    @ColumnInfo(name = "cover_image", typeAffinity = ColumnInfo.BLOB) // BLOB型を指定
    val coverImage: ByteArray? = null, // 画像データを直接格納 (Nullable)

    /** 読書ステータス (デフォルトは未読) */
    @ColumnInfo(name = "status")
    val status: BookStatus = BookStatus.UNREAD,

    /** アプリへの追加日時 (Unixタイムスタンプ milliseconds) */
    @ColumnInfo(name = "added_date")
    val addedDate: Long = System.currentTimeMillis(),

    /** 読了日時 (Unixタイムスタンプ milliseconds、任意) */
    @ColumnInfo(name = "read_date")
    val readDate: Long? = null,

    /** メモ (任意) */
    @ColumnInfo(name = "memo")
    val memo: String? = null
) {
    // Roomは data class の equals/hashCode を利用しますが、
    // ByteArray (coverImage) を含む場合の equals/hashCode の自動生成は意図通りに
    // 動かない可能性があるため、必要に応じて手動でオーバーライドします。
    // (今回は比較コストを考慮し、一旦自動生成に任せます)
    // ※リスト更新などで問題が出る場合は、coverImageを除外した比較を実装してください。
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Book

        if (id != other.id) return false
        if (title != other.title) return false
        if (author != other.author) return false
        if (coverImage != null) {
            if (other.coverImage == null) return false
            if (!coverImage.contentEquals(other.coverImage)) return false
        } else if (other.coverImage != null) return false
        if (status != other.status) return false
        if (addedDate != other.addedDate) return false
        if (readDate != other.readDate) return false
        if (memo != other.memo) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + title.hashCode()
        result = 31 * result + author.hashCode()
        result = 31 * result + (coverImage?.contentHashCode() ?: 0)
        result = 31 * result + status.hashCode()
        result = 31 * result + addedDate.hashCode()
        result = 31 * result + (readDate?.hashCode() ?: 0)
        result = 31 * result + (memo?.hashCode() ?: 0)
        return result
    }
}