package io.github.kantis.mikrom.example

import io.github.kantis.mikrom.generator.MikromParameter

@MikromParameter
public data class CreateBookCommand(val author: String, val title: String, val numberOfPages: Int)
