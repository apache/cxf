Hello World Demo using HTTPS communications
=============================================

This demo takes the hello world demo a step further 
by doing the communication using HTTPS.

Please review the README in the samples directory before
continuing.

Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 

  mvn install (builds the demo)

In separate windows:
  mvn -Pserver (starts the server)
  mvn -Pinsecure.client (runs the client in insecure mode, Scenario 1)
  mvn -Psecure.client (runs the client in secure mode, Scenario 2)
  mvn -Pinsecure.client.non.spring (runs the client in insecure mode without Spring configuration, Scenario 3)
  mvn -Psecure.client.non.spring (runs the client in secure mode without Spring configuration, Scenario 4)
  mvn clean (removes all generated and compiled classes)"


The demo illustrates how authentication can be achieved through
configuration using 3 different scenarios. The non-defaulted security
policy values are be specified via configuration files or programmatically.

Scenario 1:  (-Pinsecure.client)

A HTTPS listener is started up. The listener requires
client authentication so the client must provide suitable credentials.
The listener configuration is taken from the "CherryServer.cxf" file
located in this directory.  The client's security data is taken from
from the "InsecureClient.cxf" file in this directory, using the bean name:
"{http://apache.org/hello_world_soap_http}SoapPort.http-conduit". The
client does NOT provide the appropriate credentials and so the
invocation on the server fails.

Scenario 2:  (-Psecure.client)
The same HTTPS listener is used. The client's security data is taken
from the "WibbleClient.cxf" configuration file in this directory, 
using the bean name:
"{http://apache.org/hello_world_soap_http}SoapPort.http-conduit". 

The client is configured to provide its certificate "CN=Wibble" and
chain stored in the Java KeyStore "certs/wibble.jks" to the server. The
server authenticates the client's certificate using its trust store
"certs/truststore.jks", which holds the Certificate Authorities'
certificates.

Likewise the client authenticates the server's certificate "CN=Cherry"
and chain against the same trust store.  Note also the usage of the
cipherSuitesFilter configuration in the configuration files,
where each party imposes different ciphersuites contraints, so that the
ciphersuite eventually negotiated during the TLS handshake is acceptable
to both sides. This may be viewed by adding a -Djavax.net.debug=all 
argument to the JVM.

But please note that it is not advisable to store sensitive data such
as passwords stored in a clear text configuration file, unless the
file is sufficiently protected by OS level permissions. The KeyStores
may be configured programmatically so using user interaction may be
employed to keep passwords from being stored in configuration files.
The approach taken here is for demonstration reasons only. 

Scenario 3: (-Pinsecure.client.non.spring)

A HTTPS listener is started up.  The client does NOT provide the appropriate 
credentials programmatically and so the invocation on the server fails.

  
Scenario 4: (-Psecure.client.non.spring)

A HTTPS listener is started up. The client's security data
is in essence the same as for scenario 2, however this time it 
is provided programmatically in the client code, ClientNonSpring.java. 

But please note that it is not advisable to store sensitive data such
as passwords stored directly in java code as the code could possibly be 
disassembled. Typically the password would be obtained at runtime by 
prompting for the password. 
The approach taken here is for demonstration reasons only. 


Certificates:
If the certificates are expired for some reason, a shell script in 
bin/gencerts.sh will generate the set of certificates needed for
this sample. Just do the following:

        cd certs
        sh ../bin/gencerts.sh
       

