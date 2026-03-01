package io.github.kantis.mikrom

import kotlin.reflect.KClass

internal actual fun tryWrapValueClass(
   value: Any,
   targetClass: KClass<*>,
   conversions: TypeConversions,
): Any? = null
