---
---

* Contents
{:toc}

### Overview

POM Manipulation Extension (PME) is a Maven tool to align the versions in your POMs according to some external reference, sort of like a BOM but much more extensive and without the added baggage of a BOM declaration.

It is suppplied as a core library, a Maven extension (in the sense of installing to `lib/ext`, not `pom.xml` `<extensions/>`) and a command line tool.

PME excels in a cleanroom environment where large numbers of pre-existing projects must be rebuilt. To minimize the number of builds necessary, PME supports aligning dependency versions using an external BOM-like reference. However, it can also use a similar POM external reference to align plugin versions, and inject standardized plugin executions into project builds. Because in this scenario you're often rebuilding projects from existing release tags, PME also supports appending a rebuild version suffix, such as `rebuild-1`, where the actual rebuild number is automatically incremented beyond the highest rebuild number detected in the Maven repository.

### Release Notes

For a list of changes please see [here](https://github.com/release-engineering/pom-manipulation-ext/releases)

### Installation

#### Installation as CLI tool.

Obtain the jar from [here](http://central.maven.org/maven2/org/commonjava/maven/ext/pom-manipulation-cli) and then it may be invoked as

    java -jar pom-manipulation-cli-<version>.jar

It supports the following arguments

    -d,--debug                Enable debug
    -D <arg>                  Java Properties
    -f,--file <arg>           POM file
    -h,--help                 Print help
    -l,--log <arg>            Log file to output logging to
    --log-context <arg>       Add log-context ID
    -o,--outputFile <arg>     outputFile to output dependencies to. Only
                              used with '-p' (Print all project
                              dependencies)
    -P,--activeProfiles <arg> Comma separated list of active profiles.
    -p,--printDeps            Print all project dependencies
    --printGAVTC              Print all project dependencies in
                              group:artifact:version:type:classifier with
                              scope information
    -s,--settings <arg>       Optional settings.xml file
    -t,--trace                Enable trace
    -x <arg> <arg>            XPath tester ( file : xpath )

Note that all property arguments are the equivalent to those used when it is used as a Maven extension.

#### Installation as an Extension

Installing PME is as simple as [grabbing the binary](http://central.maven.org/maven2/org/commonjava/maven/ext/pom-manipulation-ext) and copying it to your `${MAVEN_HOME}/lib/ext` directory. Once PME is installed, Maven should output something like the following when run:

	[INFO] Maven-Manipulation-Extension

Uninstalling the extension is equally simple: just delete it from `${MAVEN_HOME}/lib/ext`.

### Disabling the Extension

You can disable PME using the `manipulation.disable` property:

	$ mvn -Dmanipulation.disable=true clean install

If you want to make it more permanent, you could add it to your `settings.xml`:

	<settings>
		<profiles>
			<profile>
				<id>disable-pme</id>
				<properties>
					<manipulation.disable>true</manipulation.disable>
				</properties>
			</profile>
		</profiles>
		<activeProfiles>
			<activeProfile>disable-pme</activeProfile>
		</activeProfiles>
	</settings>

### Extension Marker Files

When the extension runs it writes out two marker files in the execution root `target` directory. Firstly it writes out a marker file `pom-manip-ext-marker.txt`. If this marker file exists PME will not run a second time instead logging:

    Skipping manipulation as previous execution found.

The second file is a json formatted file containing the modified execution root GAV e.g.

    {
        "VersioningState": {
            "executionRootModified": {
                "groupId": "org.groupId",
                "artifactId": "myArtifactId",
                "version": "1.2.0.Final.rebuild-1"
            }
        }
    }


### Logging Summary

PME will output a summary of its changes at the end of the run. As well as reporting dependency and plugin alignment it is also possible to report what _hasn't_ been aligned by setting the property `reportNonAligned=true`.

### Feature Guide

Below are links to more specific information about configuring sets of features in PME:

* [Configuration Files](guide/configuration.html)
* [Project version manipulation](guide/project-version-manip.html)
* [Dependency manipulation](guide/dep-manip.html)
* [Plugin manipulation](guide/plugin-manip.html)
* [Properties, Profiles, Repositories, Reporting, Etc.](guide/misc.html)
* [JSON manipulation](guide/json.html)
* [XML manipulation](guide/xml.html)
* [Groovy Script Injection](guide/groovy.html)
* [Index](guide/property-index.html)
