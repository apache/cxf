
This folder contains the STS (Security Token Service) implementation of 
Apache CXF. It contains:

sts-core - The core STS implementation

sts-war - A sample war deployment for the STS

systests/basic - System tests that use the STS. An embedded jetty version of
the STS is used for testing by default. The tests can also be run using the
war created in the sts-war module by running the tests with "-Pwar".

