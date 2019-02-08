Security Token Service (STS) Demo
=================================

This demo shows how to use the Security Token Service (STS) implementation in
Apache CXF.

The policy of the service provider, as defined in hello_world.wsdl, requires a
SAML 2.0 token issued by an STS. The client will authenticate itself to the
STS using a UsernameToken over the symmetric binding, and the STS will issue
it the desired SAML 2.0 token, which the client then forwards to the service
provider. As the IssuedToken is defined as the InitiatorToken of the
Asymmetric binding in the policy of the service provider, the client will use
the associated secret key to sign various parts of the message.

CXF 3.0.0 supports both a DOM-based (in-memory) and StAX-based (streaming)
approach to WS-Security. This demo shows how to use both approaches.

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
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo.

Using either UNIX or Windows:

  mvn install (builds the demo)

To use the DOM-based WS-Security functionality:

  mvn -Psts  (from one command line window)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To use the StAX-based WS-Security functionality:

  mvn -Pstax-sts  (from one command line window)
  mvn -Pstax-server  (from one command line window)
  mvn -Pstax-client  (from a second command line window)

You can also run the DOM client against the StAX server, and vice versa, or use the
StAX STS with the DOM client, etc.

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

