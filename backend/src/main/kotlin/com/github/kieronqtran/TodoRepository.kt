package com.github.kieronqtran

import org.springframework.data.repository.CrudRepository

interface TodoRepository : CrudRepository<Todo, Long> { }
