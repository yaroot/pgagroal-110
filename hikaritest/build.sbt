organization := "com.pasotan"
scalaVersion := "2.13.3"
scalacOptions += "-Ymacro-annotations"

fork in run := true
addCompilerPlugin("org.typelevel"    % "kind-projector"     % "0.11.0" cross CrossVersion.full)
addCompilerPlugin("com.olegpy"      %% "better-monadic-for" % "0.3.1" cross CrossVersion.binary)
addCompilerPlugin("com.github.cb372" % "scala-typed-holes"  % "0.1.5" cross CrossVersion.full)

testFrameworks += new TestFramework("munit.Framework")
scalafmtOnCompile := true
cancelable in Global := true
javaOptions ++= Seq("-XX:+UseG1GC", "-Xmx600m", "-Xms600m", "-XX:SurvivorRatio=8", "-Duser.timezone=UTC")

libraryDependencies ++= Seq(
  "org.tpolecat"  %% "doobie-core"     % "0.9.2",
  "org.tpolecat"  %% "doobie-postgres" % "0.9.2",
  "org.tpolecat"  %% "doobie-hikari"   % "0.9.2",
  "org.typelevel" %% "cats-effect"     % "2.2.0",
  "ch.qos.logback" % "logback-classic" % "1.2.3"
)

enablePlugins(JavaAppPackaging)
