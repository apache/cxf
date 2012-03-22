Hello World Demo using Apache Ant for the Build Tool
====================================================

This demo takes builds a very simple "Hello World" 
service and client using Apache Ant instead of Apache 
Maven as the build tool.   Using Apache Ant requires
setting up some macrodefs for the CXF tools and
requires configuring the classpath.  The build.xml
in this sample shows how to do that.


Prerequisite
------------

The Ant build.xml for this sample requires the CXF_HOME
environment variable to be set to the root of the Apache CXF
installation, and you may wish to place the Ant /bin folder
in your system PATH variable so the "ant" command will be 
available from any directory.


Building and running the demo using Ant
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server target automatically builds the demo.

Using either UNIX or Windows:

To build and run the server, run:
  ant server
    
To build and run the client, run:
  ant client


To remove the code generated from the WSDL file and the .class
files, run:
  ant clean




   
