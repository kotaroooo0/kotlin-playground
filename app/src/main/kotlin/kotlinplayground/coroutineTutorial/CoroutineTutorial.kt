package kotlinplayground.coroutineTutorial

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import java.io.IOException
import java.time.LocalDateTime

fun main() {
    val coroutineTutorial = CoroutineTutorial()
    coroutineTutorial.run()
}

class CoroutineTutorial {

    fun run() {
        runBlocking {
            val allUrls = (1..10).map { "https://api.example.com/endpoint$it" }
            val concurrentSize = 2

            val responsesChannel = Channel<Pair<String, ApiResponse>>()

            launch {
                fetchResponsesConcurrently(allUrls, concurrentSize, responsesChannel)
                responsesChannel.close()
            }

            for (response in responsesChannel) {
                val (url, result) = response
                println("${LocalDateTime.now()}:$url")
                when (result) {
                    is ApiResponse.Success -> println("Success from $url: ${result.data}")
                    is ApiResponse.Error -> println("Error from $url: ${result.errorMessage}")
                }
            }
        }
    }

    sealed class ApiResponse {
        data class Success(val data: String) : ApiResponse()
        data class Error(val errorMessage: String) : ApiResponse()
    }

    private suspend fun fetchResponsesConcurrently(
        urls: List<String>,
        concurrentSize: Int,
        channel: Channel<Pair<String, ApiResponse>>
        ) = coroutineScope {
        val semaphore = Semaphore(concurrentSize)

        urls.forEach { url ->
            launch {
                semaphore.acquire()
                try {
                    val result = mockApiResponse(url)
                    channel.send(url to result)
                } catch (e: IOException) {
                    val result = mockApiResponse(url)
                    channel.send(url to result)
                } finally {
                    semaphore.release()
                }
            }
        }
    }

    private suspend fun mockApiResponse(url: String): ApiResponse {
        val list = listOf(1000L, 2000L, 3000L)
        val rand = list.random()
        println("$url:$rand")
        delay(rand)
        return ApiResponse.Success(url)
    }
}