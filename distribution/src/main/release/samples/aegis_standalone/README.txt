Aegis Standalone Demo
=====================

This demo shows how you can use Aegis with no web service at all
as a mapping between XML and Java.

Please review the README in the samples directory before continuing.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn install   (builds the demo)
  mvn -Pwrite  (from one command line window)
  mvn -Pread  (from another command line window)


To remove the generated code, run "mvn clean".

