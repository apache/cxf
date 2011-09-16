SOAP Headers
============

This demo illustrates Apache CXF's support for SOAP headers.  In the
WSDL file, the SOAP header is included as an additional part within
the message and binding definitions.  Using this approach to defining a
SOAP header, Apache CXF treats the header content as another parameter
to the operation.  Consequently, the header content is simply
manipulated within the method body.

The client application creates the header content, which is simply
a string value.  The server application views the supplied values
and may set these values into the operation's return values or
out/inout headers.

Please review the README in the samples directory before
continuing.

Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:
  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    
To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

  mvn clean



