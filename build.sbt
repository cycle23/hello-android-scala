enablePlugins(AndroidApp)

scalaVersion := "2.11.8"

resolvers += Resolver.jcenterRepo

localAars ++= Seq(
  file("src/main/libs/base.aar"),
  file("src/main/libs/audio.aar"),
  file("src/main/libs/common.aar"),
  file("src/main/libs/commonwidget.aar"),
  file("src/main/libs/controller.aar"),
  file("src/main/libs/panowidget.aar"),
  file("src/main/libs/videowidget.aar")
)

libraryDependencies ++= Seq(
  "com.android.support" % "appcompat-v7" % "25.0.1",
  "com.google.protobuf.nano" % "protobuf-javanano" % "3.1.0",
  "com.google.android.exoplayer" % "exoplayer" % "r1.5.12"
)

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalacOptions ++=Seq("-feature", "-Xexperimental" ,"-language:implicitConversions", "-language:postfixOps", "-target:jvm-1.7")

proguardOptions ++= Seq(
  "-keepnames class com.google.vr.ndk.** { *; }",
  "-keepnames class com.google.vr.sdk.** { *; }",
  "-keepnames class com.google.vr.vrcore.library.api.** { *; }",
  "-keep class com.google.vr.** { native <methods>; }",
  "-keep class com.google.vr.cardboard.annotations.UsedByNative",
  "-keep @com.google.vr.cardboard.annotations.UsedByNative class *",
  "-keepclassmembers class * {@com.google.vr.cardboard.annotations.UsedByNative *;}",
  "-keep class com.google.vr.cardboard.UsedByNative",
  "-keep @com.google.vr.cardboard.UsedByNative class *",
  "-keepclassmembers class * {@com.google.vr.cardboard.UsedByNative *;}",
  "-keep class com.google.protobuf.nano.*"
)