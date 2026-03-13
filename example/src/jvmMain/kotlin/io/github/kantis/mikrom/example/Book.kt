package io.github.kantis.mikrom.example

import io.github.kantis.mikrom.generator.MikromResult

@JvmInline
public value class BookId(public val value: Long)

@MikromResult
public data class Book(val id: BookId, val author: String, val title: String, val numberOfPages: Int)
