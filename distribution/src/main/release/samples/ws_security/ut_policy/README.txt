WS-SecurityPolicy Demo  (UsernameToken and Timestamp using WS-SecurityPolicy)
===============================================
This demo shows how WS-Security support in Apache CXF may be enabled.

WS-Security can be configured to the Client and Server endpoints by adding
WS-SecurityPolicies into the WSDL.

CXF 3.0.0 supports both a DOM-based (in-memory) and StAX-based (streaming)
approach to WS-Security. This demo shows how to use both approaches.

The logging feature is used to log the inbound and outbound
SOAP messages and display these to the console.

Please review the README in the samples directory before continuing.

*** Requirements ***

The samples in this directory use STRONG encryption. If you are using a version
of Java prior to 1.8.0_161 then you may need to install the Java Cryptography
Extension (JCE) Unlimited Strength Jurisdiction Policy Files [1] for the
examples to work. Note that from the 1.8.0_161 release, Java has the unlimited
strength policies installed by default. If you get errors about invalid key
lengths with an older JDK version, then the Unlimited Strength files are not
installed.

[1] http://www.oracle.com/technetwork/java/javase/downloads/index.html


Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo.

Using either UNIX or Windows:

  mvn install (builds the demo)

To use the DOM-based WS-Security functionality:

  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To use the StAX-based WS-Security functionality:

  mvn -Pstax-server  (from one command line window)
  mvn -Pstax-client  (from a second command line window)

You can also run the DOM client against the StAX server, and vice versa.

On startup, the client makes one invocation.

You can also try mvn -Pclient.unauthenticated to show that the policy
UsernameToken is enforced by the server.

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

