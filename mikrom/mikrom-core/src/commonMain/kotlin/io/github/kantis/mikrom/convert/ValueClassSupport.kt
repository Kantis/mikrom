package io.github.kantis.mikrom.convert

import kotlin.reflect.KClass

/**
 * Attempts to wrap [value] in a value class of type [targetClass].
 *
 * If [targetClass] is a value class and [value] is compatible with
 * its underlying type (directly or via [converters]), returns the
 * wrapped instance. Otherwise returns null.
 */
internal expect fun tryWrapValueClass(
   value: Any,
   targetClass: KClass<*>,
   converters: TypeConverters,
): Any?
