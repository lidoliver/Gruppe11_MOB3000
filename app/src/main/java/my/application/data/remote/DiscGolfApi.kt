package my.application.data.remote

import my.application.data.remote.api.DiscGolfApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object DiscGolfApi {
    // The base URL of the API. Always end it with a forward slash '/'.
    private const val BASE_URL = "https://api.example.com/v1/"

    // Build the Moshi object that Retrofit will be using to parse JSON
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Build the Retrofit instance
    private val retrofit = Retrofit.Builder()
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl(BASE_URL)
        .build()

    // A public val that exposes the lazy-initialized API service
    val service: DiscGolfApiService by lazy {
        retrofit.create(DiscGolfApiService::class.java)
    }
}
