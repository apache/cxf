Hello World Demo using Document/Literal Style and XMLBeans
==========================================================

This demo illustrates the use of the JAX-WS APIs and with the XMLBeans data
binding to run a simple client against a standalone server using SOAP 1.1 over
HTTP.

It also shows how CXF configuration can be used to enable schema validation
on the client and/or server side: By default the message parameters would not
be validated, but the presence of the cxf.xml configuration file on
the classpath, and its content change this default behavior:
The configuration file specifies that 

a) if a JAX-WS client proxy is created for port
{http://apache.org/hello_world_soap_http}SoapPort it should have schema
validation enabled.

b) if a JAX-WS server endpoint is created for port
{http://apache.org/hello_world_soap_http}SoapPort it should have schema
validation enabled.

The client's second greetMe invocation causes an exception (a marshalling
error) on the client side, i.e. before the request with the invalid parameter
goes on the wire.

After commenting the definition of the <jaxws:client> element in cxf.xml you 
will notice that the client's second greetMe invocation still throws an
exception, but that this time the exception is caused by an unmarshalling
error on the server side.

Commenting both elements, or renaming/removing the cfg.xml file, and thus
restoring the default behavior, results in the second greetMe invocation
not causing an exception.

Please review the README in the samples directory before continuing.

Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn install   (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".


Schema Validation Exception
----------------------------
When running the client with mvn -Pclient, you may see exceptions like
org.apache.xmlbeans.impl.values.XmlValueOutOfRangeException: string length (67) 
is greater than maxLength facet (30) for MyStringType in namespace 
http://apache.org/hello_world_soap_http/types
This is to be expected because in the wsdl we include restrictions such as
            <simpleType name="MyStringType">
                <restriction base="string">
                    <maxLength value="30" />
                </restriction>
            </simpleType>
for the greetMe request message,
and we're also enabling schema validation in our cxf.xml
            <jaxws:properties>
                <entry key="schema-validation-enabled" value="true" />
            </jaxws:properties>
so if the greetMe request length is bigger than 30 characters, we will see
this exception.

