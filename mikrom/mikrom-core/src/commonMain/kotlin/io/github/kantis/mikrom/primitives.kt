package io.github.kantis.mikrom

import java.math.BigDecimal
import kotlin.reflect.KClass

public val nonMappedPrimitives: Set<KClass<*>> = setOf(
   String::class,
   Int::class,
   Long::class,
   Float::class,
   Double::class,
   Boolean::class,
   BigDecimal::class,
)
