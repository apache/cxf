This folder contains the XKMS (XML Key Management Service) implementation of 
Apache CXF. It contains:


xkms-client - The XKMS client and invoker implementations
xkms-common - Common functionality, XML schemas, generated code
xkms-service - The XKMS core service implementation
xkms-x509-handlers - The implementation of pluggable commands for X509 keys.
xkms-features - Karaf features for XKMS client and service
xkms-itests - Integration tests
xkms-osgi - OSGi blueprint configuration for OSGi deployment
xkms-war - Web spring configuration for Web depoyment

Installation
------------

Karaf 2.x:
features:addurl mvn:org.apache.cxf.services.xkms/cxf-services-xkms-features/3.4.0-SNAPSHOT/xml
features:install cxf-xkms-service cxf-xkms-client

Karaf 3.x and later:
feature:repo-add mvn:org.apache.cxf.services.xkms/cxf-services-xkms-features/3.4.0-SNAPSHOT/xml
feature:install cxf-xkms-service cxf-xkms-client
