package io.github.kantis.mikrom

import java.math.BigDecimal
import kotlin.reflect.KClass

internal actual val platformNonMappedPrimitives: Set<KClass<*>> = setOf(
   BigDecimal::class,
)
