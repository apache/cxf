toolchain-integration-tests
===========================

jdk6-cxf-with-toolchain
-----------------------

This project contains sample maven module using `cxf-codegen-plugin` to generate java from WSDL file.

- it enforces usage of JDK 1.7 or higher to run the `mvn` command
- it configures `maven-toolchains-plugins` to target JDK 6
- cxf `fork` parameter if set to true to enable toolchain detection
- if the toolchain wasn't correctly used by the cxf-codegen-plugin`, the the build should fail during the _compile_ phase
