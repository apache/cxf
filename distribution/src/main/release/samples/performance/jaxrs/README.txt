JAX-RS Basic With HTTPS communications Performance Suite
=========================================================

This sample takes the JAX-RS basic HTTPS demo a step further
by integrating it with the performance test framework.

The JAX-RS server is configured with a HTTPS listener. The listener
requires client authentication so the client must provide suitable
credentials. The listener configuration is taken from the
"ServerConfig.xml" file located under demo directory.

The client is configured to provide its certificate
from its keystore "config/clientKeystore.jks" to the server.
The server authenticates the client's certificate using its own
keystore "config/serviceKeystore.jks", which contains the
public cert of the client.  The client makes HTTPS calls using
CXF's WebClient object.

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


Building and running the performance test case using Maven
----------------------------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the Maven pom.xml file can be used to build and run the demo.


Using either UNIX or Windows:

  cd base
  mvn install
  cd ../jaxrs
  mvn install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

Alternatively, client and server may be executed from one window.

  mvn -Pclientserver [-Dthreads=N] [-Doperation=get] [-Dtime=M]

To remove the target dir, run "mvn clean".

The JAX-RS Server takes the following arguments:
     -protocol  Connection protocol (http, https)
     -host      Allow server to bind to address (localhost, 0.0.0.0, etc)

The JAX-RS Client takes the following arguments:
     -protocol  Connection protocol (http, https)
     -host      Host address client will connect to (localhost, 0.0.0.0, etc)
     -Operation The rest verb to execute (get, post, put, delete)
     -Time      define the amount of time to spend making invocations in seconds
     -Threads   define the number of threads to run the performance client


Certificates
------------
See the src/main/config folder for the sample keys used (don't use
these keys in production!) as well as scripts used for their creation.
