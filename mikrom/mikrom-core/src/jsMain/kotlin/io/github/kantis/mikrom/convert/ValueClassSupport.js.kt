package io.github.kantis.mikrom

import kotlin.reflect.KClass

internal actual fun tryWrapValueClass(
   value: Any,
   targetClass: KClass<*>,
   converters: TypeConverters,
): Any? = null
