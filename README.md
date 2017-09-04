# STARTS (*STA*tic *R*egression *T*est *S*election) Overview

[![Build Status](https://travis-ci.org/TestingResearchIllinois/starts.svg?branch=master)](https://travis-ci.org/TestingResearchIllinois/starts)

STARTS is a tool for static class-level regression test selection tool
for Maven-based Java programs.

## Prerequisites

1. Java 1.8 and above
2. Maven 3.2.5 and above
3. Maven Surefire 2.14 and above
4. Operating System: Linux or OSX

## Installing STARTS from source

1. `git clone https://github.com/TestingResearchIllinois/starts`
2. `cd starts`
3. `mvn install`

## Integrating the STARTS Maven Plugin

To integrate STARTS Maven plugin into your project, change the pom.xml
to achieve the following:

1. Add the configuration for STARTS plugin.
2. Add an excludesFile tag to Surefire plugin.

Below is a sketch:

```xml
<project>
  ...
  <build>
    ...
    <plugins>
      ...
      <plugin>
        <groupId>org.apache.maven.plugins</groupId>
        <artifactId>maven-surefire-plugin</artifactId>
        <version>2.19.1</version>
        <configuration>
          ...
          <excludesFile>myExcludes</excludesFile>
        </configuration>
      </plugin>
      ...
      <plugin>
        <groupId>edu.illinois</groupId>
        <artifactId>starts-maven-plugin</artifactId>
        <version>1.0-SNAPSHOT</version>
      </plugin>
    </plugins>
  </build>
</project>
```

The line, `<excludesFile>myExcludes</excludesFile>`, is only needed if
the `pom.xml` file did not already declare an `</excludesFile>`, in
which case the excludes file name _must_ be `myExcludes`, as shown
above. STARTS requires the `<excludesFile>` tag as shown above and
will work even if the `myExcludes` file does not already exist on the
filesystem. See the FAQ Section below for more information.

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
(i.e. in the .starts directories): `mvn starts:clean`

__NOTE:__ By default, commands (1) - (3) *will not* update the
checksums of files in the latest version, while the command in (4)
*will* update the checksums of the files. Each command has a
`update**Checksums` option that can be used to change the default
behavior. For example, to update the checksums while checking the
diff, run `mvn starts:diff -DupdateDiffChecksums=true`.

## FAQ

- **I got the following error after integrating STARTS and running
`mvn test`:** `Failed to load list from file: myExcludes`.

   **Solution:** Once the `pom.xml` file is modified as described
   above, it is assumed that the user is only interested to run STARTS
   goals. Unfortunately, this currently breaks the usual `mvn test`
   invocation which expects the `<excludesFile>` to exist on the
   filesystem. If you must run `mvn test` with STARTS integrated with
   the `pom.xml` file, either run STARTS in a Maven profile, or make
   sure that the file `myExcludes`, which is specified in the Surefire
   `<excludesFile>` tag exists on the filesystem for every Maven
   module in your project.
