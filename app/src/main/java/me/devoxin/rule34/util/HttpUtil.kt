package me.devoxin.rule34.util

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CompletableFuture

object HttpUtil {
    private val httpClient = OkHttpClient()

    fun get(url: String): HttpRequest {
        return HttpRequest(Request.Builder().url(url).get().build())
    }

    class HttpRequest(private val request: Request) {
        fun string(): CompletableFuture<String> {
            return CompletableFuture<String>().also {
                doRequest(it) { res -> res.body()!!.string() }
            }
        }

        fun byteStream(): CompletableFuture<InputStream> {
            return CompletableFuture<InputStream>().also {
                doRequest(it) { res -> res.body()!!.byteStream() }
            }
        }

        private fun <T> doRequest(future: CompletableFuture<T>, transform: (Response) -> T) {
            httpClient.newCall(request).enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        future.completeExceptionally(IOException("Request was unsuccessful, response code: ${response.code()}"))
                        return response.close()
                    }

                    future.complete(transform(response))
                }

                override fun onFailure(call: Call, e: IOException) {
                    future.completeExceptionally(e)
                }
            })
        }
    }
}
