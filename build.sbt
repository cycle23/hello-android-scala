enablePlugins(AndroidApp)

scalaVersion := "2.11.8"

libraryDependencies += "com.android.support" % "appcompat-v7" % "25.0.1"

javacOptions ++= Seq("-source", "1.7", "-target", "1.7")

scalacOptions ++=Seq("-feature", "-Xexperimental" ,"-language:implicitConversions", "-language:postfixOps", "-target:jvm-1.7")
