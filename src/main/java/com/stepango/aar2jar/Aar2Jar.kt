package com.stepango.aar2jar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.ArtifactTransform
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.internal.artifacts.ArtifactAttributes.ARTIFACT_FORMAT
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

class Aar2Jar : Plugin<Project> {

    override fun apply(project: Project) {
        project.configurations
                .register("compileOnlyAar")
                .configure {
                    isTransitive = false
                    attributes {
                        attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
                    }

                    project.the<JavaPluginConvention>()
                            .sourceSets["main"]
                            .compileClasspath += this@configure
                }

        project.configurations
                .register("implementationAar")
                .configure {
                    isTransitive = false
                    attributes {
                        attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
                    }

                    project.the<JavaPluginConvention>()
                            .sourceSets["main"]
                            .apply {
                                compileClasspath += this@configure
                                runtimeClasspath += this@configure
                            }
                }

        project.dependencies {
            registerTransform {
                from.attribute(ARTIFACT_FORMAT, "aar")
                to.attribute(ARTIFACT_FORMAT, ArtifactTypeDefinition.JAR_TYPE)
                artifactTransform(AarToJarTransform::class.java)
            }
        }
    }
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