package io.github.kantis.mikrom.gradle

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

public open class MikromGradleExtension(objects: ObjectFactory) {
   public val compilerPluginVersion: Property<String> =
      objects.property(String::class.java).convention(readBuiltInVersion())
}

private fun readBuiltInVersion(): String {
   val props = java.util.Properties()
   val stream =
      MikromGradleExtension::class.java.classLoader
         .getResourceAsStream("mikrom-gradle-plugin.properties")
         ?: error("Could not find mikrom-gradle-plugin.properties on classpath")
   stream.use { props.load(it) }
   return props.getProperty("version")
      ?: error("mikrom-gradle-plugin.properties does not contain a 'version' property")
}
