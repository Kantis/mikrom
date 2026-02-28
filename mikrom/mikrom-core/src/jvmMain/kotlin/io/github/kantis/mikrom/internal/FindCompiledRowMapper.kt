package io.github.kantis.mikrom.internal

import io.github.kantis.mikrom.RowMapper
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val COMPILED_ROW_MAPPER_CACHE = ConcurrentHashMap<KClass<*>, Any>()

private object CacheMiss

public actual fun <T : Any> KClass<T>.compiledRowMapper(): RowMapper<T>? {
   val cached = COMPILED_ROW_MAPPER_CACHE[this]
   if (cached != null) {
      @Suppress("UNCHECKED_CAST")
      return if (cached === CacheMiss) null else cached as RowMapper<T>
   }
   val mapper = invokeRowMapperOnDefaultCompanion<T>(java)
      ?: findNestedRowMapperClass<T>(java)
   COMPILED_ROW_MAPPER_CACHE[this] = mapper ?: CacheMiss
   @Suppress("UNCHECKED_CAST")
   return mapper
}

private fun <T : Any> invokeRowMapperOnDefaultCompanion(jClass: Class<*>): RowMapper<T>? =
   jClass
      .companionOrNull("Companion")
      ?.let(::invokeRowMapperOnCompanion)

@Suppress("UNCHECKED_CAST")
private fun <T : Any> invokeRowMapperOnCompanion(companion: Any): RowMapper<T>? =
   try {
      companion
         .javaClass
         .getDeclaredMethod("rowMapper")
         .invoke(companion) as? RowMapper<T>
   } catch (e: NoSuchMethodException) {
      null
   } catch (e: InvocationTargetException) {
      val cause = e.cause ?: throw e
      throw InvocationTargetException(cause, cause.message ?: e.message)
   }

@Suppress("UNCHECKED_CAST")
private fun <T : Any> findNestedRowMapperClass(jClass: Class<*>): RowMapper<T>? =
   try {
      jClass.declaredClasses
         .singleOrNull { it.simpleName == "\$RowMapper" }
         ?.getField("INSTANCE")
         ?.get(null) as? RowMapper<T>
   } catch (_: Exception) {
      null
   }

private fun Class<*>.companionOrNull(companionName: String) =
   try {
      val companion = getDeclaredField(companionName)
      companion.isAccessible = true
      companion.get(null)
   } catch (e: Throwable) {
      null
   }
