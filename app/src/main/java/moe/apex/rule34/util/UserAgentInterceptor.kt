package moe.apex.rule34.util

import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {
    companion object {
        private const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        builder.addHeader("User-Agent", DEFAULT_USER_AGENT)
        return chain.proceed(builder.build())
    }
}
