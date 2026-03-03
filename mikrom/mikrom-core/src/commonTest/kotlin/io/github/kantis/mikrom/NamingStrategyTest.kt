package io.github.kantis.mikrom

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class NamingStrategyTest : FunSpec({
   test("AS_IS returns parameter name unchanged") {
      NamingStrategy.AS_IS.toColumnName("invoiceId") shouldBe "invoiceId"
      NamingStrategy.AS_IS.toColumnName("name") shouldBe "name"
      NamingStrategy.AS_IS.toColumnName("already_snake") shouldBe "already_snake"
   }

   test("SNAKE_CASE converts camelCase to snake_case") {
      NamingStrategy.SNAKE_CASE.toColumnName("invoiceId") shouldBe "invoice_id"
      NamingStrategy.SNAKE_CASE.toColumnName("customerName") shouldBe "customer_name"
      NamingStrategy.SNAKE_CASE.toColumnName("name") shouldBe "name"
      NamingStrategy.SNAKE_CASE.toColumnName("htmlParser") shouldBe "html_parser"
      NamingStrategy.SNAKE_CASE.toColumnName("myURLParser") shouldBe "my_u_r_l_parser"
   }

   test("UPPER_SNAKE_CASE converts camelCase to UPPER_SNAKE_CASE") {
      NamingStrategy.UPPER_SNAKE_CASE.toColumnName("invoiceId") shouldBe "INVOICE_ID"
      NamingStrategy.UPPER_SNAKE_CASE.toColumnName("name") shouldBe "NAME"
      NamingStrategy.UPPER_SNAKE_CASE.toColumnName("customerName") shouldBe "CUSTOMER_NAME"
   }
})
