JAX-RS Basic Spark Demo 
=======================

This demo demonstrates how to connect HTTP and Spark streams with JAX-RS

Build the demo with "mvn install" and start it with

mvn exec:java

Next do: 

curl -X POST -H "Accept: text/plain" -H "Content-Type: text/plain" -d "Hello Spark" https://localhost:9000/stream

Limitations: 

This demo accepts one request at a time due to Spark restricting that only a single streaming context can be active
in JVM at a given moment of time. This is the error which will be logged if you try to access the demo server concurrently:

"org.apache.spark.SparkException: Only one SparkContext may be running in this JVM (see SPARK-2243).
 To ignore this error, set spark.driver.allowMultipleContexts = true".
 
 More flexible demo server will be added in due time. 

