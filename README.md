[![Build Status](https://ci-builds.apache.org/job/CXF/job/pipeline/job/4.1.x-fixes/badge/icon?subject=Build)](https://ci-builds.apache.org/job/CXF/job/pipeline/job/4.1.x-fixes/)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.apache.cxf/cxf/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.apache.cxf/cxf)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/apache/cxf/badge)](https://api.securityscorecards.dev/projects/github.com/apache/cxf)
[![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/6978/badge)](https://bestpractices.coreinfrastructure.org/projects/6978)

Welcome to Apache CXF!
======================
Apache CXF is an open source services framework for building and deploying
web services and REST APIs in Java. CXF helps you build and develop services
using standard frontend programming APIs, including JAX-WS (SOAP) and JAX-RS
(REST). Services can speak a variety of protocols such as SOAP, XML/HTTP,
RESTful HTTP, or CORBA and work over transports such as HTTP, JMS, and JBI.

CXF integrates seamlessly with Spring and Spring Boot, providing auto-
configuration support so services can be embedded in Spring Boot applications
with minimal boilerplate. It supports generating OpenAPI/Swagger documentation
from JAX-RS services automatically, and includes a Swagger UI endpoint for
interactive API exploration.

On the security front, CXF provides comprehensive support for WS-Security,
WS-SecurityPolicy, OAuth 2.0, OpenID Connect (OIDC), and JWT, enabling both
SOAP and REST services to be secured following industry standards. TLS
configuration, client certificate authentication, and SAML token handling are
also fully supported.

CXF includes first-class observability features including integration with
Micrometer for metrics collection, enabling teams to instrument service
latency, error rates, and throughput with popular monitoring backends such as
Prometheus and Grafana.

The framework provides extensive tooling for both contract-first development
(generating Java from WSDL or OpenAPI specs) and code-first development
(generating WSDL or OpenAPI specs from annotated Java classes), all
integrated with Maven and Gradle build systems.

CXF includes a broad feature set, but it is primarily focused on the following
areas:

- Web Services Standards Support: CXF supports a wide range of web service
  standards including SOAP, the Basic Profile, WSDL, WS-Addressing,
  WS-Policy, WS-ReliableMessaging, WS-Security, WS-SecurityPolicy,
  WS-SecureConversation, and WS-Trust.
- Frontends: CXF supports multiple "frontend" programming models. CXF
  implements the JAX-WS APIs for SOAP services and JAX-RS for REST services,
  as well as a "simple frontend" for creating clients and endpoints without
  annotations. CXF supports both contract-first development with WSDL or
  OpenAPI and code-first development starting from Java.
- REST and OpenAPI: The JAX-RS frontend supports full RESTful service
  development, with automatic OpenAPI 3.0 spec generation, Swagger UI
  integration, JSON/XML message binding, and support for reactive programming
  models.
- Security: CXF provides comprehensive security capabilities including
  WS-Security for SOAP services, and OAuth 2.0, OpenID Connect, and JWT
  for REST services. Integration with Apache Wss4j and Apache Santuario
  provides robust XML security.
- Spring Boot Integration: CXF ships dedicated Spring Boot starters for
  both JAX-WS and JAX-RS, enabling auto-configured, embedded service
  deployment with minimal configuration.
- Observability: Built-in Micrometer integration provides metrics for
  service invocations, enabling monitoring with Prometheus, Grafana, and
  other observability platforms.
- Ease of use: CXF is designed to be intuitive and easy to use. Simple APIs
  enable rapid code-first service development, Maven and Gradle plugins make
  tooling integration easy, and Spring Boot starters reduce configuration
  overhead.
- Binary and Legacy Protocol Support: CXF has been designed with a pluggable
  architecture that supports not only XML but also non-XML type bindings,
  such as JSON and CORBA, in combination with any type of transport.


Export Notice
============================
This distribution includes cryptographic software.  The country in 
which you currently reside may have restrictions on the import, 
possession, use, and/or re-export to another country, of 
encryption software.  BEFORE using any encryption software, please 
check your country's laws, regulations and policies concerning the
import, possession, or use, and re-export of encryption software, to 
see if this is permitted.  See <https://www.wassenaar.org/> for more
information.

The U.S. Government Department of Commerce, Bureau of Industry and
Security (BIS), has classified this software as Export Commodity 
Control Number (ECCN) 5D002.C.1, which includes information security
software using or performing cryptographic functions with asymmetric
algorithms.  The form and manner of this Apache Software Foundation
distribution makes it eligible for export under the License Exception
ENC Technology Software Unrestricted (TSU) exception (see the BIS 
Export Administration Regulations, Section 740.13) for both object 
code and source code.

The following provides more details on the included cryptographic
software:
- https://santuario.apache.org/
- https://www.bouncycastle.org/
- https://ws.apache.org/wss4j/



Getting Started
===============

For an Apache CXF source distribution, please read BUILDING.txt for 
instructions on building Apache CXF. 

For an Apache CXF binary distribution, please read release_notes.txt
for installation instructions and list of supported and unsupported 
features.

Alternatively, you can also find out how to get started here:
https://cxf.apache.org/

If you need more help try talking to us on our mailing lists:
https://cxf.apache.org/mailing-lists.html
 
If you find any issues with CXF, please submit reports with JIRA here:
https://issues.apache.org/jira/browse/CXF

We welcome contributions, and encourage you to get involved in the CXF
community. If you'd like to learn more about how you can contribute, please
see:
https://cxf.apache.org/getting-involved.html

Thank you for using CXF!

The Apache CXF Team
https://cxf.apache.org/
