toolchain-integration-tests
===========================

jdk-cxf-with-toolchain
-----------------------

Currently disabled due to https://issues.apache.org/jira/browse/CXF-7714

This project contains sample maven module using `cxf-codegen-plugin` to generate java from WSDL file.

- it enforces usage of JDK 1.9 or higher to run the `mvn` command
- it configures `maven-toolchains-plugins` to target JDK 8
- cxf `fork` parameter if set to true to enable toolchain detection
- if the toolchain wasn't correctly used by the cxf-codegen-plugin`, the the build should fail during the _compile_ phase

wsdl-artifact-resolution
------------------------

Verifies that a wsdlArtifact from local repository is properly resolved for codegen.
