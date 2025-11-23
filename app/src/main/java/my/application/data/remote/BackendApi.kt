package my.application.data.remote

import my.application.data.remote.api.BackendApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object BackendApi {
    // Update this URL to match your backend server
    // For local testing on emulator, use: http://10.0.2.2:5000/
    // For local testing on physical device, use: http://YOUR_COMPUTER_IP:5000/
    // For production, use your deployed backend URL

    // Use 10.0.2.2 for Android Emulator (maps to host's localhost)
    // Use 10.38.11.46 for Physical Device (your computer's current WiFi IP)
    //private const val BASE_URL = "http://192.168.11.11:5000/"
    private const val BASE_URL = "http://10.0.2.2:5000/"

    // Build the Moshi object for JSON parsing
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Add logging interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Build OkHttpClient with interceptor
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Build the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(BASE_URL)
        .client(httpClient)
        .build()

    // Expose the API service
    val service: BackendApiService by lazy {
        retrofit.create(BackendApiService::class.java)
    }
}
