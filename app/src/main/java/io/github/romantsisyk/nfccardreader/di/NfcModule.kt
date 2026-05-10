package io.github.romantsisyk.nfccardreader.di

import android.content.Context
import androidx.room.Room
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.romantsisyk.nfccardreader.data.local.NfcDatabase
import io.github.romantsisyk.nfccardreader.data.local.dao.ScanDao
import io.github.romantsisyk.nfccardreader.data.repository.NfcRepositoryImpl
import io.github.romantsisyk.nfccardreader.domain.repository.NfcRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NfcModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NfcDatabase =
        Room.databaseBuilder(context, NfcDatabase::class.java, NfcDatabase.DATABASE_NAME)
            .addMigrations(NfcDatabase.MIGRATION_1_2)
            .build()

    @Provides
    @Singleton
    fun provideScanDao(database: NfcDatabase): ScanDao = database.scanDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindNfcRepository(impl: NfcRepositoryImpl): NfcRepository
}
