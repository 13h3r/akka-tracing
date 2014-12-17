import sbt._
import Keys._
import scoverage.ScoverageSbtPlugin

object AkkaTracingBuild extends Build {

  lazy val commonSettings =
    Defaults.defaultSettings ++
    compilationSettings ++
    testSettings ++
    Seq (
      organization := "com.github.levkhomich",
      version := "0.4-SNAPSHOT",
      scalaVersion := "2.11.4",
      homepage := Some(url("https://github.com/levkhomich/akka-tracing")),
      licenses := Seq("Apache Public License 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
    )

  lazy val compilationSettings =
    Seq(
      scalacOptions in GlobalScope ++= Seq(
        "-encoding", "utf8",
        "-deprecation",
        "-unchecked",
        "-feature",
        "-language:_",
        "-Xcheckinit",
        "-Xlint",
        "-Xlog-reflective-calls"
      )
    )

  lazy val testSettings =
    ScoverageSbtPlugin.instrumentSettings ++
    CoverallsPlugin.coverallsSettings ++
    Seq(
      parallelExecution in Test := false,
      scalacOptions in Test ++= Seq("-Yrangepos"),
      testOptions in Test := Seq(Tests.Filter(!"true".equals(System.getenv("CI")) || !_.contains("Performance")))
    )

  lazy val publicationSettings = Seq(
    publishMavenStyle := true,
    crossScalaVersions := Seq("2.10.4", "2.11.4"),
    javacOptions ++= Seq(
      "-source", "1.6",
      "-target", "1.6"
    ),
    scalacOptions ++= Seq(
      "-target:jvm-1.6"
    ),
    publishTo <<= version { v =>
      val nexus = "https://oss.sonatype.org/"
      if (v.trim.endsWith("SNAPSHOT"))
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    publishArtifact in Test := false,
    pomIncludeRepository := { _ => false },
    pomExtra :=
      <inceptionYear>2014</inceptionYear>
      <scm>
        <url>https://github.com/levkhomich/akka-tracing.git</url>
        <connection>scm:git:git@github.com:levkhomich/akka-tracing.git</connection>
        <tag>HEAD</tag>
      </scm>
      <issueManagement>
        <system>github</system>
        <url>https://github.com/levkhomich/akka-tracing/issues</url>
      </issueManagement>
      <developers>
        <developer>
          <name>Lev Khomich</name>
          <email>levkhomich@gmail.com</email>
          <url>http://github.com/levkhomich</url>
        </developer>
      </developers>
  )

  lazy val root = Project(
    id = "akka-tracing-root",
    base = file("."),
    settings =
      commonSettings ++
      publicationSettings ++
      Seq(
        publish := (),
        publishLocal := (),
        // workaround for sbt-pgp
        packagedArtifacts := Map.empty
      )
  ).aggregate(core, spray, play)

  lazy val core = Project(
    id = "akka-tracing-core",
    base = file("core"),
    settings =
      commonSettings ++
      publicationSettings ++ Seq(
        name := "Akka Tracing: Core",
        libraryDependencies ++=
          Dependencies.thrift ++
          Dependencies.akka ++
          Dependencies.test,
        sourceGenerators in Compile += Def.task {
          val srcManaged = (sourceManaged in Compile).value
          val thriftSrc = (sourceDirectory in Compile).value / "thrift" / "zipkin.thrift"
          s"${baseDirectory.value}/project/gen_thrift.sh $thriftSrc $srcManaged".!
          (srcManaged / "com" / "github" / "levkhomich" / "akka" / "tracing" / "thrift").listFiles().toSeq
        }.taskValue
      )
  )

  lazy val spray = Project(
    id = "akka-tracing-spray",
    base = file("spray"),
    settings =
      commonSettings ++
      publicationSettings ++ Seq(
        name := "Akka Tracing: Spray",
        libraryDependencies ++=
            Dependencies.spray ++
            Dependencies.test
      )
  ).dependsOn(core)

  lazy val play = Project(
    id = "akka-tracing-play",
    base = file("play"),
    settings =
      commonSettings ++
      publicationSettings ++ Seq(
        name := "Akka Tracing: Play",
        libraryDependencies ++=
            Dependencies.play ++
            Dependencies.test,
        resolvers += "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/"
      )
  ).dependsOn(core)
}

object Dependencies {

  object Compile {
    val akkaActor    = "com.typesafe.akka" %% "akka-actor"          % "2.3.7"
    val sprayRouting = "io.spray"          %% "spray-routing"       % "1.3.2"
    val play         = "com.typesafe.play" %% "play"                % "2.3.7"
    val config       = "com.typesafe"      %  "config"              % "1.2.1"
    val libThrift    = "org.apache.thrift" %  "libthrift"           % "0.9.2"
    val slf4jLog4j12 = "org.slf4j"         %  "slf4j-log4j12"       % "1.7.7"
  }

  object Test {
    val specs        = "org.specs2"        %% "specs2"              % "2.3.11" % "test"
    val finagle      = "com.twitter"       %% "finagle-core"        % "6.24.0" % "test"
    val sprayCan     = "io.spray"          %% "spray-can"           % "1.3.2"  % "test"
  }

  val akka = Seq(Compile.akkaActor, Compile.config)
  val spray = Seq(Compile.sprayRouting)
  val play = Seq(Compile.play)
  val thrift = Seq(Compile.libThrift, Compile.slf4jLog4j12)
  val test = Seq(Test.specs, Test.finagle, Test.sprayCan)
}
