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
    // Oppdater denne URL-en for å matche backend-serveren din
    // For lokal testing på emulator, bruk: http://10.0.2.2:5000/
    // For lokal testing på fysisk enhet, bruk: http://DIN_PC_IP:5000/
    // For produksjon, bruk din deployede backend-URL

    // Bruk 10.0.2.2 for Android Emulator (mapper til vertens localhost)
    // Bruk 10.38.11.46 for fysisk enhet (din datamaskins nåværende WiFi-IP)
    //private const val BASE_URL = "http://192.168.11.11:5000/"
    //private const val BASE_URL = "http://10.0.2.2:5000/"

    //heroku base url
    private const val BASE_URL = "https://gruppe11-discgolf-app-e20a44954943.herokuapp.com/"

    // Bygg Moshi-objektet for JSON-parsing
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Legg til logging-interceptor for debugging
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    // Bygg OkHttpClient med interceptor
    private val httpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Bygg Retrofit-instansen
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(BASE_URL)
        .client(httpClient)
        .build()

    // Eksponer API-tjenesten
    val service: BackendApiService by lazy {
        retrofit.create(BackendApiService::class.java)
    }



}
