package io.github.kantis.mikrom.example

import io.github.kantis.mikrom.generator.RowMapped

@RowMapped
public data class Book(val author: String, val title: String, val numberOfPages: Int)

public fun main() {
}
