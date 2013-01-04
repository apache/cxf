Hello World Demo using HTTPS communications
=============================================
This demo provides a "hello world" example of making SOAP calls with HTTPS.

Please review the README in the samples directory before continuing.

Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 

  mvn clean install (builds the demo)

In the first terminal windows:
  mvn -Pserver (starts the server)

Sequentially, in the second terminal window:
  mvn -Pinsecure.client (Scenario 1, will fail due to no credentials provided)
  mvn -Psecure.client (Scenario 2, runs successfully)
  mvn -Pinsecure.client.non.spring (Scenario 3, will fail due to no credentials provided)
  mvn -Psecure.client.non.spring (Scenario 4, runs successfully)

Later, when desired:
  mvn clean (removes all generated and compiled classes)


The demo illustrates how authentication can be achieved through
configuration using two different scenarios, via configuration files
or using the CXF Java API.

For all four scenarios, the same HTTPS listener (activated above via
mvn -Pserver) is used.  This listener requires client authentication
so the client must provide suitable credentials.  The listener configuration 
is given in the "CherryServer.xml" file located in the /server folder.

Scenario 1:  (-Pinsecure.client)

The client's security data is taken from from the "InsecureClient.xml" file 
in the /client directory, using the bean name: 
"{http://apache.org/hello_world_soap_http}SoapPort.http-conduit". 
This file does not have any credential information so the SOAP call
ton the server fails.

Scenario 2:  (-Psecure.client)

Same as above, except this time "SecureClient.xml", which contains the
appropriate credential information.  This SOAP call will succeed here.

In this configuration file, the client provides its certificate "CN=Wibble" 
and chain stored in the Java KeyStore "certs/wibble.jks" to the server. The
server authenticates the client's certificate using its truststore
"certs/truststore.jks", which holds the Certificate Authorities'
certificates.

Likewise the client authenticates the server's certificate "CN=Cherry"
and chain against the same trust store.  Note the usage of the
cipherSuitesFilter configuration in the configuration files,
where each party imposes different ciphersuites constraints, so that the
ciphersuite eventually negotiated during the TLS handshake is acceptable
to both sides. This may be viewed by adding a -Djavax.net.debug=all 
argument to the JVM.

But please note that it is not advisable to store sensitive data such
as passwords in clear text configuration files, unless the
file is sufficiently protected by OS level permissions. The KeyStores
may be configured programmatically so user interaction may be
employed to keep passwords from being stored in configuration files.
The approach taken here is for demonstration purposes only.

Scenario 3: (-Pinsecure.client.non.spring)

Here, configuration is done via Java API (in ClientSpring.java) and not
Spring XML files.  The client does NOT provide the appropriate credentials 
programmatically and so the invocation on the server fails.

Scenario 4: (-Psecure.client.non.spring)

Same Java class as in Scenario #3 is used, however the class is coded 
to configure TLS appropriately in this circumstance, so the SOAP call
will succeed.  Please note that it is not advisable to store sensitive 
data such as passwords directly in java code as the code could
possibly be disassembled. Typically the password would be obtained at 
runtime by prompting for the password.  The approach taken here is for
demonstration purposes only.

Certificates:
See the src/main/config folder for the sample keys used (don't use
these keys in production!) as well as scripts used for their creation.

