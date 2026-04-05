package moe.apex.breadboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import moe.apex.breadboard.util.UserAgentInterceptor
import okhttp3.*
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object RequestUtil {
    private val client = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor())
        .build()


    suspend fun get(url: String, apply: Request.Builder.() -> Unit = {}): String = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url).apply(apply).get().build()
        
        suspendCancellableCoroutine { continuation ->
            val call = client.newCall(req)

            continuation.invokeOnCancellation {
                call.cancel()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        response.use {
                            val body = it.body.string()
                            continuation.resume(body)
                        }
                    }
                }
            })
        }
    }
}
