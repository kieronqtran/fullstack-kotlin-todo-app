package com.github.kieronqtran

import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@CrossOrigin(
        origins = arrayOf("*"),
        maxAge = 3600,
        methods = arrayOf(RequestMethod.POST, RequestMethod.GET, RequestMethod.OPTIONS, RequestMethod.PATCH, RequestMethod.DELETE),
        allowedHeaders = arrayOf("x-requested-with", "origin", "content-type", "accept")
)
@RestController
@Suppress("unused")
class TodoController(val repository: TodoRepository) {

	@GetMapping("/")
	fun listTodo() = Flux.fromIterable(repository.findAll())

	@PostMapping("/")
	fun createTodo(@RequestBody todo: Todo?) = if(todo != null) Mono.just(repository.save(todo)) else Mono.empty()

	@DeleteMapping("/")
	fun clean() {
		repository.deleteAll()
	}


	@GetMapping("/{id}")
	fun getTodo(@PathVariable("id") id: Long) =
			if (!repository.existsById(id)) Mono.empty()
			else Mono.just(repository.findById(id).get())

	@DeleteMapping("/{id}")
	fun remove(@PathVariable("id") id: Long) {
		repository.deleteById(id)
	}

	@PatchMapping("/{id}")
	fun update(@PathVariable("id") id: Long, @RequestBody todo: Todo): Mono<Todo> {
		if(!repository.existsById(id)) return Mono.empty()
        val t : Todo = repository.findById(id).get()
        t.title = todo.title
        t.completed = todo.completed
        t.order = todo.order
		val result = repository.save(t)
		return Mono.just(result)
	}
}