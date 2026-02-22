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
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.makeNullable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import kotlin.collections.forEachIndexed

internal class MikromIrVisitor(
   private val context: IrPluginContext,
) : IrVisitorVoid() {
   private companion object {
      private val MIKROM_ORIGIN = IrDeclarationOrigin.GeneratedByPlugin(MikromGenerateRowMapperKey)

      private val ILLEGAL_STATE_EXCEPTION_FQ_NAME =
         FqName("kotlin.IllegalStateException")

      private val ILLEGAL_STATE_EXCEPTION_CLASS_ID =
         ClassId.topLevel(ILLEGAL_STATE_EXCEPTION_FQ_NAME)
   }

   private val nullableStringType = context.irBuiltIns.stringType.makeNullable()
   private val illegalStateExceptionConstructor =
      context.referenceConstructors(ILLEGAL_STATE_EXCEPTION_CLASS_ID)
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
      val anyConstructor = context.irBuiltIns.anyClass.owner.primaryConstructor
         ?: return null

      val irBuilder = DeclarationIrBuilder(context, declaration.symbol)
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
//      val rowMapperClass = function.parent as? IrClass ?: return null
      val mappedType = function.returnType.classifierOrNull?.owner as? IrClass ?: return null
      val primaryConstructor = mappedType.primaryConstructor ?: return null
      val irBuilder = DeclarationIrBuilder(context, function.symbol)

      return irBuilder.irBlockBody {
         // Map<String, Any>
         val row = function.parameters[1] // first will be the "this" reference..
         val arguments = generateConstructorArguments(primaryConstructor.parameters, row)

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
   ): List<IrVariable> {
      val variables = mutableListOf<IrVariable>()

      for (valueParameter in constructorParameters) {
         variables += irTemporary(
            nameHint = valueParameter.name.asString(),
            value = irBlock {
               val mapRef = irGet(row)
               val keyLiteral = irString(valueParameter.name.asString())

               // Find the Map.get function
               val mapClass = context.irBuiltIns.mapClass
               val getFunction = mapClass.functions.single {
                  it.owner.name == Name.identifier("get")
               }

               +irCall(getFunction).apply {
                  dispatchReceiver = mapRef
                  arguments[1] = keyLiteral
               }
            },
         )
      }

      return variables
   }
}
