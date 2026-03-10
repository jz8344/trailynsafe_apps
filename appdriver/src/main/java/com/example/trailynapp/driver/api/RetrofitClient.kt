package com.example.trailynapp.driver.api

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

        // URL del backend en Railway
        private const val BASE_URL = "https://api.trailynsafe.lat/"

        private val loggingInterceptor =
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        private val headerInterceptor =
                okhttp3.Interceptor { chain ->
                        val request =
                                chain.request()
                                        .newBuilder()
                                        .addHeader("Accept", "application/json")
                                        .build()
                        chain.proceed(request)
                }

        private val okHttpClient =
                OkHttpClient.Builder()
                        .addInterceptor(headerInterceptor)
                        .addInterceptor(loggingInterceptor)
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(30, TimeUnit.SECONDS)
                        .writeTimeout(30, TimeUnit.SECONDS)
                        .build()

        private val retrofit =
                Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(okHttpClient)
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()

        val apiService: ApiService = retrofit.create(ApiService::class.java)
}
