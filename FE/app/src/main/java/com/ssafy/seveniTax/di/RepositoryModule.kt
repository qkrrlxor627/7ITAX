package com.ssafy.seveniTax.di

import com.ssafy.seveniTax.data.repository.*
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindPayRepository(impl: PayRepositoryImpl): PayRepository

    @Binds @Singleton
    abstract fun bindCardRepository(impl: CardRepositoryImpl): CardRepository

    @Binds @Singleton
    abstract fun bindBookEntryRepository(impl: BookEntryRepositoryImpl): BookEntryRepository

    @Binds @Singleton
    abstract fun bindTaxRepository(impl: TaxRepositoryImpl): TaxRepository

    @Binds @Singleton
    abstract fun bindClassificationRepository(impl: ClassificationRepositoryImpl): ClassificationRepository

    @Binds @Singleton
    abstract fun bindChatbotRepository(impl: ChatbotRepositoryImpl): ChatbotRepository
}
