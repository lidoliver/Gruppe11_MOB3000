package my.application.data.remote

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import my.application.data.remote.api.PlayerApi
import my.application.data.remote.api.BackendApiService
import java.util.concurrent.TimeUnit

object RetrofitProvider {
    // For Android Emulator use: http://10.0.2.2:5000/
    // For Physical Device use: http://YOUR_COMPUTER_IP:5000/
    private const val BASE_URL = "http://localhost:5000/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val playerApi: PlayerApi by lazy {
        retrofit.create(PlayerApi::class.java)
    }

    val backendApiService: BackendApiService by lazy {
        retrofit.create(BackendApiService::class.java)
    }
}

