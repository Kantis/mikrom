package io.github.kantis.mikrom.plugin

import org.jetbrains.kotlin.GeneratedDeclarationKey
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType

internal data object MikromGenerateRowMapperKey : GeneratedDeclarationKey()

internal data class MikromGenerateRowMapperClassKey(
   val ownerClassSymbol: FirClassSymbol<*>,
   val rowMapperType: ConeClassLikeType,
) : GeneratedDeclarationKey()

internal data class MikromGenerateCompanionKey(
   val ownerClassSymbol: FirClassSymbol<*>,
) : GeneratedDeclarationKey()

internal data class MikromGenerateRowMapperAccessorKey(
   val ownerClassSymbol: FirClassSymbol<*>,
   val rowMapperType: ConeClassLikeType,
) : GeneratedDeclarationKey()
