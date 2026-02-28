package io.github.kantis.mikrom.plugin.ir

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
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.makeNotNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isNullable
import org.jetbrains.kotlin.ir.util.primaryConstructor
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
      private val MIKROM_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(MikromGenerateRowMapperKey)

      private val ROW_CLASS_ID = ClassId(FqName("io.github.kantis.mikrom"), Name.identifier("Row"))

      private val ILLEGAL_STATE_EXCEPTION_FQ_NAME =
         FqName("kotlin.IllegalStateException")

      private val ILLEGAL_STATE_EXCEPTION_CLASS_ID =
         ClassId.topLevel(ILLEGAL_STATE_EXCEPTION_FQ_NAME)
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

   override fun visitConstructor(declaration: IrConstructor) {
      if (declaration.origin == MIKROM_ORIGIN && declaration.body == null) {
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
      if (declaration.origin == MIKROM_ORIGIN && declaration.body == null) {
         declaration.body = generateMapRowFunction(declaration)
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
               val keyLiteral = irString(valueParameter.name.asString())

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
                  arguments[2] = keyLiteral
                  arguments[3] = classRef
               }
            },
         )
      }

      return variables
   }
}
