Sniffy
============

[![Stories in Ready](https://badge.waffle.io/sniffy/sniffy.png?label=ready&title=Ready)](https://waffle.io/sniffy/sniffy)
[![Quality Gate](https://sonarqube.com/api/badges/gate?key=io.sniffy:sniffy-parent)](https://sonarqube.com/dashboard?id=io.sniffy%3Asniffy-parent)
[![Join the chat at https://gitter.im/sniffy/sniffy](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/sniffy/sniffy?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![Codacy Badge](https://api.codacy.com/project/badge/grade/ec48f442755f4df5b62bf3bcba3a2246)](https://www.codacy.com/app/sniffy/sniffy)
[![CI Status](https://travis-ci.org/sniffy/sniffy.svg?branch=master)](https://travis-ci.org/sniffy/sniffy)
[![Coverage Status](https://coveralls.io/repos/sniffy/sniffy/badge.png?branch=master)](https://coveralls.io/r/sniffy/sniffy?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.sniffy/sniffy/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/io.sniffy/sniffy)
[![Download](https://api.bintray.com/packages/sniffy/sniffy/sniffy/images/download.svg) ](https://bintray.com/sniffy/sniffy/sniffy/_latestVersion)
[![License](http://img.shields.io/:license-mit-blue.svg?style=flat)](http://badges.mit-license.org)

Sniffy is a Java profiler which shows the results directly in your browser.
It also brings profiling to your unit (or rather component) tests and allows you to disable certain outgoing connections for fault-tolerance testing.


![RecordedDemo](http://sniffy.io/demo.gif)

Live Demo - [http://demo.sniffy.io/](http://demo.sniffy.io/owners.html?lastName=)
Documentation - [http://sniffy.io/docs/latest/](http://sniffy.io/docs/latest/)

Support
============
Ask questions on stackoverflow with tag [sniffy](http://stackoverflow.com/questions/tagged/sniffy)

Building
============
JDBC sniffer is built using JDK8+ and Maven 3.2+ - just checkout the project and type `mvn install`
JDK8 is required only for building the project - once it's built, you can use Sniffy with any JRE 1.6+

UI part of Sniffy is maintained in a separate repository [sniffy-ui](https://github.com/sniffy/sniffy-ui)

Contribute
============
You are most welcome to contribute to Sniffy!

Read the [Contribution guidelines](https://github.com/sniffy/sniffy/blob/master/CONTRIBUTING.md)
