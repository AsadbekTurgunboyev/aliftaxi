package com.example.taxi.di

import android.content.Context
import com.chuckerteam.chucker.api.ChuckerInterceptor
import com.example.taxi.data.repository.RegisterRepositoryImpl
import com.example.taxi.data.source.ApiService
import com.example.taxi.domain.preference.UserPreferenceManager
import com.example.taxi.domain.repository.RegisterRepository
import com.example.taxi.domain.usecase.register.GetRegisterResponseUseCase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit


private const val TIME_OUT = 30L
//const val BASE_URL: String = "https://my.mrtaxi.uz/api/driver/"
//const val MAIN_URL: String = "https://my.mrtaxi.uz"
//const val BASE_URL: String = "https://my.likestar.uz/api/driver/"
//const val MAIN_URL: String = "https://my.likestar.uz"
//const val BASE_URL: String = "https://wintaxi.uz/api/driver/"
//const val MAIN_URL: String = "https://wintaxi.uz"
//const val BASE_URL: String = "https://my.xamkortaxi.uz/api/driver/"
//const val MAIN_URL: String = "https://my.xamkortaxi.uz"
const val BASE_URL: String = "https://lidertaxi.uz/api/driver/"
const val MAIN_URL: String = "https://lidertaxi.uz"
//const val BASE_URL: String = "https://aliftaxi.uz/api/driver/"
//const val MAIN_URL: String = "https://aliftaxi.uz"

val NetworkModule = module {

    single { createService(get()) }

    single { createRetrofit(get(), BASE_URL) }

    single { createOkHttpClient(get(),androidContext()) }
    single { UserPreferenceManager(androidContext()) }


}

//    fun createPostRepository(apiService: ApiService): PostsRepository {
//        return PostsRepositoryImp(apiService)
//    }
//
//    fun createGetPostsUseCase(postsRepository: PostsRepository): GetPostsUseCase {
//        return GetPostsUseCase(postsRepository)
//    }
fun createOkHttpClient(pref: UserPreferenceManager,context: Context): OkHttpClient {


    val httpLoggingInterceptor = HttpLoggingInterceptor()
    httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.NONE

    return OkHttpClient.Builder()
        .connectTimeout(TIME_OUT, TimeUnit.SECONDS)
        .readTimeout(TIME_OUT, TimeUnit.SECONDS)
         .addInterceptor(httpLoggingInterceptor)
        .addInterceptor(ChuckerInterceptor(context))
        .addInterceptor { chain ->
            val originalRequest = chain.request()
            val token = pref.getToken()
            val language = pref.getLanguage().code

            val request = originalRequest.newBuilder()
                .apply {
                    header("Accept-Language", language)
                    token?.let { header("Authorization", "Bearer $it") }
                }
                .build()

            chain.proceed(request)
        }
        .build()
}

fun createRetrofit(okHttpClient: OkHttpClient, url: String): Retrofit {
    val gson: Gson = GsonBuilder().create()

    return Retrofit.Builder()
        .baseUrl(url)
        .client(okHttpClient)
        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
        .addConverterFactory(GsonConverterFactory.create(gson)).build()
}

fun createService(retrofit: Retrofit): ApiService {
    return retrofit.create(ApiService::class.java)

}


fun createRegisterRepository(apiService: ApiService): RegisterRepository {
    return RegisterRepositoryImpl(apiService)
}

fun createGetRegistersUseCase(registerRepository: RegisterRepository): GetRegisterResponseUseCase {
    return GetRegisterResponseUseCase(registerRepository)
}

