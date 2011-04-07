JAX-RS Basic Demo With HTTPS communications
===========================================

This demo takes the JAX-RS basic demo a step further 
by doing the communication using HTTPS.

The JAX-RS server is configured with a HTTPS listener. The listener 
requires client authentication so the client must provide suitable 
credentials. The listener configuration is taken from the 
"CherryServer.xml" file located under demo directory.  

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

NOTE: Classes AuthSSLInitializationError, AuthSSLProtocolSocketFactory, 
and AuthSSLX509TrustManager are files copied from the Apache HTTP Client
project and used by the client for certificate validation.

Please review the README in the samples directory before
continuing.


Building and running the demo using maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 


Using either UNIX or Windows:

  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)
    

To remove the target dir, run "mvn clean".




Certificates
------------

If the certificates are expired or unusable for some reason, a shell 
script in the certs folder will generate a new set of certificates 
needed for this sample. Just do the following:

  cd certs
  sh gencerts.sh
