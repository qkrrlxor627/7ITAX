package com.ssafy.seveniTax.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// SecureStorage, BiometricHelper 모두 @Inject constructor + @Singleton 보유
// → Hilt가 자동 주입하므로 별도 @Provides 불필요 (이중 바인딩 제거)
@Module
@InstallIn(SingletonComponent::class)
object AppModule
