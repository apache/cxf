Basic Setup for Building and Running the Demos
==============================================

As described in the installation notes, extract the Apache CXF
binary distribution archive into an installation directory
under the root drive.  This creates the apache-cxf-x.x.x folder,
which includes all of the product subdirectories.

To build and run the demos, you must install the J2SE Development
Kit (JDK) 5.0 or later.

All of the samples (with the exception of the "antbuild" sample,
which shows how to manage a project using Ant as the buildtool)
are built using Apache Maven, version 2.2.x or 3.x.  You can 
build the Mavenized samples all at once by running 
"mvn clean install" from the samples root folder or by running
the same command within individual sample folders.  For running
each sample, follow the READMEs located in each sample's folder.

To be able to run the Maven "mvn" command from any folder, be
sure to add the MAVEN_HOME/bin directory to your system PATH
variable.


Building the Demos in a Servlet Container Using Apache Maven
=====================================================================

"mvn clean install" will generate a WAR file for the servlet-based
examples.  Either the WAR can be manually copied to your servlet
container's war deployment directory (webapps by default with Tomcat)
or the Tomcat Maven Plugin (http://tomcat.apache.org/maven-plugin.html) 
can be used to auto-install the WAR onto Tomcat.


