WS-RM Demo
==========

This demo shows how WS-ReliableMessaging support in Apache CXF may be enabled.  

The client and server both apply the reliableMessaging feature to the bus.
This ensures installation of the WS-RM interceptors, comprising logical
interceptors (RMInInterceptor/RMOutInterceptor) responsible for managing the
reliability properties of the current message, and a protocol interceptor
(RMSoapInterceptor) responsible for encoding/decoding these properties as SOAP
Headers.

As WS-RM is dependent on WS-Addressing, the demo uses the same approach as the
ws_addressing sample to enable this functionality. However, you may notice
that the WS-Addressing namespace URI is different in this case (i.e.
http://schemas.xmlsoap.org/ws/2004/08/addressing as opposed to
http://www.w3.org/2005/08/addressing). This is because the WS-RM specification
is still based on an older version of WS-Addressing.

The logging feature is used to log the inbound and outbound SOAP messages and
display these to the console. Notice the usage of out-of-band RM protocol
messages (CreateSequence and CreateSequenceResponse) and the WS-RM headers in
application-level messages (Sequence, SequenceAcknowledgement, AckRequested
etc.)  

Finally, the MessageLossSimulator interceptor is installed on the client-side
to simulate message loss by discarding every second application level message.
This simulated unreliability allows the retransmission of unacknowledged
messages to be observed.

This demo also illustrates usage of the decoupled HTTP transport, whereby a
separate server->client HTTP connection is used to deliver responses to
(application or RM protocol) requests and server side originated standalone
acknowledgments.

The "partial response" referred to in the log output is the payload of the
HTTP 202 Accepted response sent on the back-channel of the original 
client->server connection. 

In all other respects this demo is based on the basic hello_world sample,
illustrating that WS-Addressing usage is independent of the application. One
notable addition to the familiar hello_world WSDL is the usage of the
<wsaw:UsingAddressing> extension element to indicate the WS-Addressing support
is enabled for the service endpoint.

Please review the README in the samples directory before continuing.


Building and running the demo using Maven
-----------------------------------------
From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn install (builds the demo)
  mvn -Pserver  (from one command line window)
  Mvn -Pclient  (from a second command line window)

On startup, the client makes a sequence of 4 oneway invocations.
The output of the logging interceptors will show that only the 1st and 3rd
reach their destination. Notice how after approximately 2 seconds the
messages that actually have arrived at the server will be acknowledged,
and how after approximately 4 seconds the client will resend the 2nd and 4th
application message. These will be acknowledged another 2 seconds
later so that there will be no further retransmissions from the client.

To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

