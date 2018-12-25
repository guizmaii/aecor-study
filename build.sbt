ThisBuild / organization := "com.guizmaii"
ThisBuild / scalaVersion := "2.12.8"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck := true
ThisBuild / scalafmtSbtCheck := true

lazy val projectName = "aecor-study"

// ## Libs

val cats             = "org.typelevel" %% "cats-core" % "1.5.0"
val kindProjector    = compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
val betterMonadicFor = addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")

lazy val aecor = ((version: String) =>
  Seq(
    "io.aecor" %% "core"                    % version,
    "io.aecor" %% "schedule"                % version,
    "io.aecor" %% "akka-cluster-runtime"    % version,
    "io.aecor" %% "distributed-processing"  % version,
    "io.aecor" %% "boopickle-wire-protocol" % version,
    "io.aecor" %% "test-kit"                % version % Test
  ))("0.18.0")

lazy val circe =
  Seq(
    "com.beachape" %% "enumeratum"       % "1.5.13",
    "com.beachape" %% "enumeratum-circe" % "1.5.18"
  )

// ## Projects

lazy val root =
  Project(id = projectName, base = file("."))
    .settings(moduleName := "root")
    .settings(noPublishSettings: _*)
    .aggregate(core)
    .dependsOn(core)

lazy val core =
  project
    .settings(moduleName := projectName)
    .settings(
      betterMonadicFor,
      libraryDependencies ++=
        Seq(
          kindProjector,
          cats
        ) ++ aecor ++ circe
    )

// ## Commons

/**
  * Copied from Cats
  */
def noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)
