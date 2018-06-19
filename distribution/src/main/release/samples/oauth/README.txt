OAuth 1.0a demo (client and server)
========================================
OAuth 1.0a demo that shows the protocol's flow between OAuth parties. The demo
contains two parts:

 * client: A web application capable of making OAuth-authenticated requests.
   It is a usual OAuth client, not dependent on Apache CXF framework, built to
   test this demo server part and for OAuth educational purposes.
   The implementation works with any OAuth 1.0a providers, not only that
   included in this demo.

 * server: A web application capable of accepting OAuth-authenticated requests.
   It is based on the Apache CXF OAuth module. It exposes OAuth endpoints and
   protected resources in the form of JAX-RS services. The Server demo shows
   and explains how the CXF OAuth module can be configured to secure JAX-RS
   services and integrated with existing web applications.

Building and running the demo using maven
---------------------------------------
	
 * client: Main directory of client demo application is located in folder:
   "client", in the base folder of this sample. To start the demo app use the
   maven command:

       mvn jetty:run

   It will start the Jetty web server and deploy the client application at the
   local host on port 8080. This port number is defined in the pom.xml.

 * server: Main directory of server demo application is located in folder: 
   "server", in the base folder of this sample. To start the demo app use the
   maven command:

       mvn jetty:run

   It will start the Jetty web server and deploy the client application at the
   local host on port 8081. This port number is defined in the pom.xml.

Performing steps in the OAuth flow 
-----------------------------
When you have successfully deployed the client and server, follow these steps:

 1. Go to the OAuth server (http://localhost:8081) and login with the given
    username and password
 2. Provide details and register new application at the OAuth server.
 3. You have registered the client application at the OAuth server, with
    associated and displayed client identifier, client shared-secret and
    callback url. You will need those on the client side.
 4. Go to OAuth client demo (http://localhost:8080) and provide information
    about the registered application.
 5. Perform usual OAuth 1.0 flow steps


