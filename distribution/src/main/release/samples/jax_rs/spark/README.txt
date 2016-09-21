JAX-RS Spark Streaming Demo 
===========================

This demo demonstrates how to connect HTTP and Spark streams with JAX-RS.
The demo accept simple Strings or binary attachments which are processed with Tika.
In both cases a list of strings is pushed into a Spark Streaming Pipeline with the 
pipeline output response streamed back to the HTTP client.

Build the demo with "mvn install" and start it with

mvn exec:java -Dexec.mainClass=demo.jaxrs.server.simple.Server
(uses Spark Receiver initialized with a list of strings)

or 

mvn exec:java -Dexec.mainClass=demo.jaxrs.server.simple.Server -DexecArgs=-receiverType=queue
(Uses Spark Queue Receiver initialized with a parallelized data set)

In both cases a new streaming context is created on every request. 

You cam also try: 

mvn exec:java -Dexec.mainClass=demo.jaxrs.server.socket.Server

(Uses a client socket receiver - JAX-RS server will push a list of strings to it 
and will write down the response data it gets back)


Next do: 

1. Simple text processing:

curl -X POST -H "Accept: text/plain" -H "Content-Type: text/plain" -d "Hello Spark" http://localhost:9000/spark/stream

2. PDF/ODT/ODP processing:

Open multipart.html located in src/main/resources, locate any PDF or OpenOffice text or presentation file available 
on the local disk and upload.

Note Spark restricts that only a single streaming context can be active in JVM at a given moment of time. 
This is the error which will be logged if you try to access the demo server concurrently:
"org.apache.spark.SparkException: Only one SparkContext may be running in this JVM (see SPARK-2243).

To ignore this error, set spark.driver.allowMultipleContexts = true".

