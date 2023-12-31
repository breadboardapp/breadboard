package me.devoxin.rule34

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CompletableFuture

object RequestUtil {
    private val client = OkHttpClient()

    fun get(url: String, apply: Request.Builder.() -> Unit = {}): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        val req = Request.Builder().url(url).apply(apply).get().build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                future.complete(response.body()!!.string())
            }
        })

        return future
    }
}
