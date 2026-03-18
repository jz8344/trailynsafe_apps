package com.example.trailynapp.driver.api

import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException

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

        private val networkExceptionHandler = okhttp3.Interceptor { chain ->
            try {
                chain.proceed(chain.request())
            } catch (e: Exception) {
                // Atrapa IOException, SocketTimeoutException, UnknownHostException, etc.
                // y retorna un response 503 simulado para evitar crashes.
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(503)
                    .message("Sin conexion a internet")
                    .body("{\"message\": \"Error de conexion o servidor inalcanzable\"}".toResponseBody(null))
                    .build()
            }
        }

        private val okHttpClient =
                OkHttpClient.Builder()
                        .addInterceptor(networkExceptionHandler)
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
