Hello World Demo using Document/Literal Style and a Dynamic Client
==================================================================

This demo illustrates the use of the CXF dynamic client 
against a standalone server using SOAP 1.1 over HTTP.

Please review the README in the samples directory before continuing.


Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README 
file is located), the pom.xml file is used to build and run the 
demo. 

Using either UNIX or Windows:

  mvn install   (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


