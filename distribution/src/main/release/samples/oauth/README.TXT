OAuth 1.0a demo (client and server)
========================================
The OAuth 1.0a demo that shows protocol's flow between OAuth parties. Demo contains two parts:

    client:
            A web application capable of making OAuth-authenticated requests. It is a usual OAuth client,
            not dependent on Apache CXF framework, built to test this demo server part and for OAuth
            educational purposes. Implementation works with any OAuth 1.0a providers, not only included
            in this demo.


    server:
            A web application capable of accepting OAuth-authenticated requests. It is based on Apache
            CXF OAuth module. It exposes OAuth endpoints and protected resources in the form of JAX-RS
            services. Server demo shows and explains how CXF OAuth module can be configured to secure
            JAX-RS services and integrated with existing web applications.

Building and running the demo using maven
---------------------------------------
	
    client:
            Main directory of client demo application is located in folder: "client", in base folder
            of this sample.
            To start demo app use maven command:

            mvn jetty:run

            It will cause in starting Jetty web server and deploying client application at host on port: 8080.
            Port number is defined in pom.xml.

    server:
            Main directory of server demo application is located in folder: "server", in base folder
            of this sample.
            To start demo app use maven command:

            mvn jetty:run

            It will cause in starting Jetty web server and deploying client application at host on port: 8081.
            Port number is defined in pom.xml.

			
	Both client and server modules depend on Spring 3, so you need to use CXF spring3 profile.
	You can build both client and server modules using command: 
			
			mvn clean install
			
	and deploy war from 'target' folder in selected web container.

Performing steps in the OAuth flow 
-----------------------------
When you have successfully deployed client and server you can start with OAuth steps:
1. Go to OAuth server (http://localhost:8081) and login with given username and password
2. Provide details and register new application at the OAuth server.
3. You have registered client application at the OAuth server, with associated and displayed
  client identifier, client shared-secret and callback url. You will need those on the client side.
4. Go to OAuth client demo (http://localhost:8080) and provide information about the registered application.
5. Perform usual OAuth 1.0 flow steps



Running OAuth 1.0a demo at Google App Engine
-----------------------------
//todo add challenges in deploying demo to GAE
