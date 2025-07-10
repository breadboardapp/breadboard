package moe.apex.rule34.util

import moe.apex.rule34.BuildConfig
import okhttp3.Interceptor
import okhttp3.Response

class UserAgentInterceptor : Interceptor {
    companion object {
        private const val DEFAULT_USER_AGENT = "Breadboard/${BuildConfig.VERSION_NAME} https://github.com/breadboardapp/breadboard"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val builder = originalRequest.newBuilder()

        builder.addHeader("User-Agent", DEFAULT_USER_AGENT)
        return chain.proceed(builder.build())
    }
}
