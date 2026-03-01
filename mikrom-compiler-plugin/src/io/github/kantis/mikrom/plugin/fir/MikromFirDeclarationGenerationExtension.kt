package io.github.kantis.mikrom.plugin.fir

import io.github.kantis.mikrom.plugin.MikromGenerateCompanionKey
import io.github.kantis.mikrom.plugin.MikromGenerateRowMapperAccessorKey
import io.github.kantis.mikrom.plugin.MikromGenerateRowMapperClassKey
import io.github.kantis.mikrom.plugin.MikromGenerateRowMapperKey
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.constructType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

public class MikromFirDeclarationGenerationExtension(
   session: FirSession,
) : FirDeclarationGenerationExtension(session) {
   private fun rowMapperType(typeArgument: ConeTypeProjection): ConeClassLikeType =
      ClassId(
         FqName("io.github.kantis.mikrom"),
         Name.identifier("RowMapper"),
      ).createConeType(session, typeArguments = arrayOf(typeArgument))

   private val rowType by lazy {
      ClassId(
         FqName("io.github.kantis.mikrom"),
         Name.identifier("Row"),
      ).createConeType(session)
   }

   private val mikromType by lazy {
      ClassId(
         FqName("io.github.kantis.mikrom"),
         Name.identifier("Mikrom"),
      ).createConeType(session)
   }

   override fun FirDeclarationPredicateRegistrar.registerPredicates() {
      register(ROW_MAPPED_PREDICATE)
      register(HAS_ROW_MAPPED_PREDICATE)
   }

   override fun getNestedClassifiersNames(
      classSymbol: FirClassSymbol<*>,
      context: NestedClassGenerationContext,
   ): Set<Name> {
      val provider = session.predicateBasedProvider
      if (!provider.matches(ROW_MAPPED_PREDICATE, classSymbol))
         return emptySet()

      return setOf(
         ROW_MAPPER_CLASS_NAME,
         SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT,
      )
   }

   override fun generateNestedClassLikeDeclaration(
      owner: FirClassSymbol<*>,
      name: Name,
      context: NestedClassGenerationContext,
   ): FirClassLikeSymbol<*>? =
      when (name) {
         SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanion(owner)
         ROW_MAPPER_CLASS_NAME -> generateRowMapperClass(owner)
         else -> null
      }

   private fun generateCompanion(owner: FirClassSymbol<*>): FirClassLikeSymbol<*> =
      createCompanionObject(
         owner = owner,
         key = MikromGenerateCompanionKey(owner),
      ).symbol

   private fun generateRowMapperClass(owner: FirClassSymbol<*>): FirClassLikeSymbol<*> {
      val rowMapperType = rowMapperType(owner.constructType())

      return createNestedClass(
         owner = owner,
         name = ROW_MAPPER_CLASS_NAME,
         classKind = ClassKind.OBJECT,
         key = MikromGenerateRowMapperClassKey(owner, rowMapperType),
      ) {
         superType(rowMapperType)
         visibility = owner.visibility
      }.symbol
   }

   override fun getCallableNamesForClass(
      classSymbol: FirClassSymbol<*>,
      context: MemberGenerationContext,
   ): Set<Name> {
      val key = (classSymbol.origin as? FirDeclarationOrigin.Plugin)?.key

      return when (key) {
         is MikromGenerateRowMapperClassKey -> setOf(SpecialNames.INIT, MAP_ROW_FUN_NAME)
         is MikromGenerateCompanionKey -> setOf(SpecialNames.INIT, ROW_MAPPER_FUN_NAME)
         else -> emptySet()
      }
   }

   override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
      val owner = context.owner
      val key = (owner.origin as? FirDeclarationOrigin.Plugin)?.key

      return when (key) {
         is MikromGenerateRowMapperClassKey -> {
            listOf(
               createConstructor(
                  owner = owner,
                  key = MikromGenerateRowMapperKey,
                  isPrimary = true,
               ) {
                  visibility = Visibilities.Private
               }.symbol,
            )
         }

         is MikromGenerateCompanionKey -> {
            listOf(
               createConstructor(
                  owner = owner,
                  key = MikromGenerateCompanionKey(key.ownerClassSymbol),
                  isPrimary = true,
               ) {
                  visibility = Visibilities.Private
               }.symbol,
            )
         }

         else -> {
            emptyList()
         }
      }
   }

   override fun generateFunctions(
      callableId: CallableId,
      context: MemberGenerationContext?,
   ): List<FirNamedFunctionSymbol> {
      val owner = context?.owner ?: return emptyList()
      val key = (owner.origin as? FirDeclarationOrigin.Plugin)?.key

      return when {
         // mapRow() on $RowMapper class
         callableId.callableName == MAP_ROW_FUN_NAME && key is MikromGenerateRowMapperClassKey -> {
            listOf(
               createMemberFunction(
                  owner = owner,
                  key = MikromGenerateRowMapperKey,
                  name = callableId.callableName,
                  returnType = key.ownerClassSymbol.constructType(),
               ) {
                  valueParameter(Name.identifier("row"), rowType)
                  valueParameter(Name.identifier("mikrom"), mikromType)
               }.symbol,
            )
         }

         // rowMapper() on companion object
         callableId.callableName == ROW_MAPPER_FUN_NAME && key is MikromGenerateCompanionKey -> {
            val rowMapperType = rowMapperType(key.ownerClassSymbol.constructType())
            listOf(
               createMemberFunction(
                  owner = owner,
                  key = MikromGenerateRowMapperAccessorKey(key.ownerClassSymbol, rowMapperType),
                  name = callableId.callableName,
                  returnType = rowMapperType,
               ).symbol,
            )
         }

         else -> {
            emptyList()
         }
      }
   }

   public companion object {
      private val ROW_MAPPER_CLASS_NAME = Name.identifier("\$RowMapper")
      private val MAP_ROW_FUN_NAME = Name.identifier("mapRow")
      private val ROW_MAPPER_FUN_NAME = Name.identifier("rowMapper")

      private val ROW_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         annotated(FqName("io.github.kantis.mikrom.generator.RowMapped"))
      }

      private val HAS_ROW_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         hasAnnotated(FqName("io.github.kantis.mikrom.generator.RowMapped"))
      }
   }
}
