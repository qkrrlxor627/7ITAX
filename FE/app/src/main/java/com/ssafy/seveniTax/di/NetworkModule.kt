package com.ssafy.seveniTax.di

import com.ssafy.seveniTax.data.local.SecureStorage
import com.ssafy.seveniTax.data.remote.*
import com.ssafy.seveniTax.data.remote.PaymentApi
import com.ssafy.seveniTax.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(secureStorage: SecureStorage): OkHttpClient =
        ApiClient.buildOkHttpClient(secureStorage)

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides @Singleton
    fun providePayApi(retrofit: Retrofit): PayApi = retrofit.create(PayApi::class.java)

    @Provides @Singleton
    fun provideCardApi(retrofit: Retrofit): CardApi = retrofit.create(CardApi::class.java)

    @Provides @Singleton
    fun provideBookEntryApi(retrofit: Retrofit): BookEntryApi = retrofit.create(BookEntryApi::class.java)

    @Provides @Singleton
    fun provideClassificationApi(retrofit: Retrofit): ClassificationApi = retrofit.create(ClassificationApi::class.java)

    @Provides @Singleton
    fun provideTaxCalendarApi(retrofit: Retrofit): TaxCalendarApi = retrofit.create(TaxCalendarApi::class.java)

    @Provides @Singleton
    fun provideTaxEstimationApi(retrofit: Retrofit): TaxEstimationApi = retrofit.create(TaxEstimationApi::class.java)

    @Provides @Singleton
    fun provideExportApi(retrofit: Retrofit): ExportApi = retrofit.create(ExportApi::class.java)

    @Provides @Singleton
    fun providePaymentApi(retrofit: Retrofit): PaymentApi = retrofit.create(PaymentApi::class.java)

    @Provides @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi = retrofit.create(NotificationApi::class.java)

    @Provides @Singleton
    fun provideChatbotApi(retrofit: Retrofit): ChatbotApi = retrofit.create(ChatbotApi::class.java)
}
