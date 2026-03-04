package io.github.kantis.mikrom

public fun interface ParameterMapper<in T> {
   public fun mapParameters(value: T): Map<String, Any?>
}
