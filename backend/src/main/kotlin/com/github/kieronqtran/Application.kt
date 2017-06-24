package com.github.kieronqtran

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan


@SpringBootApplication
@ComponentScan
open class Application {

    object Config {
        var root: String = "http://localhost:8080"
    }

    companion object {
        @JvmStatic fun main(args: Array<String>) {
            if (args.isNotEmpty()) Config.root=args[0]
            SpringApplication.run(Application::class.java, *args)
        }
    }
}

