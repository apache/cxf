Aegis Demo
====================

This demo sets up a server with JAX-WS and JAXB, and a client with Aegis.
It is very unlikely that you will want to intentionally build a system
with this arrangement. However, this sample demonstrates using Aegis on
the client with someone else's service.

In a real application, the client would have a completely separate implementation
of a SEI for use with Aegis. Since CXF does not have a 'wsdl2java' tool for Aegis,
this is a manual process. If you do have access to the SEI source code for the
server, you merely need to add .aegis.xml files to specify the information
otherwise present in the @nnotations. Note that the client needs both the WSDL
and the .aegis.xml file to achieve coherence.

Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn install   (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)


To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


