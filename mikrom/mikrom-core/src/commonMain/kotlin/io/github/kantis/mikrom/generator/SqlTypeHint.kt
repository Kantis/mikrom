package io.github.kantis.mikrom.generator

public enum class SqlType { VARCHAR, NVARCHAR }

@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
public annotation class SqlTypeHint(val type: SqlType)
