JAX-RS Basic OpenId Connect Demo 
================================

This demo demonstrates how to authenticate a user with OpenId Connect.

OAuth2 Implicit Flow is implemented by Google Authentication Script.

1. Create a new Client In Google Developer Console,  

Ensure a Redirect URI field is empty and set JavaScript Origins to https://localhost:8080. 
 
Build the demo with "mvn install" and start it with

mvn jetty:run-war -Dclient_id=${client_id}

Then start a browser and go to 

https://localhost:8080/user/simpleLogin.html


