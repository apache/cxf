JAX-RS Spark Streaming Demo 
===========================

This demo demonstrates how to connect HTTP and Spark streams with JAX-RS

Build the demo with "mvn install" and start it with

mvn exec:java

Next do: 

1. Simple text processing:

curl -X POST -H "Accept: text/plain" -H "Content-Type: text/plain" -d "Hello Spark" http://localhost:9000/stream

2. PDF processing:

Open multipart.html located in src/main/resources, locate any PDF file available on the local disk and upload.

Note Spark restricts that only a single streaming context can be active in JVM at a given moment of time. 
This is the error which will be logged if you try to access the demo server concurrently:
"org.apache.spark.SparkException: Only one SparkContext may be running in this JVM (see SPARK-2243).

To ignore this error, set spark.driver.allowMultipleContexts = true".

