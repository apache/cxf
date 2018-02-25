toolchain-integration-tests
===========================

jdk6-cxf-with-toolchain
-----------------------

This project contains sample maven module using `cxf-codegen-plugin` to generate java from WSDL file.

- it enforces usage of JDK 1.7 or higher to run the `mvn` command
- it configures `maven-toolchains-plugins` to target JDK 6
- cxf `fork` parameter if set to true to enable toolchain detection
- if the toolchain wasn't correctly used by the cxf-codegen-plugin`, the the build should fail during the _compile_ phase

wsdl-artifact-resolution
------------------------

Verifies that a wsdlArtifact from local repository is properly resolved for codegen.

wsdl-classpath-resolution
------------------------

Verifies that using wsdlRoot with "classpath;<path-to-replace>" will generate Java classes with valid classpath 
URLs instead of the local file URL.