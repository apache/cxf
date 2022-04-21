Minimal OSGI Sample Documentation
=================================

Introduction
-------------

You may not know it but CXF actually has an OSGI bundle for JAX-RS. Unfortunately, documentation on how to use it is very hard to come by.
This sample demonstrates the minimal amount of bundles necessary and code tweaks required to get a sample that uses CXF's JAX-RS
implementation to work correctly in an OSGI environment.

This is not necessarily the best way to achieve the above. In particular, use of CXF-specific syntax (as much as possible). However, I believe it's a
good balance between code readability and simplicity.

Introduction
Pre-requisites
Sample walkthrough
 Code walkthrough
 Deployment notes
Running the sample
Bundles

Pre-requisites
--------------

To take advantage of this sample, you should have a working knowledge of the following:

JSR-370 JAX-RS
* Maven
* OSGI
* Servlet

Sample walkthrough
------------------

Code walkthrough

Let's start off with a basic JSR-370 JAX-RS application that you would like to deploy in an OSGI environment:
* SampleResource
* SampleApplication

With this sample application, if you go to a URL that looks like the following, you should get a "My Sample Text" in the browser:
	http://<host>:<port>/

To get this application running in an OSGi environment, when the HTTP OSGI service starts up, CXFNonSpringJaxrsServlet needs to be
registered. This is accomplished in the code found inside the Activator. We use ServiceTracker to take care of the hard work of monitoring
when the HTTP OSGI service comes up and registering ourselves to it.

The big gotcha here though is our application is in one bundle but CXF is contained within another bundle. Because OSGI creates one
classloader for each bundle and CXF uses reflection to discover applications, this discovery won't work in an OSGI environment. To get around
this limitation, we subclass CXFNonSpringJaxrsServlet with our own SampleServlet that refers directly to our SampleApplication
class. With the necessary imports inside the MANIFEST.MF, OSGI will set up the correct bundle linkage at runtime and we won't get the dreaded
ClassNotFound exception.

Deployment notes

By default, CXF's default OSGI bundle includes a whack of dependencies that aren't necessary to get the core JAX-RS running. This sample
strips away all the fluff and only includes the necessary JARs. You can check the pom.xml accompanying this example to see many of the
dependencies that have been excluded.

To ease deployment in different OSGI environments, we take advantage of Apache Felix's File Install bundle to use a directory-based deployment
structure.

With a few tweaks here and there, you can configure this sample to run in different OSGI environments. By default, it works in Eclipse Equinox but
it runs sufficiently well in Apache Felix as well.

Running the sample
------------------

You can run it directly from maven using the following command
	
	mvn exec:exec

Note that when you run the sample this way, your input to the OSGI console will not echo. No worries, it'll still be processed!

If you want to run it manually, do the following

  1) mvn package
  2) cd target/delivery/jax_rs_minimal_osgi-<version>-equinox/jax_rs_minimal_osgi-<version>/
  3) java -jar  org.eclipse.osgi-3.13.0.v20180226-1711.jar

If all goes well, the Eclipse Equinox environment will start up and File Install will install all the necessary bundles.

If you visit the following URL in your favorite browser on the machine, you should find it returns some text from SampleApplication:

	http://localhost:8080/

