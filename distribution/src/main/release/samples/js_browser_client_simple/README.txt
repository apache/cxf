JavaScript Client Demo using Document/Literal Style
===================================================
This demo illustrates the use of the JavaScript client generator. This
demo deploys a service based on the wsdl_first demo, and then
provides a browser-compatible client that communicates with it. Please
read the README.txt for the wsdl_first sample for more information on
the service.

The cxf.xml for this sample configures the embedded Jetty web server
to deliver static HTML content from the 'staticContent' directory.

Please review the README in the samples directory before continuing.

Please see the wiki user documentation for complete information on
JavaScript client feature.

Prerequisite
------------
If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK directories on the PATH it is not 
necessary to set the environment variables as described in
the samples directory README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment variables.


Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located)

Using either UNIX or Windows:

  mvn install
  mvn -Pserver
  mvn -Pclient


Running the client in a browser
-------------------------------
Once the server is running, confirm you can see the WSDL at:

  http://localhost:9000/SoapContext/SoapPort?wsdl

Then browse to:

  http://localhost:9000/HelloWorld.html

(Change localhost above if you're using a different hostname.)

On the web page you see, click on the 'invoke' button to invoke the
very simple sayHi service, which takes no input and returns a single
string.


Schema Validation Exception
----------------------------
When running the client with mvn -Pclient, you may see exceptions like
Marshalling Error: cvc-maxLength-valid: Value 'Invoking greetMe with 
invalid length string, expecting exception...' with length = '67' is 
not facet-valid with respect to maxLength '30' for type 'MyStringType'. 
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



