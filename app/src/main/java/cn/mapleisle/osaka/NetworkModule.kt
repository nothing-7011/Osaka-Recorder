package cn.mapleisle.osaka

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

// ==========================================
// 1. Gemini 原生数据模型
// ==========================================

data class GeminiRequest(
    val contents: List<GeminiContent>
    // Gemini 原生其实支持 system_instruction 字段，但为了简化兼容，
    // 我们将 System Prompt 拼接到 User 文本里，效果是一样的。
)

data class GeminiContent(
    val role: String = "user",
    val parts: List<GeminiPart>
)

data class GeminiPart(
    val text: String? = null,
    val inline_data: GeminiBlob? = null
)

data class GeminiBlob(
    val mime_type: String,
    val data: String
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

data class GeminiCandidate(
    val content: GeminiContent?,
    val finishReason: String?
)

// ==========================================
// 2. API 接口定义 (Gemini 原生路径)
// ==========================================

interface GeminiApi {
    @POST("models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// ==========================================
// 3. 网络客户端工厂
// ==========================================

object NetworkClient {

    class RetryInterceptor(private val maxRetries: Int) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var request = chain.request()
            var response: Response? = null
            var exception: IOException? = null
            var tryCount = 0

            while (tryCount < maxRetries) {
                try {
                    response = chain.proceed(request)
                    if (response.isSuccessful) return response
                    if (response.code in 400..499) return response
                    response.close()
                } catch (e: IOException) {
                    exception = e
                }
                tryCount++
                try { Thread.sleep(500L * tryCount) } catch (e: InterruptedException) {}
            }
            throw exception ?: IOException("Failed after $tryCount retries")
        }
    }

    fun createService(userBaseUrl: String, timeoutSeconds: Long, maxRetries: Int): GeminiApi {
        val formattedUrl = if (userBaseUrl.endsWith("/")) userBaseUrl else "$userBaseUrl/"

        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .writeTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .addInterceptor(RetryInterceptor(maxRetries))
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(formattedUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeminiApi::class.java)
    }
}