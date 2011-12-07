
This folder contains the STS (Security Token Service) implementation of 
Apache CXF. It contains:

sts-core - The core STS implementation

sts-war - A sample war deployment for the STS. This can be deployed to Tomcat
via "mvn tomcat:deploy". Note that to run the systests below against this
deployment, it is necessary to configure TLS in Tomcat appropriately. Edit
the conf/server.xml file to enable a TLS port on 8443, e.g.:

<Connector port="8443" protocol="HTTP/1.1" SSLEnabled="true"
               maxThreads="150" scheme="https" secure="true"
               keystoreFile="$CXF_HOME/services/sts/sts-war/src/main/resources/tomcatKeystore.jks"
               keystorePass="tompass"
               clientAuth="false" sslProtocol="TLS" />

systests/basic - System tests that use the STS. An embedded jetty version of
the STS is used for testing by default. The tests can also be run using the
war created in the sts-war module by running the tests with "-Pwar".

