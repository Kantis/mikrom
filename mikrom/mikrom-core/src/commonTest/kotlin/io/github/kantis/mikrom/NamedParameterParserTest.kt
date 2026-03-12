package io.github.kantis.mikrom

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf

class NamedParameterParserTest : FunSpec({
   test("single named parameter") {
      val parsed = parseNamedParameters("SELECT * FROM users WHERE id = :id")
      parsed.sql shouldBe "SELECT * FROM users WHERE id = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("id")
   }

   test("multiple named parameters") {
      val parsed = parseNamedParameters("SELECT * FROM users WHERE name = :name AND age > :age")
      parsed.sql shouldBe "SELECT * FROM users WHERE name = ? AND age > ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("name", "age")
   }

   test("repeated named parameter") {
      val parsed = parseNamedParameters("SELECT * FROM users WHERE name = :name OR alias = :name")
      parsed.sql shouldBe "SELECT * FROM users WHERE name = ? OR alias = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("name", "name")
   }

   test("PostgreSQL :: cast is not treated as parameter") {
      val parsed = parseNamedParameters("SELECT created_at::date FROM events WHERE id = :id")
      parsed.sql shouldBe "SELECT created_at::date FROM events WHERE id = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("id")
   }

   test("parameters inside single-quoted strings are not replaced") {
      val parsed = parseNamedParameters("SELECT * FROM users WHERE name = ':notParam' AND id = :id")
      parsed.sql shouldBe "SELECT * FROM users WHERE name = ':notParam' AND id = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("id")
   }

   test("parameters inside double-quoted strings are not replaced") {
      val parsed = parseNamedParameters("""SELECT * FROM users WHERE "col:name" = :value""")
      parsed.sql shouldBe """SELECT * FROM users WHERE "col:name" = ?"""
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("value")
   }

   test("parameters inside line comments are not replaced") {
      val parsed = parseNamedParameters(
         """
         SELECT * FROM users -- filter by :name
         WHERE id = :id
         """.trimIndent(),
      )
      parsed.sql shouldBe "SELECT * FROM users -- filter by :name\nWHERE id = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("id")
   }

   test("parameters inside block comments are not replaced") {
      val parsed = parseNamedParameters("SELECT /* :notParam */ * FROM users WHERE id = :id")
      parsed.sql shouldBe "SELECT /* :notParam */ * FROM users WHERE id = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("id")
   }

   test("query with no named parameters passes through unchanged") {
      val sql = "SELECT * FROM users WHERE id = ?"
      val parsed = parseNamedParameters(sql)
      parsed.sql shouldBe sql
      parsed.shouldBeInstanceOf<ParsedQuery.Positional>()
   }

   test("empty query") {
      val parsed = parseNamedParameters("")
      parsed.sql shouldBe ""
      parsed.shouldBeInstanceOf<ParsedQuery.Positional>()
   }

   test("parameter with underscores and digits") {
      val parsed = parseNamedParameters("SELECT * FROM t WHERE col = :my_param_2")
      parsed.sql shouldBe "SELECT * FROM t WHERE col = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("my_param_2")
   }

   test("escaped single quote inside string literal") {
      val parsed = parseNamedParameters("SELECT * FROM t WHERE name = 'O''Brien' AND id = :id")
      parsed.sql shouldBe "SELECT * FROM t WHERE name = 'O''Brien' AND id = ?"
      (parsed as ParsedQuery.Named).parameterNames shouldBe listOf("id")
   }

   test("resolveParams maps values in order") {
      val parsed = parseNamedParameters("INSERT INTO t (a, b) VALUES (:alpha, :beta)") as ParsedQuery.Named
      val values = parsed.resolveParams(mapOf("alpha" to 1, "beta" to "two"))
      values shouldBe listOf(1, "two")
   }

   test("resolveParams handles repeated parameter") {
      val parsed = parseNamedParameters("SELECT * FROM t WHERE a = :x OR b = :x") as ParsedQuery.Named
      val values = parsed.resolveParams(mapOf("x" to 42))
      values shouldBe listOf(42, 42)
   }

   test("resolveParams handles null values") {
      val parsed = parseNamedParameters("INSERT INTO t (a) VALUES (:val)") as ParsedQuery.Named
      val values = parsed.resolveParams(mapOf("val" to null))
      values shouldBe listOf(null)
   }

   test("resolveParams throws on missing parameter") {
      val parsed = parseNamedParameters("SELECT * FROM t WHERE a = :x AND b = :y") as ParsedQuery.Named
      val ex = shouldThrow<IllegalStateException> {
         parsed.resolveParams(mapOf("x" to 1))
      }
      ex.message shouldContain ":y"
   }

   test("resolveParams error lists all missing parameters") {
      val parsed = parseNamedParameters("SELECT * FROM t WHERE a = :x AND b = :y AND c = :z") as ParsedQuery.Named
      val ex = shouldThrow<IllegalStateException> {
         parsed.resolveParams(emptyMap())
      }
      ex.message shouldContain ":x"
      ex.message shouldContain ":y"
      ex.message shouldContain ":z"
   }

   test("colon at end of query is not treated as parameter") {
      val parsed = parseNamedParameters("SELECT * FROM t:")
      parsed.sql shouldBe "SELECT * FROM t:"
      parsed.shouldBeInstanceOf<ParsedQuery.Positional>()
   }

   test("colon followed by digit is not treated as parameter") {
      val parsed = parseNamedParameters("SELECT * FROM t WHERE a = :1invalid")
      parsed.sql shouldBe "SELECT * FROM t WHERE a = :1invalid"
      parsed.shouldBeInstanceOf<ParsedQuery.Positional>()
   }
})
