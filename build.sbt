ThisBuild / organization := "com.guizmaii"
ThisBuild / scalaVersion := "2.12.7"
ThisBuild / scalafmtOnCompile := true
ThisBuild / scalafmtCheck := true
ThisBuild / scalafmtSbtCheck := true

lazy val projectName = "aecor-study"

// ## Libs

lazy val cats              = "org.typelevel" %% "cats-core" % "1.5.0"
lazy val catsTagless       = "org.typelevel" %% "cats-tagless-macros" % "0.2.0"
lazy val catsPar           = "io.chrisdavenport" %% "cats-par" % "0.2.0"
lazy val scalapbRuntime    = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf"
lazy val kindProjector     = compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.9")
lazy val betterMonadicFor  = addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
lazy val scalametaParadise = addCompilerPlugin(("org.scalameta" % "paradise" % "3.0.0-M11").cross(CrossVersion.full))

lazy val aecor = ((version: String) =>
  Seq(
    "io.aecor" %% "core"                    % version,
    "io.aecor" %% "schedule"                % version,
    "io.aecor" %% "akka-cluster-runtime"    % version,
    "io.aecor" %% "distributed-processing"  % version,
    "io.aecor" %% "boopickle-wire-protocol" % version,
    "io.aecor" %% "aecor-postgres-journal"  % "0.3.0",
    "io.aecor" %% "test-kit"                % version % Test
  ))("0.18.0")

lazy val enumeratum =
  Seq(
    "com.beachape" %% "enumeratum"       % "1.5.13",
    "com.beachape" %% "enumeratum-circe" % "1.5.18"
  )

lazy val doobie = ((version: String) =>
  Seq(
    "org.tpolecat" %% "doobie-core"     % version,
    "org.tpolecat" %% "doobie-postgres" % version,
    "org.tpolecat" %% "doobie-hikari"   % version
  ))("0.6.0")

lazy val pureconfig = ((version: String) =>
  Seq(
    "com.github.pureconfig" %% "pureconfig"        % version,
    "com.github.pureconfig" %% "pureconfig-http4s" % version,
  ))("0.10.0")

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
    .settings(scalacOptions += "-Xplugin-require:macroparadise")
    .settings(scalaPbSettings: _*)
    .settings(
      betterMonadicFor,
      scalametaParadise,
      libraryDependencies ++=
        Seq(
          kindProjector,
          cats,
          catsTagless,
          catsPar,
          scalapbRuntime
        ) ++ aecor ++ enumeratum ++ doobie ++ pureconfig
    )

// ## Commons

/**
  * ScalaPB requirement
  */
lazy val scalaPbSettings =
  PB.targets in Compile := Seq(
    scalapb.gen(flatPackage = true) -> (sourceManaged in Compile).value
  )

/**
  * Copied from Cats
  */
lazy val noPublishSettings =
  Seq(
    publish := {},
    publishLocal := {},
    publishArtifact := false
  )
