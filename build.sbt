ThisBuild / organization := "com.guizmaii"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck := true
ThisBuild / scalafmtSbtCheck := true

lazy val projectName = "aecor-study"

val aecor = ((version: String) =>
  Seq(
    "io.aecor" %% "core"                    % version,
    "io.aecor" %% "schedule"                % version,
    "io.aecor" %% "akka-cluster-runtime"    % version,
    "io.aecor" %% "distributed-processing"  % version,
    "io.aecor" %% "boopickle-wire-protocol" % version,
    "io.aecor" %% "test-kit"                % version % Test
  ))("0.18.0")

lazy val root =
  Project(id = projectName, base = file("."))
    .settings(moduleName := "root")
    .settings(noPublishSettings: _*)
    .aggregate(core)
    .dependsOn(core)

lazy val core =
  project
    .settings(moduleName := projectName)
    .settings(addCompilerPlugin("org.scalameta" % "paradise" % "3.0.0-M11" cross CrossVersion.full))
    .settings(libraryDependencies ++= aecor)

/**
  * Copied from Cats
  */
def noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)
