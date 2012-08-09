JAX-RS Basic Demo With HTTPS communications
===========================================

This demo takes the JAX-RS basic demo a step further 
by doing the communication using HTTPS.

The JAX-RS server is configured with a HTTPS listener. The listener 
requires client authentication so the client must provide suitable 
credentials. The listener configuration is taken from the 
"ServerConfig.xml" file located under demo directory.  

The client is configured to provide its certificate
from its keystore "config/clientKeystore.jks" to the server. 
The server authenticates the client's certificate using its own 
keystore "config/serviceKeystore.jks", which contains the 
public cert of the client.  The client makes HTTPS calls using
three methods: the portable Apache HttpComponents' HttpClient object,
CXF's WebClient object, and CXF's JAXRSClientFactory object.

Likewise the client authenticates the server's certificate "CN=localhost"
using its keystore.  Note also the usage of the cipherSuitesFilter 
configuration in the configuration files, where each party imposes 
different ciphersuites constraints, so that the ciphersuite eventually
negotiated during the TLS handshake is acceptable to both sides. 
This may be viewed by adding a -Djavax.net.debug=all argument to the JVM.

But please note that it is not advisable to store sensitive data such
as passwords stored in a clear text configuration file, unless the
file is sufficiently protected by OS level permissions. The KeyStores
may be configured programmatically so using user interaction may be
employed to keep passwords from being stored in configuration files.
The approach taken here is for demonstration reasons only. 

Please review the README in the samples directory before
continuing.


Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    
To remove the target dir, run "mvn clean".


Certificates
------------
See the src/main/config folder for the sample keys used (don't use
these keys in production!) as well as scripts used for their creation.
