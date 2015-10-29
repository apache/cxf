JAX-RS Big Query Demo 
=====================

This demo demonstrates how Google BigQuery service can be used with 
OAuth2 Authorization Code flow and a Google server-to-server flow and shows
Apache CXF OIDC RP and JOSE support in action. 

The demo accesses public BigQuery data sets - it is easy to modify it to access 
the project-specific datasets if preferred.

First, create a Big Query project as described in
https://cloud.google.com/bigquery/sign-up

1. OAuth2 Authorization Code flow.

Create a client id and secret as described in 
https://cloud.google.com/bigquery/authorization#clientsecrets.

Set Redirect URI to 
https://localhost:8080/bigquery/service/oidc/rp/complete 

Build the demo with "mvn install" and start it with

mvn jetty:run-war -Dclient_id=${client_id} -Dclient_secret=${client_secret} -Dproject_id=${project_id}

where ${client_id} and ${client_secret} are Client Id and Secret, and ${project_id} is the id of your Google project.

Then start a browser and go to 

https://localhost:8080/bigquery/simpleLogin.jsp

2. Server to Server Flow.

Create a client with a service account as described in
https://developers.google.com/identity/protocols/OAuth2ServiceAccount,
choose "Generate New P12 Key" and save it to the local disk.


Build the demo with "mvn install" and start it with

mvn exec:java -Dexec.args="/home/pathto/BigQueryProjectKey.p12 notasecret ${client_email} ${project_id}"

where ${client_email} is Service Account Client Email and ${project_id} is the id of your Google project.







