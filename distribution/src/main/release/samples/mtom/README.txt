MTOM Demo for SWA & XOP
=======================

This demo illustrates the use of a SOAP message with an attachment and
XML-binary Optimized Packaging.

Please review the README in the samples directory before continuing.


Building and running the demo using Maven
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    
To remove the code generated from the WSDL file and the .class
files, run mvn clean".


