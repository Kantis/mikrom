package io.github.kantis.mikrom.plugin.ir

import io.github.kantis.mikrom.plugin.MikromGenerateCompanionKey
import io.github.kantis.mikrom.plugin.MikromGenerateParameterMapperAccessorKey
import io.github.kantis.mikrom.plugin.MikromGenerateParameterMapperKey
import io.github.kantis.mikrom.plugin.MikromGenerateRowMapperAccessorKey
import io.github.kantis.mikrom.plugin.MikromGenerateRowMapperKey
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.collections.forEachIndexed

internal class MikromIrVisitor(
   private val pluginContext: IrPluginContext,
) : IrVisitorVoid() {
   private companion object {
      private val ROW_MAPPER_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(MikromGenerateRowMapperKey)
      private val PARAMETER_MAPPER_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(MikromGenerateParameterMapperKey)
      private val COMPANION_ORIGIN_PREFIX = "MikromGenerateCompanionKey"
      private val ACCESSOR_ORIGIN_PREFIX = "MikromGenerateRowMapperAccessorKey"
      private val PARAMETER_MAPPER_ACCESSOR_ORIGIN_PREFIX = "MikromGenerateParameterMapperAccessorKey"

      private val ROW_CLASS_ID = ClassId(FqName("io.github.kantis.mikrom"), Name.identifier("Row"))
      private val ROW_MAPPER_NESTED_NAME = Name.identifier("\$RowMapper")
      private val PARAMETER_MAPPER_NESTED_NAME = Name.identifier("\$ParameterMapper")

      private val MIKROM_CLASS_ID = ClassId(FqName("io.github.kantis.mikrom"), Name.identifier("Mikrom"))
      private val NAMING_STRATEGY_CLASS_ID = ClassId(FqName("io.github.kantis.mikrom.generator"), Name.identifier("NamingStrategy"))
      private val COLUMN_CLASS_ID = ClassId(FqName("io.github.kantis.mikrom.generator"), Name.identifier("Column"))

      private val ILLEGAL_STATE_EXCEPTION_FQ_NAME =
         FqName("kotlin.IllegalStateException")

      private val ILLEGAL_STATE_EXCEPTION_CLASS_ID =
         ClassId.topLevel(ILLEGAL_STATE_EXCEPTION_FQ_NAME)

      private val PAIR_CLASS_ID = ClassId(FqName("kotlin"), Name.identifier("Pair"))
      private val TO_FQ_NAME = FqName("kotlin.to")
   }

   private val namingStrategyGetter by lazy {
      val mikromClass = pluginContext.referenceClass(MIKROM_CLASS_ID)!!
      mikromClass.owner.declarations
         .filterIsInstance<IrProperty>()
         .single { it.name.asString() == "namingStrategy" }
         .getter!!.symbol
   }

   private val toColumnNameFunction by lazy {
      val nsClass = pluginContext.referenceClass(NAMING_STRATEGY_CLASS_ID)!!
      nsClass.functions.single {
         it.owner.name.asString() == "toColumnName"
      }
   }

   private val nullableStringType = pluginContext.irBuiltIns.stringType.makeNullable()
   private val illegalStateExceptionConstructor =
      pluginContext.referenceConstructors(ILLEGAL_STATE_EXCEPTION_CLASS_ID)
         .single { constructor ->
            val parameter = constructor.owner.parameters.singleOrNull()
               ?: return@single false
            parameter.type == nullableStringType
         }

   private fun IrBuilderWithScope.irUninitializedProperty(property: IrDeclarationWithName): IrConstructorCall =
      irCall(illegalStateExceptionConstructor).apply {
         arguments[0] = irString("Uninitialized property '${property.name}'.")
      }

   override fun visitElement(element: IrElement) {
      when (element) {
         is IrDeclaration,
         is IrFile,
         is IrModuleFragment,
         -> element.acceptChildrenVoid(this)

         else -> Unit
      }
   }

   private fun IrDeclaration.isGeneratedByMikrom(): Boolean {
      val origin = origin
      if (origin == ROW_MAPPER_ORIGIN) return true
      if (origin == PARAMETER_MAPPER_ORIGIN) return true
      if (origin !is IrDeclarationOrigin.GeneratedByPlugin) return false
      val key = origin.pluginKey
      return key is MikromGenerateCompanionKey ||
         key is MikromGenerateRowMapperAccessorKey ||
         key is MikromGenerateParameterMapperAccessorKey
   }

   override fun visitConstructor(declaration: IrConstructor) {
      if (declaration.isGeneratedByMikrom() && declaration.body == null) {
         declaration.body = generateDefaultConstructor(declaration)
      }
   }

   private fun generateDefaultConstructor(declaration: IrConstructor): IrBody? {
      val parentClass = declaration.parent as? IrClass ?: return null
      val anyConstructor = pluginContext.irBuiltIns.anyClass.owner.primaryConstructor
         ?: return null

      val irBuilder = DeclarationIrBuilder(pluginContext, declaration.symbol)
      return irBuilder.irBlockBody {
         +irDelegatingConstructorCall(anyConstructor)
         +IrInstanceInitializerCallImpl(
            startOffset,
            endOffset,
            classSymbol = parentClass.symbol,
            type = context.irBuiltIns.unitType,
         )
      }
   }

   override fun visitSimpleFunction(declaration: IrSimpleFunction) {
      if (declaration.body != null) return
      if (!declaration.isGeneratedByMikrom()) return

      val origin = declaration.origin as? IrDeclarationOrigin.GeneratedByPlugin ?: return
      val key = origin.pluginKey

      when {
         key is MikromGenerateRowMapperAccessorKey -> {
            declaration.body = generateRowMapperAccessor(declaration)
         }

         key is MikromGenerateParameterMapperAccessorKey -> {
            declaration.body = generateParameterMapperAccessor(declaration)
         }

         key is MikromGenerateRowMapperKey -> {
            declaration.body = generateMapRowFunction(declaration)
         }

         key is MikromGenerateParameterMapperKey -> {
            declaration.body = generateMapParametersFunction(declaration)
         }
      }
   }

   private fun generateRowMapperAccessor(function: IrSimpleFunction): IrBody? {
      val companionClass = function.parentAsClass
      val ownerClass = companionClass.parentAsClass

      // Find the $RowMapper nested object in the owner class
      val rowMapperObject = ownerClass.declarations
         .filterIsInstance<IrClass>()
         .singleOrNull { it.name == ROW_MAPPER_NESTED_NAME }
         ?: return null

      val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)
      return irBuilder.irBlockBody {
         +irReturn(irGetObject(rowMapperObject.symbol))
      }
   }

   private fun generateParameterMapperAccessor(function: IrSimpleFunction): IrBody? {
      val companionClass = function.parentAsClass
      val ownerClass = companionClass.parentAsClass

      val parameterMapperObject = ownerClass.declarations
         .filterIsInstance<IrClass>()
         .singleOrNull { it.name == PARAMETER_MAPPER_NESTED_NAME }
         ?: return null

      val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)
      return irBuilder.irBlockBody {
         +irReturn(irGetObject(parameterMapperObject.symbol))
      }
   }

   private fun generateMapRowFunction(function: IrSimpleFunction): IrBody? {
      val mappedType = function.returnType.classifierOrNull?.owner as? IrClass ?: return null
      val primaryConstructor = mappedType.primaryConstructor ?: return null
      val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)

      return irBuilder.irBlockBody {
         // parameters[0] = dispatch receiver (RowMapper this)
         // parameters[1] = row: Row
         // parameters[2] = mikrom: Mikrom
         val row = function.parameters[1]
         val mikrom = function.parameters[2]
         val arguments = generateConstructorArguments(primaryConstructor.parameters, row, mikrom)

         val constructorCall = irCall(primaryConstructor).apply {
            arguments.forEachIndexed { index, variable ->
               this.arguments[index] = irGet(variable)
            }
         }

         +irReturn(constructorCall)
      }
   }

   private fun generateMapParametersFunction(function: IrSimpleFunction): IrBody? {
      // parameters[0] = dispatch receiver ($ParameterMapper this)
      // parameters[1] = value: T
      val valueParam = function.parameters[1]
      val mappedType = valueParam.type.classifierOrNull?.owner as? IrClass ?: return null
      val primaryConstructor = mappedType.primaryConstructor ?: return null
      val irBuilder = DeclarationIrBuilder(pluginContext, function.symbol)

      // Find the `to` extension function: fun <A, B> A.to(that: B): Pair<A, B>
      val toFunction = pluginContext.referenceFunctions(
         org.jetbrains.kotlin.name.CallableId(FqName("kotlin"), Name.identifier("to")),
      ).single()

      // Find mapOf(vararg pairs: Pair<K, V>): Map<K, V>
      val mapOfFunction = pluginContext.referenceFunctions(
         org.jetbrains.kotlin.name.CallableId(FqName("kotlin.collections"), Name.identifier("mapOf")),
      ).single { ref ->
         val fn = ref.owner
         fn.typeParameters.size == 2 && fn.parameters.any { it.varargElementType != null }
      }

      val pairClass = pluginContext.referenceClass(PAIR_CLASS_ID)!!
      val stringType = pluginContext.irBuiltIns.stringType
      val anyNType = pluginContext.irBuiltIns.anyNType
      val pairType = pairClass.typeWith(stringType, anyNType)
      val arrayOfPairType = pluginContext.irBuiltIns.arrayClass.typeWith(pairType)

      return irBuilder.irBlockBody {
         // Build pairs: "propName" to value.propName for each constructor parameter
         val pairExpressions = primaryConstructor.parameters.map { ctorParam ->
            val propertyGetter = mappedType.properties
               .single { it.name == ctorParam.name }
               .getter!!

            var valueExpr: IrExpression = irCall(propertyGetter).apply {
               dispatchReceiver = irGet(valueParam)
            }

            // Unwrap value classes to their underlying value
            val paramClass = ctorParam.type.classifierOrNull?.owner as? IrClass
            if (paramClass?.isValue == true) {
               val underlyingGetter = paramClass.properties.single().getter!!
               valueExpr = irCall(underlyingGetter).apply {
                  dispatchReceiver = valueExpr
               }
            }

            irCall(toFunction).apply {
               typeArguments[0] = stringType
               typeArguments[1] = anyNType
               // `to` is an extension function: fun <A, B> A.to(that: B): Pair<A, B>
               // parameters[0] = extension receiver (A), parameters[1] = that (B)
               arguments[0] = irString(ctorParam.name.asString())
               arguments[1] = valueExpr
            }
         }

         // Build: mapOf("name" to value.name, "age" to value.age)
         val mapOfCall = irCall(mapOfFunction).apply {
            typeArguments[0] = stringType
            typeArguments[1] = anyNType

            // Find the vararg parameter index
            val varargParamIndex = mapOfFunction.owner.parameters.indexOfFirst { it.varargElementType != null }
            arguments[varargParamIndex] = IrVarargImpl(
               startOffset,
               endOffset,
               type = arrayOfPairType,
               varargElementType = pairType,
               elements = pairExpressions,
            )
         }

         +irReturn(mapOfCall)
      }
   }

   private fun IrBlockBodyBuilder.generateConstructorArguments(
      constructorParameters: List<IrValueParameter>,
      row: IrValueParameter,
      mikrom: IrValueParameter,
   ): List<IrVariable> {
      val variables = mutableListOf<IrVariable>()

      val rowClass = pluginContext.referenceClass(ROW_CLASS_ID)!!
      // Row.get and Row.getOrNull now have 4 parameters:
      // [0] = dispatch receiver (Row this)
      // [1] = context parameter (Mikrom)
      // [2] = column (String)
      // [3] = clazz (KClass)
      val getFunction = rowClass.functions.single {
         it.owner.name == Name.identifier("get") && it.owner.parameters.size == 4
      }
      val getOrNullFunction = rowClass.functions.single {
         it.owner.name == Name.identifier("getOrNull") && it.owner.parameters.size == 4
      }
      val kClassSymbol = pluginContext.referenceClass(
         ClassId.topLevel(FqName("kotlin.reflect.KClass")),
      )!!

      for (valueParameter in constructorParameters) {
         val isNullable = valueParameter.type.isNullable()
         val fn = if (isNullable) getOrNullFunction else getFunction
         val baseType = valueParameter.type.makeNotNull()

         variables += irTemporary(
            nameHint = valueParameter.name.asString(),
            value = irBlock {
               val rowRef = irGet(row)
               val mikromRef = irGet(mikrom)
               val keyExpr = columnNameExpression(valueParameter, mikrom)

               val classRef = IrClassReferenceImpl(
                  startOffset,
                  endOffset,
                  type = kClassSymbol.typeWith(baseType),
                  symbol = baseType.classifierOrNull!!,
                  classType = baseType,
               )

               +irCall(fn).apply {
                  dispatchReceiver = rowRef
                  typeArguments[0] = baseType
                  arguments[1] = mikromRef
                  arguments[2] = keyExpr
                  arguments[3] = classRef
               }
            },
         )
      }

      return variables
   }

   private fun IrBuilderWithScope.columnNameExpression(
      valueParameter: IrValueParameter,
      mikrom: IrValueParameter,
   ): IrExpression {
      val columnAnnotation = valueParameter.annotations.firstOrNull {
         it.type.classifierOrNull == pluginContext.referenceClass(COLUMN_CLASS_ID)
      }

      if (columnAnnotation != null) {
         val nameArg = columnAnnotation.arguments[0]
         val explicitName = (nameArg as? org.jetbrains.kotlin.ir.expressions.IrConst)?.value as? String
         if (explicitName != null) return irString(explicitName)
      }

      return irCall(toColumnNameFunction).apply {
         dispatchReceiver = irCall(namingStrategyGetter).apply {
            dispatchReceiver = irGet(mikrom)
         }
         arguments[1] = irString(valueParameter.name.asString())
      }
   }
}
