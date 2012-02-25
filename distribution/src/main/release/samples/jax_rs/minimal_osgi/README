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

JSR-311 JAX-RS
* Maven
* OSGI
* Servlet

Sample walkthrough
------------------

Code walkthrough

Let's start off with a basic JSR-311 JAX-RS application that you would like to deploy in an OSGI environment:
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
  2) cd target/deliver/jax_rs_minimal_osgi-<version>-equinox/jax_rs_minimal_osgi-<version>/
  3) java -jar org.eclipse.osgi-3.6.2.R36x_v20110210.jar

If all goes well, the Eclipse Equinox environment will start up and File Install will install all the necessary bundles.

If you visit the following URL in your favorite browser on the machine, you should find it returns some text from SampleApplication:

	http://localhost:8080/

Bundles
-------

If you're using Java 6, here is a listing of the bundles that are running inside Eclipse Equinox to make this example run successfully:

id      State       Bundle
0       ACTIVE      org.eclipse.osgi_3.6.2.R36x_v20110210                  ; OSGI system runtime
1       ACTIVE      org.apache.felix.fileinstall_3.1.10                    ; Bundle deployment system - may be removed if you have some other way to deploy
2       ACTIVE      org.codehaus.jettison.jettison_1.3.0                   ; CXF JAX-RS dependency for JSON
3       ACTIVE      org.mortbay.jetty.util_6.1.24                          ; HTTP OSGI service dependency
4       ACTIVE      org.apache.cxf.bundle-jaxrs_2.5.0.SNAPSHOT             ; CXF JAX-RS bundle
5       ACTIVE      org.apache.neethi_3.0.1                                ; CXF JAX-RS dependency
6       ACTIVE      org.eclipse.osgi.services_3.2.100.v20100503            ; CXF JAX-RS dependency / HTTP OSGI service dependency
7       ACTIVE      org.apache.felix.http.whiteboard_2.2.0                 ; HTTP OSGI service dependency
8       ACTIVE      org.mortbay.jetty.security_6.1.24                      ; HTTP OSGI service dependency
9       ACTIVE      org.mortbay.jetty.server_6.1.24                        ; HTTP OSGI service dependency
10      ACTIVE      org.apache.felix.http.bundle_2.2.0                     ; HTTP OSGI service dependency
11      ACTIVE      org.apache.felix.http.base_2.2.0                       ; HTTP OSGI service dependency
12      ACTIVE      org.apache.servicemix.specs.jsr311-api-1.1_1.8.0       ; CXF JAX-RS dependency
13      ACTIVE      org.apache.felix.http.bridge_2.2.0                     ; HTTP OSGI service dependency
14      ACTIVE      minimalosgi_0.0.0                                      ; Sample application
15      ACTIVE      org.apache.felix.http.api_2.2.0                        ; HTTP OSGI service dependency
16      ACTIVE      org.apache.felix.http.jetty_2.2.0                      ; HTTP OSGI service dependency

If you're using Java 5, there's a few more bundles that are added to make things run successfully

id      State       Bundle
17      ACTIVE      org.apache.geronimo.specs.geronimo-activation_1.1_spec_1.1.0    ; CXF JAX-RS dependency
18      ACTIVE      org.apache.servicemix.specs.stax-api-1.0_1.9.0                  ; CXF JAX-RS dependency
19      ACTIVE      org.apache.servicemix.specs.activation-api-1.1_1.9.0            ; CXF JAX-RS dependency
20      ACTIVE      org.apache.servicemix.specs.jaxb-api-2.2_1.9.0                  ; CXF JAX-RS dependency
21      ACTIVE      org.apache.geronimo.specs.geronimo-annotation_1.0_spec_1.1.1    ; CXF JAX-RS dependency
