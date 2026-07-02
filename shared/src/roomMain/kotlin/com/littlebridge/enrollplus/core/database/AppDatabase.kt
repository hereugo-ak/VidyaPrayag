package com.littlebridge.enrollplus.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.littlebridge.enrollplus.feature.library.data.local.LibraryBookDao
import com.littlebridge.enrollplus.feature.library.data.local.LibraryBookEntity
import com.littlebridge.enrollplus.feature.library.data.local.LibraryCacheDao
import com.littlebridge.enrollplus.feature.library.data.local.LibraryCacheEntity
import com.littlebridge.enrollplus.feature.library.data.local.LibraryPendingActionDao
import com.littlebridge.enrollplus.feature.library.data.local.LibraryPendingActionEntity
import com.littlebridge.enrollplus.feature.schools.data.local.SchoolDao
import com.littlebridge.enrollplus.feature.schools.data.local.SchoolEntity

@Database(
    entities = [
        SchoolEntity::class,
        LibraryBookEntity::class,
        LibraryCacheEntity::class,
        LibraryPendingActionEntity::class,
    ],
    version = 2,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun libraryBookDao(): LibraryBookDao
    abstract fun libraryCacheDao(): LibraryCacheDao
    abstract fun libraryPendingActionDao(): LibraryPendingActionDao
    companion object
}

// Room generator will provide this on iOS
@Suppress("NO_ACTUAL_FOR_EXPECT")
expect fun AppDatabase.Companion.instantiateImpl(): AppDatabase
