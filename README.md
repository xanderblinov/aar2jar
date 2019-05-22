# aar2jar
Gradle plugin which will help you to add Aar dependencies into your java|kotlin modules


How to use: add following lines in your module's `build.gradle` file
```
plugins {
	id 'kotlin'
	id 'com.stepango.aar2jar' version “0.4” // <- this one
}


dependencies {
	implementation Libs.kotlin
	compileOnlyAar "com.android.support:support-annotations:28.0.0" // <- Use any AAR dependencies
}
```
Make sure you have this dependency in any of your other modules `build.gradle`

```
implementation "com.android.support:support-annotations:28.0.0"
//or 
runtimeOnly "com.android.support:support-annotations:28.0.0"
```
