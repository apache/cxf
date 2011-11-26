Spring HTTP demo
================

This example will lead you through creating your first service with
Spring. You'll learn how to:

    * Writing a simple JAX-WS "code-first" service
    * Set up the HTTP servlet transport
    * Use CXF's Spring beans


Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn clean install   (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

