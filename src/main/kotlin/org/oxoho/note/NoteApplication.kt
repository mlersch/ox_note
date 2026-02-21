package org.oxoho.note

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class NoteApplication

fun main(args: Array<String>) {
	runApplication<NoteApplication>(*args)
}
