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

Please review the README in the samples directory before continuing.

*** Requirements ***

The samples in this directory use STRONG encryption.  The default encryption algorithms
included in a JRE is not adequate for these samples.   The Java Cryptography Extension
(JCE) Unlimited Strength Jurisdiction Policy Files available on Oracle's JDK download
page[3] *must* be installed for the examples to work.   If you get errors about invalid
key lengths, the Unlimited Strength files are not installed.

[3] http://www.oracle.com/technetwork/java/javase/downloads/index.html


Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo.

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Psts  (from one command line window)
  mvn -Pserver  (from a second command line window)
  mvn -Pclient  (from a third command line window)

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

