package io.github.kantis.mikrom

public fun interface RowMapper<out T> {
   public fun mapRow(
      row: Row,
      mikrom: Mikrom,
   ): T
}
