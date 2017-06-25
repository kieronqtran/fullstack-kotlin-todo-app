package com.github.kieronqtran

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.damo.aspen.Test
import io.damo.aspen.spring.SpringTestTreeRunner
import okhttp3.*
import org.amshove.kluent.*
import org.junit.runner.RunWith
import org.springframework.boot.test.context.SpringBootTest
import java.io.IOException


@RunWith(SpringTestTreeRunner::class)
@SpringBootTest(classes = arrayOf(Application::class),webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class TodoControllerTest: Test() {
    private val baseUrl = Application.Config.root
    private val client = OkHttpClient()
    init {
//        Adapted from todoBackEnd test
        describe("the pre-requisites") {
            before { delete(baseUrl) }

            test("the api root responds to a GET (i.e. the server is up and accessible, CORS headers are set up)") {
                val response = get(baseUrl)
                response.code().shouldEqualTo(200)
            }

            test("the api root responds to a POST with the todo which was posted to it") {
                val result: Todo = postAndReturnValue(baseUrl, Todo(title = "a todo"))
                result.title.`should equal to`("a todo")
            }

            test("after a DELETE the api root responds to a GET with a JSON representation of an empty array") {
                delete(baseUrl)
                val result: Array<Todo> = getAndReturnValue(baseUrl)
                result.size shouldEqualTo 0
                result shouldEqual arrayOf<Todo>()
            }
        }

        describe("storing new todos by posting to the root url") {
            before { delete(baseUrl) }

            test("adds a new todo to the list of todos at the root url") {
                post(baseUrl, Todo(title = "walk the dog"))
                val result: Array<Todo> = getAndReturnValue(baseUrl)
                result.size shouldEqualTo 1
                result[0].title shouldEqualTo "walk the dog"
            }

            fun createTodoAndVerifyItLooksValidWith(callback: (Todo) -> Unit) {
                val result1 = postAndReturnValue(baseUrl, Todo(title = "walk the dog"))
                callback(result1)
                val result: Array<Todo> = getAndReturnValue(baseUrl)
                callback(result[0])
            }

            test("sets up a new todo as initially not completed") {
                createTodoAndVerifyItLooksValidWith {
                    it.completed `should equal to` false
                }
            }

            test("each new todo has a url") {
                createTodoAndVerifyItLooksValidWith {
                    it.url shouldBeInstanceOf String::class.java
                }
            }

            test("each new todo has a url, which returns a todo") {
                val newTodo: Todo = postAndReturnValue(baseUrl, Todo(title = "my todo"))
                val fetchedTodo: Todo = getAndReturnValue(newTodo.url)
                fetchedTodo.title `should equal to` "my todo"
            }
        }

        describe("working with an existing todo") {
            before { delete(baseUrl) }

            test("can navigate from a list of todos to an individual todo via urls") {
                val todoList: Array<Todo> = arrayOf(Todo(title = "todo the first"), Todo(title = "todo the second"))
                todoList.forEach { post(baseUrl, it) }
                val fetchedTodoList: Array<Todo> = getAndReturnValue(baseUrl)
                fetchedTodoList.size shouldEqualTo 2
                val fetchedFirstTodo: Todo = getAndReturnValue(fetchedTodoList[0].url)
                fetchedFirstTodo.title `should equal to` "todo the first"
            }

            test("can change the todo's title by PATCHing to the todo's url") {
                //create fresh todo and get it url
                val newTodo: Todo = postAndReturnValue(baseUrl, Todo(title = "initial title"))
                val updatedTodo: Todo = patchAndReturnValue(newTodo.url, hashMapOf(Pair("title", "bathe the cat"))) // create JSON object with title only
                updatedTodo.title shouldEqualTo "bathe the cat"
            }

            test("can change the todo's completeness by PATCHing to the todo's url") {
                val newTodo: Todo = postAndReturnValue(baseUrl, Todo(title = "whatever"))
                val updatedTodo: Todo = patchAndReturnValue(newTodo.url, hashMapOf(Pair("completed", true)))
                updatedTodo.completed `should equal to` true
            }

            test("changes to a todo are persisted and show up when re-fetching the todo") {
                val newTodo: Todo = postAndReturnValue(baseUrl, Todo(title = "initial title"))
                val updatedTodo: Todo = patchAndReturnValue(newTodo.url, hashMapOf(Pair("title", "changed title"), Pair("completed", true)))

                fun verifiedTodoProperties(todo: Todo) {
                    todo.completed `should equal to` true
                    todo.title `should equal to` "changed title"
                }
                val fetchedTodo: Todo = getAndReturnValue(updatedTodo.url)
                verifiedTodoProperties(fetchedTodo)
                val todoList: Array<Todo> = getAndReturnValue(baseUrl)
                todoList.size `should equal to` 1
                verifiedTodoProperties(todoList[0])
            }

            test("can delete a todo making a DELETE request to the todo's url") {
                val newTodo: Todo = postAndReturnValue(baseUrl, Todo(title = "initial title"))
                delete(newTodo.url)
                val todoList: Array<Todo> = getAndReturnValue(baseUrl)
                todoList.size `should equal to` 0
            }
        }

        describe("tracking todo order") {
            test("can create a todo with an order field") {
                val postResult = postAndReturnValue(baseUrl, Todo(title = "blah", order = 523))
                postResult.order `should equal to` 523
            }

            test("can PATCH a todo to change its order") {
                val newTodo: Todo = postAndReturnValue(baseUrl, Todo(title = "initial title", order = 10))
                val updatedTodo: Todo = patchAndReturnValue(newTodo.url, hashMapOf(Pair("order", 95)))
                updatedTodo.order `should equal to` 95
            }

            test("remembers changes to a todo's order") {
                val newTodo: Todo = postAndReturnValue(baseUrl, Todo(title = "initial title", order = 10))
                val updatedTodo: Todo = patchAndReturnValue(newTodo.url, hashMapOf(Pair("order", 95)))
                val refetchTodo: Todo = getAndReturnValue(updatedTodo.url)
                refetchTodo.order `should equal to` 95
            }
        }
    }

    fun get(url: String): Response {
        val request = Request.Builder()
                .get()
                .url(url)
                .build()
        return client.newCall(request).execute()
    }

    inline fun <reified T: Any> getAndReturnValue(url: String): T {
        val request = Request.Builder()
                .get()
                .url(url)
                .build()
        val response = OkHttpClient().newCall(request).execute()
        return jacksonObjectMapper().readValue(response.body()?.byteStream(), T::class.java)
    }

    fun getAsync(url: String, callback: (res: Response) -> Unit) {
        val request = Request.Builder()
                .get()
                .url(url)
                .build()
        client.newCall(request).enqueue(object: Callback {
            override fun onResponse(call: Call?, response: Response?) {
                callback(response!!)
            }

            override fun onFailure(call: Call?, e: IOException?) {
                e?.printStackTrace()
            }

        })
    }

    fun <T>post(url: String, body: T): Response {
        val request = Request.Builder()
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("text"),
                        jacksonObjectMapper().writeValueAsString(body)))
                .url(url)
                .build()
        return client.newCall(request).execute()
    }

    inline fun <reified Y: Any> postAndReturnValue(url: String, body: Y): Y {
        val client = OkHttpClient()
        val request = Request.Builder()
                .header("Content-Type", "application/json")
                .post(RequestBody.create(MediaType.parse("text"),
                        jacksonObjectMapper().writeValueAsString(body)))
                .url(url)
                .build()
        val response = client.newCall(request).execute()
        return jacksonObjectMapper().readValue(response.body()?.byteStream(), Y::class.java)
    }

    fun postAsync(url: String, todo: Todo, callback: (res: Response) -> Unit) {
        val request = Request.Builder()
                .post(RequestBody.create(MediaType.parse("application/json"),
                    jacksonObjectMapper().writeValueAsString(todo)))
                .url(url)
                .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call?, e: IOException?) {
                e?.printStackTrace()
            }

            @Throws(IOException::class)
            override fun onResponse(call: Call, response: Response) {
                callback(response)
            }
        })
    }

    fun delete(url: String): Response {
        val request = Request.Builder()
                .delete()
                .url(url)
                .build()
        return client.newCall(request).execute()
    }

    fun patch(url: String, body: Any): Response {
        val request = Request.Builder()
                .header("Content-Type", "application/json")
                .patch(RequestBody.create(MediaType.parse("text"),
                        jacksonObjectMapper().writeValueAsString(body)))
                .url(url)
                .build()
        return client.newCall(request).execute()
    }

    inline fun <reified T: Any>patchAndReturnValue(url: String, body: Any): T {
        val client = OkHttpClient()
        val request = Request.Builder()
                .header("Content-Type", "application/json")
                .patch(RequestBody.create(MediaType.parse("text"),
                        jacksonObjectMapper().writeValueAsString(body)))
                .url(url)
                .build()
        val response = client.newCall(request).execute()
        return jacksonObjectMapper().readValue(response.body()?.byteStream(), T::class.java)
    }
}
