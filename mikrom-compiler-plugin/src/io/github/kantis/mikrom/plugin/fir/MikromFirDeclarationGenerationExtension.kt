package io.github.kantis.mikrom.plugin.fir

import io.github.kantis.mikrom.plugin.MikromGenerateCompanionKey
import io.github.kantis.mikrom.plugin.MikromGenerateParameterMapperAccessorKey
import io.github.kantis.mikrom.plugin.MikromGenerateParameterMapperClassKey
import io.github.kantis.mikrom.plugin.MikromGenerateParameterMapperKey
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
import org.jetbrains.kotlin.name.StandardClassIds

public class MikromFirDeclarationGenerationExtension(
   session: FirSession,
) : FirDeclarationGenerationExtension(session) {
   private fun rowMapperType(typeArgument: ConeTypeProjection): ConeClassLikeType =
      ClassId(
         FqName("io.github.kantis.mikrom"),
         Name.identifier("RowMapper"),
      ).createConeType(session, typeArguments = arrayOf(typeArgument))

   private fun parameterMapperType(typeArgument: ConeTypeProjection): ConeClassLikeType =
      ClassId(
         FqName("io.github.kantis.mikrom"),
         Name.identifier("ParameterMapper"),
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

   private val mapType by lazy {
      StandardClassIds.Map.createConeType(
         session,
         typeArguments = arrayOf(
            session.builtinTypes.stringType.coneType,
            session.builtinTypes.nullableAnyType.coneType,
         ),
      )
   }

   override fun FirDeclarationPredicateRegistrar.registerPredicates() {
      register(ROW_MAPPED_PREDICATE)
      register(HAS_ROW_MAPPED_PREDICATE)
      register(PARAMETER_MAPPED_PREDICATE)
      register(HAS_PARAMETER_MAPPED_PREDICATE)
   }

   override fun getNestedClassifiersNames(
      classSymbol: FirClassSymbol<*>,
      context: NestedClassGenerationContext,
   ): Set<Name> {
      val provider = session.predicateBasedProvider
      val isRowMapped = provider.matches(ROW_MAPPED_PREDICATE, classSymbol)
      val isParameterMapped = provider.matches(PARAMETER_MAPPED_PREDICATE, classSymbol)

      if (!isRowMapped && !isParameterMapped) return emptySet()

      val names = mutableSetOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
      if (isRowMapped) names += ROW_MAPPER_CLASS_NAME
      if (isParameterMapped) names += PARAMETER_MAPPER_CLASS_NAME
      return names
   }

   override fun generateNestedClassLikeDeclaration(
      owner: FirClassSymbol<*>,
      name: Name,
      context: NestedClassGenerationContext,
   ): FirClassLikeSymbol<*>? =
      when (name) {
         SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT -> generateCompanion(owner)
         ROW_MAPPER_CLASS_NAME -> generateRowMapperClass(owner)
         PARAMETER_MAPPER_CLASS_NAME -> generateParameterMapperClass(owner)
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

   private fun generateParameterMapperClass(owner: FirClassSymbol<*>): FirClassLikeSymbol<*> {
      val paramMapperType = parameterMapperType(owner.constructType())

      return createNestedClass(
         owner = owner,
         name = PARAMETER_MAPPER_CLASS_NAME,
         classKind = ClassKind.OBJECT,
         key = MikromGenerateParameterMapperClassKey(owner, paramMapperType),
      ) {
         superType(paramMapperType)
         visibility = owner.visibility
      }.symbol
   }

   override fun getCallableNamesForClass(
      classSymbol: FirClassSymbol<*>,
      context: MemberGenerationContext,
   ): Set<Name> {
      val key = (classSymbol.origin as? FirDeclarationOrigin.Plugin)?.key

      return when (key) {
         is MikromGenerateRowMapperClassKey -> {
            setOf(SpecialNames.INIT, MAP_ROW_FUN_NAME)
         }

         is MikromGenerateParameterMapperClassKey -> {
            setOf(SpecialNames.INIT, MAP_PARAMETERS_FUN_NAME)
         }

         is MikromGenerateCompanionKey -> {
            val provider = session.predicateBasedProvider
            val ownerSymbol = key.ownerClassSymbol
            val names = mutableSetOf<Name>(SpecialNames.INIT)
            if (provider.matches(ROW_MAPPED_PREDICATE, ownerSymbol)) {
               names += ROW_MAPPER_FUN_NAME
            }
            if (provider.matches(PARAMETER_MAPPED_PREDICATE, ownerSymbol)) {
               names += PARAMETER_MAPPER_FUN_NAME
            }
            names
         }

         else -> {
            emptySet()
         }
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

         is MikromGenerateParameterMapperClassKey -> {
            listOf(
               createConstructor(
                  owner = owner,
                  key = MikromGenerateParameterMapperKey,
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

         // mapParameters() on $ParameterMapper class
         callableId.callableName == MAP_PARAMETERS_FUN_NAME && key is MikromGenerateParameterMapperClassKey -> {
            listOf(
               createMemberFunction(
                  owner = owner,
                  key = MikromGenerateParameterMapperKey,
                  name = callableId.callableName,
                  returnType = mapType,
               ) {
                  valueParameter(Name.identifier("value"), key.ownerClassSymbol.constructType())
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

         // parameterMapper() on companion object
         callableId.callableName == PARAMETER_MAPPER_FUN_NAME && key is MikromGenerateCompanionKey -> {
            val paramMapperType = parameterMapperType(key.ownerClassSymbol.constructType())
            listOf(
               createMemberFunction(
                  owner = owner,
                  key = MikromGenerateParameterMapperAccessorKey(key.ownerClassSymbol, paramMapperType),
                  name = callableId.callableName,
                  returnType = paramMapperType,
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
      private val PARAMETER_MAPPER_CLASS_NAME = Name.identifier("\$ParameterMapper")
      private val MAP_ROW_FUN_NAME = Name.identifier("mapRow")
      private val MAP_PARAMETERS_FUN_NAME = Name.identifier("mapParameters")
      private val ROW_MAPPER_FUN_NAME = Name.identifier("rowMapper")
      private val PARAMETER_MAPPER_FUN_NAME = Name.identifier("parameterMapper")

      private val ROW_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         annotated(FqName("io.github.kantis.mikrom.generator.RowMapped"))
      }

      private val HAS_ROW_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         hasAnnotated(FqName("io.github.kantis.mikrom.generator.RowMapped"))
      }

      private val PARAMETER_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         annotated(FqName("io.github.kantis.mikrom.generator.ParameterMapped"))
      }

      private val HAS_PARAMETER_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         hasAnnotated(FqName("io.github.kantis.mikrom.generator.ParameterMapped"))
      }
   }
}
