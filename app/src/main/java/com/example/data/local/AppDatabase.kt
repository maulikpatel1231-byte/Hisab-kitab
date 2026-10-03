package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.EmailGroup
import com.example.data.model.Person
import com.example.data.model.Project
import com.example.data.model.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [Person::class, Transaction::class, Project::class, EmailGroup::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun personDao(): PersonDao
    abstract fun transactionDao(): TransactionDao
    abstract fun projectDao(): ProjectDao
    abstract fun emailGroupDao(): EmailGroupDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "hisab_kitab_db"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Seed default project and default email group on DB creation
                            CoroutineScope(Dispatchers.IO).launch {
                                INSTANCE?.projectDao()?.insertProject(
                                    Project(
                                        id = 1L,
                                        name = "Daily Book (Mera Hisab)",
                                        description = "Primary daily personal / business book",
                                        ownerEmail = "Maulikpatel1231@gmail.com",
                                        sharedEmails = "",
                                        colorHex = "#0F766E"
                                    )
                                )
                                INSTANCE?.emailGroupDao()?.insertGroup(
                                    EmailGroup(
                                        id = 1L,
                                        name = "Partners & Team",
                                        memberEmails = "Maulikpatel1231@gmail.com, partner@gmail.com"
                                    )
                                )
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
