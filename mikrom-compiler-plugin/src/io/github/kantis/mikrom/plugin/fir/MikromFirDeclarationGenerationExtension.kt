package io.github.kantis.mikrom.plugin.fir

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
import org.jetbrains.kotlin.fir.plugin.createConeType
import org.jetbrains.kotlin.fir.plugin.createConstructor
import org.jetbrains.kotlin.fir.plugin.createMemberFunction
import org.jetbrains.kotlin.fir.plugin.createNestedClass
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
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
   // Supertype which the generated nested RowMapper should implement..
   private fun rowMapperType(typeArgument: ConeTypeProjection) =
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

   override fun generateNestedClassLikeDeclaration(
      owner: FirClassSymbol<*>,
      name: Name,
      context: NestedClassGenerationContext,
   ): FirClassLikeSymbol<*>? {
      if (name != ROW_MAPPER_CLASS_NAME) return null

      val rowMapperType = rowMapperType(owner.constructType())

      val rowMapperImplementation = createNestedClass(
         owner = owner,
         name = ROW_MAPPER_CLASS_NAME,
         classKind = ClassKind.OBJECT,
         key = MikromGenerateRowMapperClassKey(owner, rowMapperType),
      ) {
         superType(rowMapperType)
         visibility = owner.visibility
      }.apply {
//         markAsDeprecatedHidden(session)
      }

      return rowMapperImplementation.symbol
   }

   override fun getNestedClassifiersNames(
      classSymbol: FirClassSymbol<*>,
      context: NestedClassGenerationContext,
   ): Set<Name> {
      val provider = session.predicateBasedProvider
      if (!provider.matches(ROW_MAPPED_PREDICATE, classSymbol))
         return emptySet()

      return setOf(ROW_MAPPER_CLASS_NAME)
   }

   override fun getCallableNamesForClass(
      classSymbol: FirClassSymbol<*>,
      context: MemberGenerationContext,
   ): Set<Name> {
      val key = (classSymbol.origin as? FirDeclarationOrigin.Plugin)?.key
      if (key !is MikromGenerateRowMapperClassKey) return emptySet()

      return buildSet {
         add(SpecialNames.INIT)
         add(MAP_ROW_FUN_NAME)
      }
   }

   override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
      val rowMapperClassSymbol = context.owner
      val key = (rowMapperClassSymbol.origin as? FirDeclarationOrigin.Plugin)?.key
      if (key !is MikromGenerateRowMapperClassKey) return emptyList()

      // TODO: Should probably call super(), to be compliant with a normal Object..
      val constructor = createConstructor(
         owner = rowMapperClassSymbol,
         key = MikromGenerateRowMapperKey,
         isPrimary = true,
      ) {
         visibility = Visibilities.Private
      }

      return listOf(constructor.symbol)
   }

   override fun generateFunctions(
      callableId: CallableId,
      context: MemberGenerationContext?,
   ): List<FirNamedFunctionSymbol> {
      if (callableId.callableName != MAP_ROW_FUN_NAME)
         return emptyList()

      val rowMapperClassSymbol = context?.owner
      val key = (rowMapperClassSymbol?.origin as? FirDeclarationOrigin.Plugin)?.key
      if (key !is MikromGenerateRowMapperClassKey) return emptyList()

      val build = createMemberFunction(
         owner = rowMapperClassSymbol,
         key = MikromGenerateRowMapperKey,
         name = callableId.callableName,
         returnType = key.ownerClassSymbol.constructType(),
      ) {
         valueParameter(Name.identifier("row"), rowType)
         valueParameter(Name.identifier("mikrom"), mikromType)
      }

      return listOf(build.symbol)
   }

   public companion object {
      private val ROW_MAPPER_CLASS_NAME = Name.identifier("RowMapper")
      private val MAP_ROW_FUN_NAME = Name.identifier("mapRow")

      private val ROW_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         annotated(FqName("io.github.kantis.mikrom.generator.RowMapped"))
      }

      private val HAS_ROW_MAPPED_PREDICATE = DeclarationPredicate.Companion.create {
         hasAnnotated(FqName("io.github.kantis.mikrom.generator.RowMapped"))
      }
   }
}