Bundles
------------------

   ID|State      |Level|Name                                                          
    0|Active     |    0|OSGi System Bundle (3.13.0.v20180226-1711)|3.13.0.v20180226-1711
    1|Active     |    4|Apache Felix File Install (3.6.4)|3.6.4              
    2|Active     |    4|org.osgi:org.osgi.service.cm (1.6.0.201802012106)|1.6.0.201802012106
    3|Active     |    4|org.osgi:org.osgi.service.event (1.4.0.201802012106)|1.4.0.201802012106
    8|Active     |    1|Jetty :: Websocket :: Client (9.2.6.v20141205)|9.2.6.v20141205
    9|Active     |    1|javax.annotation API (1.3.1)|1.3.1
   10|Active     |    1|jcl-over-slf4j (1.7.25)|1.7.25
   11|Active     |    1|Apache Felix Http Bridge (2.3.2)|2.3.2
   12|Active     |    1|Old JAXB Runtime (2.3.0)|2.3.0                                    
   13|Active     |    1|Jetty :: Websocket :: API (9.2.6.v20141205)|9.2.6.v20141205                                                                                                             
   14|Active     |    1|Apache CXF Runtime HTTP Transport (3.3.0.SNAPSHOT)|3.3.0.SNAPSHOT
   15|Active     |    1|Apache Felix Http Base (2.3.2)|2.3.2                         
   16|Active     |    1|Apache Felix Http Jetty (3.0.0)|3.0.0   
   18|Active     |    1|Jetty :: Websocket :: Servlet Interface (9.2.6.v20141205)|9.2.6.v20141205
   19|Active     |    1|JavaBeans Activation Framework (1.2.0)|1.2.0
   20|Active     |    1|Old JAXB XJC (2.3.0)|2.3.0
   21|Active     |    1|Woodstox (5.0.3)|5.0.3
   22|Active     |    1|javax.xml.soap API (1.4.0)|1.4.0
   23|Active     |    1|Web Services Metadata 2.0 (1.1.3)|1.1.3
   24|Active     |    1|Activation 1.1 (1.1.0)|1.1.0
   25|Active     |    1|Jetty :: Http Utility (9.4.12.v20180830)|9.4.12.v20180830
   26|Active     |    1|Jetty :: Websocket :: Common (9.2.6.v20141205)|9.2.6.v20141205
   27|Active     |    1|jaxb-api (2.3.0)|2.3.0
   28|Active     |    1|Jetty :: Utilities (9.4.12.v20180830)|9.4.12.v20180830
   29|Active     |    1|Apache ServiceMix :: Specs :: JAX-RS API 2.1 (2.9.1)|2.9.1
   30|Active     |    1|Apache Felix Http Bundle (3.0.0)|3.0.0
   31|Active     |    1|Apache ServiceMix :: Specs :: SAAJ API 1.3 (2.9.0)|2.9.0
   32|Active     |    1|Stax2 API (3.1.4)|3.1.4
   33|Active     |    1|Extended StAX API (1.7.8)|1.7.8
   34|Active     |    1|Jetty :: Webapp Application Support (9.4.12.v20180830)|9.4.12.v20180830
   35|Active     |    1|Apache ServiceMix :: Specs :: Stax API 1.0 (2.9.0)|2.9.0
   36|Active     |    1|Apache ServiceMix :: Specs :: JAXWS API 2.2 (2.9.0)|2.9.0
   37|Active     |    1|Jetty :: XML utilities (9.4.12.v20180830)|9.4.12.v20180830
   38|Active     |    1|Jetty :: Websocket :: Server (9.2.6.v20141205)|9.2.6.v20141205
   39|Active     |    1|Jetty :: Server Core (9.4.12.v20180830)|9.4.12.v20180830
   40|Active     |    1|Jetty :: Security (9.4.12.v20180830)|9.4.12.v20180830
   41|Active     |    1|Apache Felix Http Api (2.3.2)|2.3.2
   42|Active     |    1|Jetty :: IO Utility (9.4.12.v20180830)|9.4.12.v20180830
   43|Active     |    1|Old JAXB Core (2.3.0)|2.3.0
   44|Active     |    1|Apache CXF Core (3.3.0.SNAPSHOT)|3.3.0.SNAPSHOT
   45|Active     |    1|Jetty :: JMX Management (9.4.12.v20180830)|9.4.12.v20180830
   46|Active     |    1|Apache CXF Advanced Logging Feature (3.3.0.SNAPSHOT)|3.3.0.SNAPSHOT
   47|Active     |    1|JavaBeans Activation Framework API jar (1.2.0)|1.2.0
   48|Active     |    1|Java Servlet API (3.1.0)|3.1.0
   49|Active     |    1|XmlSchema Core (2.2.4)|2.2.4
   50|Active     |    1|MIME streaming extension (1.9.7)|1.9.7
   51|Active     |    1|Apache CXF Runtime JAX-RS Frontend (3.3.0.SNAPSHOT)|3.3.0.SNAPSHOT
   52|Active     |    1|Apache CXF Runtime Security functionality (3.3.0.SNAPSHOT)|3.3.0.SNAPSHOT
   53|Active     |    1|Apache Felix Http Whiteboard (2.3.2)|2.3.2
   54|Active     |    1|Jetty :: Servlet Handling (9.4.12.v20180830)|9.4.12.v20180830
   55|Active     |    1|geronimo-stax-api_1.0_spec (1.0.1)|1.0.1
   56|Active     |    1|Apache ServiceMix :: Specs :: JAXB API 2.2 (2.9.0)|2.9.0
   57|Active     |    1|slf4j-api (1.7.25)|1.7.25
   59|Active     |    1|SAAJ 1.3 (1.1.0)|1.1.0
   60|Active     |    1|minimalosgi (0.0.0)|0.0.0
