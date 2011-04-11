STS Issue Operation
===================

This demo illustrates a sample implementation of WS-Trust Issue operation
for the STS provider framework in CXF. This sample implementation 
supports X509Token as request credentials in the RST
and on successful authentication responds back with a signed SAMLToken.

The requestor can request for a SAML 1.1 or a SAML 2.0 token to be issued
by specifying the following attribute in the RST.

For SAML 2.0:
<TokenType>urn:oasis:names:tc:SAML:2.0:assertion</TokenType>

For SAML 1.1
<TokenType>urn:oasis:names:tc:SAML:1.1:assertion</TokenType>

For X509 based authentication a local keystore stsstore.jks is used,
which is provided with the sample.


Prerequisites
-------------
Maven is required to build and run this sample.


Building and running the demo using Maven
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

mvn install
    
To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

  mvn clean


Running the demo using Maven
---------------------------

From the base directory of this sample (i.e., where this README file is
located) run the commands, entered on a single command line:

mvn jetty:run

This will start a jetty instance on port 8080 and run the STS provider 
configured with this sample Issue operation.

