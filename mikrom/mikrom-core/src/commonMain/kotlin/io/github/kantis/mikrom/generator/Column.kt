package io.github.kantis.mikrom.generator

/**
 * Specifies a custom column name for the annotated parameter when mapping database rows.
 * If not present, the parameter name is used as the column name.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
public annotation class Column(val name: String)
