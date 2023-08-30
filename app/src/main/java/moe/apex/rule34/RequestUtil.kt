package moe.apex.rule34

import okhttp3.*
import java.io.IOException
import java.util.concurrent.CompletableFuture

object RequestUtil {
    private val client = OkHttpClient()

    fun get(url: String): CompletableFuture<String> {
        val future = CompletableFuture<String>()
        val req = Request.Builder()
                .url(url)
                .get()
                .build()

        client.newCall(req).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                future.completeExceptionally(e)
            }

            override fun onResponse(call: Call, response: Response) {
                future.complete(response.body!!.string())
            }
        })

        return future
    }
}
