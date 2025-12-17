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
import io.github.romantsisyk.nfccardreader.domain.usecase.InterpretNfcDataUseCase
import io.github.romantsisyk.nfccardreader.domain.usecase.ParseTLVUseCase
import io.github.romantsisyk.nfccardreader.domain.usecase.ProcessNfcIntentUseCase
import javax.inject.Singleton

/**
 * Hilt module providing NFC-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object NfcModule {

    /**
     * Provides the Room database instance.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NfcDatabase {
        return Room.databaseBuilder(
            context,
            NfcDatabase::class.java,
            NfcDatabase.DATABASE_NAME
        ).build()
    }

    /**
     * Provides the ScanDao from the database.
     */
    @Provides
    @Singleton
    fun provideScanDao(database: NfcDatabase): ScanDao {
        return database.scanDao()
    }

    /**
     * Provides the InterpretNfcDataUseCase.
     */
    @Provides
    fun provideInterpretNfcDataUseCase(): InterpretNfcDataUseCase {
        return InterpretNfcDataUseCase()
    }

    /**
     * Provides the ParseTLVUseCase.
     */
    @Provides
    fun provideParseTLVUseCase(): ParseTLVUseCase {
        return ParseTLVUseCase()
    }

    /**
     * Provides the ProcessNfcIntentUseCase.
     */
    @Provides
    fun provideProcessNfcIntentUseCase(
        parseTLVUseCase: ParseTLVUseCase,
        interpretNfcDataUseCase: InterpretNfcDataUseCase
    ): ProcessNfcIntentUseCase {
        return ProcessNfcIntentUseCase(parseTLVUseCase, interpretNfcDataUseCase)
    }
}

/**
 * Hilt module for binding repository implementations.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds NfcRepositoryImpl to NfcRepository interface.
     */
    @Binds
    @Singleton
    abstract fun bindNfcRepository(impl: NfcRepositoryImpl): NfcRepository
}