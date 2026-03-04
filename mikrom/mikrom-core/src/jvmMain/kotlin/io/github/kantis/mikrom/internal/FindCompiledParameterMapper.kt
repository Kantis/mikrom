package io.github.kantis.mikrom.internal

import io.github.kantis.mikrom.ParameterMapper
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private val COMPILED_PARAMETER_MAPPER_CACHE = ConcurrentHashMap<KClass<*>, Any>()

private object ParameterMapperCacheMiss

public actual fun <T : Any> KClass<T>.compiledParameterMapper(): ParameterMapper<T>? {
   val cached = COMPILED_PARAMETER_MAPPER_CACHE[this]
   if (cached != null) {
      @Suppress("UNCHECKED_CAST")
      return if (cached === ParameterMapperCacheMiss) null else cached as ParameterMapper<T>
   }
   val mapper = invokeParameterMapperOnDefaultCompanion<T>(java)
      ?: findNestedParameterMapperClass<T>(java)
   COMPILED_PARAMETER_MAPPER_CACHE[this] = mapper ?: ParameterMapperCacheMiss
   @Suppress("UNCHECKED_CAST")
   return mapper
}

private fun <T : Any> invokeParameterMapperOnDefaultCompanion(jClass: Class<*>): ParameterMapper<T>? =
   jClass
      .companionOrNull("Companion")
      ?.let(::invokeParameterMapperOnCompanion)

@Suppress("UNCHECKED_CAST")
private fun <T : Any> invokeParameterMapperOnCompanion(companion: Any): ParameterMapper<T>? =
   try {
      companion
         .javaClass
         .getDeclaredMethod("parameterMapper")
         .invoke(companion) as? ParameterMapper<T>
   } catch (e: NoSuchMethodException) {
      null
   } catch (e: InvocationTargetException) {
      val cause = e.cause ?: throw e
      throw InvocationTargetException(cause, cause.message ?: e.message)
   }

@Suppress("UNCHECKED_CAST")
private fun <T : Any> findNestedParameterMapperClass(jClass: Class<*>): ParameterMapper<T>? =
   try {
      jClass.declaredClasses
         .singleOrNull { it.simpleName == "\$ParameterMapper" }
         ?.getField("INSTANCE")
         ?.get(null) as? ParameterMapper<T>
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
