# STARTS (*STA*tic *R*egression *T*est *S*election) Overview

[![Build Status](https://travis-ci.org/TestingResearchIllinois/starts.svg?branch=master)](https://travis-ci.org/TestingResearchIllinois/starts)
[![Build status](https://ci.appveyor.com/api/projects/status/giplqg2f4sylogop?svg=true)](https://ci.appveyor.com/project/august782/starts)
[![Build Status](https://github.com/TestingResearchIllinois/starts/actions/workflows/maven.yml/badge.svg)](https://github.com/TestingResearchIllinois/starts/actions)
[![Coverage](.github/badges/jacoco.svg)](https://github.com/TestingResearchIllinois/starts/actions/workflows/coverage.yml)

STARTS is a static class-level regression test selection tool
for Maven-based Java programs.

## Prerequisites

1. Java 1.8
2. Maven 3.2.5 and above
3. Maven Surefire 2.14 and above
4. Operating System: Linux or OSX

## Integrating STARTS Plugin from Maven Central

Change the pom.xml to add the configuration for the STARTS plugin:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>edu.illinois</groupId>
      <artifactId>starts-maven-plugin</artifactId>
      <version>1.3</version>
    </plugin>
  </plugins>
</build>
```

## Building STARTS from source

Build from source to use the latest development version, which supports Java 8-15, on Linux, OSX, or Windows.

1. `git clone https://github.com/TestingResearchIllinois/starts`
2. `cd starts`
3. `mvn install`

Then, change the pom.xml to add the configuration for the latest development version of the STARTS plugin:

```xml
<build>
  <plugins>
    <plugin>
      <groupId>edu.illinois</groupId>
      <artifactId>starts-maven-plugin</artifactId>
      <version>1.4-SNAPSHOT</version>
    </plugin>
  </plugins>
</build>
```

## Using the STARTS Maven Plugin

### Available Options

1. To see all the goals that STARTS provides, run `mvn starts:help`
2. To see the details for any of the goals, run `mvn starts:help -Ddetail=true -Dgoal=<goal>`;
 replace `<goal>` with the goal of interest.

### Major Functionality

1. To see the **types** that changed since the last time STARTS was run:
`mvn starts:diff`

2. To see the **types** that may be impacted by changes since the last
time STARTS was run: `mvn starts:impacted`

3. To see the **tests** that are affected by the most recent changes:
`mvn starts:select`

4. To perform RTS using STARTS (i.e., select tests and run the
selected tests): `mvn starts:starts`

5. To remove all artifacts that STARTS stores between versions
(i.e. in the `.starts` directories): `mvn starts:clean`

__NOTE:__ By default, commands (1) - (3) *will not* update the
checksums of files in the latest version, while the command in (4)
*will* update the checksums of the files. Each command has a
`update**Checksums` option that can be used to change the default
behavior. For example, to update the checksums while checking the
diff, run `mvn starts:diff -DupdateDiffChecksums=true`.

## Papers on STARTS

Below is a list of research papers that describe some aspects of
STARTS:

* [STARTS: STAtic Regression Test Selection](http://mir.cs.illinois.edu/legunsen/pubs/LegunsenETALSTARTSDemo.pdf)
  ```
  Owolabi Legunsen, August Shi, Darko Marinov
  32nd IEEE/ACM International Conference On Automated Software Engineering, Tool Demonstrations Track
  (ASE Demo 2017), pages 949-954, Urbana-Champaign, IL, October-November 2017
  ```
* [An Extensive Study of Static Regression Test Selection in Modern Software Evolution](http://mir.cs.illinois.edu/legunsen/pubs/LegunsenETAL16StaticRTSStudy.pdf)
  ```
  Owolabi Legunsen, Farah Hariri, August Shi, Yafeng Lu, Lingming Zhang, Darko Marinov
  24th ACM SIGSOFT International Symposium on the Foundations of Software Engineering
  (FSE 2016), pages 583-594, Seattle, WA, November 2016
  ```
