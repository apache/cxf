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
  2) cd target/deliver/jax_rs_minimal_osgi-<version>-equinox/jax_rs_minimal_osgi-<version>/
  3) java -jar  org.eclipse.osgi-3.13.0.v20180226-1711.jar

If all goes well, the Eclipse Equinox environment will start up and File Install will install all the necessary bundles.

If you visit the following URL in your favorite browser on the machine, you should find it returns some text from SampleApplication:

	http://localhost:8080/

