package io.github.kantis.mikrom.convert

import kotlin.reflect.KClass

internal actual fun tryWrapValueClass(
   value: Any,
   targetClass: KClass<*>,
   converters: TypeConverters,
): Any? {
   val javaClass = targetClass.java

   // Value classes on JVM always have a static box-impl method
   val boxImpl = javaClass.declaredMethods
      .firstOrNull { it.name == "box-impl" && it.parameterCount == 1 }
      ?: return null

   val paramType = boxImpl.parameterTypes[0].kotlin

   if (paramType.isInstance(value)) {
      return boxImpl.invoke(null, value)
   }

   val converted = converters.convert(value, paramType)
   if (converted != null && paramType.isInstance(converted)) {
      return boxImpl.invoke(null, converted)
   }

   return null
}
