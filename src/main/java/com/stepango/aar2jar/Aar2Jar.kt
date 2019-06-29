package com.stepango.aar2jar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.ArtifactTransform
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.internal.artifacts.ArtifactAttributes.ARTIFACT_FORMAT
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class Aar2Jar : Plugin<Project> {

    override fun apply(project: Project) {

        project.pluginManager.apply(IdeaPlugin::class.java)
        project.pluginManager.apply(JavaBasePlugin::class.java)

        project.dependencies {
            registerTransform {
                from.attribute(ARTIFACT_FORMAT, "aar")
                to.attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
                artifactTransform(AarToJarTransform::class.java)
            }
        }

        val compileOnlyAar = project.configurations.register("compileOnlyAar")
        val implementationAar = project.configurations.register("implementationAar")
        val testCompileOnlyAar = project.configurations.register("testCompileOnlyAar")
        val testImplementationAar = project.configurations.register("testImplementationAar")
        val sourceSets = project.the<JavaPluginConvention>().sourceSets

        compileOnlyAar.configure {
            isTransitive = false
            attributes {
                attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            }
            sourceSets.withName("main") {
                compileClasspath += this@configure
            }
        }

        implementationAar.configure {
            isTransitive = false
            attributes {
                attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            }
            sourceSets.withName("main") {
                compileClasspath += this@configure
                runtimeClasspath += this@configure
            }
        }

        testCompileOnlyAar.configure {
            isTransitive = false
            attributes {
                attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            }
            sourceSets.withName("test") {
                compileClasspath += this@configure
            }
        }

        testImplementationAar.configure {
            isTransitive = false
            attributes {
                attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
            }
            sourceSets.withName("test") {
                compileClasspath += this@configure
                runtimeClasspath += this@configure

            }
        }

        project.extensions
                .getByType<IdeaModel>()
                .module
                .scopes["PROVIDED"]
                ?.get("plus")
                ?.apply {
                    add(implementationAar.get())
                    add(compileOnlyAar.get())
                    add(testImplementationAar.get())
                    add(testCompileOnlyAar.get())
                }

    }
}

fun SourceSetContainer.withName(name: String, f: SourceSet.() -> Unit) {
    this[name]?.apply { f(this) } ?: whenObjectAdded { if (this.name == name) f(this) }
}

class AarToJarTransform : ArtifactTransform() {

    override fun transform(input: File): List<File> {
        val file = File(outputDirectory, input.name.replace(".aar", ".jar"))
        ZipFile(input).use { zipFile ->
            zipFile.entries()
                    .toList()
                    .first { it.name == "classes.jar" }
                    .let(zipFile::getInputStream)
                    .use { input ->
                        FileOutputStream(file).use { output ->
                            input.copyTo(output)
                        }
                    }
        }

        return listOf(file)
    }

}