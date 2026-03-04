package io.github.kantis.mikrom

import kotlin.reflect.KClass

internal expect val platformNonMappedPrimitives: Set<KClass<*>>

private val commonNonMappedPrimitives: Set<KClass<*>> = setOf(
   String::class,
   Int::class,
   Long::class,
   Float::class,
   Double::class,
   Boolean::class,
)

public val nonMappedPrimitives: Set<KClass<*>> = commonNonMappedPrimitives + platformNonMappedPrimitives
