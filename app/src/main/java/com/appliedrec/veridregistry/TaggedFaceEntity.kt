package com.appliedrec.veridregistry

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "tagged_faces")
data class TaggedFaceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,

    @ColumnInfo(name = "date_added")
    val dateAdded: Date,

    @ColumnInfo(name = "template_data", typeAffinity = ColumnInfo.BLOB)
    val templateData: FloatArray,

    @ColumnInfo(name = "user_name")
    val userName: String
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TaggedFaceEntity) return false

        if (id != other.id) return false
        if (dateAdded != other.dateAdded) return false
        if (!templateData.contentEquals(other.templateData)) return false
        if (userName != other.userName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + templateData.contentHashCode()
        result = 31 * result + userName.hashCode()
        return result
    }
}
