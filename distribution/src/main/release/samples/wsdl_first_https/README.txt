Hello World Demo using HTTPS communications
=============================================

This demo takes the hello world demo a step further 
by doing the communication using HTTPS.

Please review the README in the samples directory before
continuing.


Prerequisite
------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to run the environment script described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment by running the script.


Building and running the demo using maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

  mvn install (builds the demo)

In separate windows:
  mvn -Pserver (starts the server)
  mvn -Psecure.client (runs the client in secure mode, Scenario 2)
  mvn -Pinsecure.client (runs the client in insecure mode, Scenario 1)
  mvn clean (removes all generated and compiled classes)


Building and running the demo using Ant
---------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server target automatically builds the demo.

Using either UNIX or Windows:

  ant server
    

To remove the code generated from the WSDL file and the .class
files, run "ant clean".


The demo illustrates how authentication can be achieved through
configuration using 2 different scenarios. The non-defaulted security
policy values are be specified via configuration files.

Scenario 1:

A HTTPS listener is started up. The listener requires
client authentication so the client must provide suitable credentials.
The listener configuration is taken from the "CherryServer.cxf" file
located in this directory.  The client's security data is taken from
from the "InsecureClient.cxf" file in this directory, using the bean name:
"{http://apache.org/hello_world_soap_http}SoapPort.http-conduit". The
client does NOT provide the appropriate credentials and so the
invocation on the server fails.

To run:

  ant server
  ant insecure.client

Scenario 2: 
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

But please note that it is not adviseable to store sensitive data such
as passwords stored in a clear text configuration file, unless the
file is sufficiently protected by OS level permissions. The KeyStores
may be configured programatically so using user interaction may be
employed to keep passwords from being stored in configuration files.
The approach taken here is for demonstration reasons only. 


To run:

  ant server
  ant secure.client

Certificates:
If the certificates are expired for some reason, a shell script in 
bin/gencerts.sh will generate the set of certificates needed for
this sample. Just do the following:

        cd certs
        sh ../bin/gencerts.sh
       
   
