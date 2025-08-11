package com.appliedrec.veridregistry

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [TaggedFaceEntity::class], version = 1, exportSchema = false)
@TypeConverters(DatabaseTypeConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taggedFaceDao(): TaggedFaceDao
}
