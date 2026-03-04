package io.github.kantis.mikrom.internal

import kotlin.reflect.KClass

public actual fun <T : Any> KClass<T>.compiledRowMapper(): io.github.kantis.mikrom.RowMapper<T>? {
   TODO("Not yet implemented")
}

public actual fun <T : Any> KClass<T>.compiledParameterMapper(): io.github.kantis.mikrom.ParameterMapper<T>? {
   TODO("Not yet implemented")
}
