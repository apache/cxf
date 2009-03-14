*** About these samples ***

This CXF sample is being used to help implement WS-SecurityPolicy,
WS-SecureConversation, and WS-Trust within CXF.

CXF is presently relying on the test web services used by Microsoft's Web Services
Interoperability Plug-Fest[1].  The main scenario document[2] listed in the second 
column of the table at the top of [1] describes each test case.

[1] http://mssoapinterop.org/ilab/
[2] http://131.107.72.15/ilab/WSSecurity/WCFInteropPlugFest_Security.doc


*** Requirements ***

The samples in this directory use STRONG encryption.  The default encryption algorithms 
included in a JRE is not adequate for these samples.   The Java Cryptography Extension 
(JCE) Unlimited Strength Jurisdiction Policy Files available on Sun's JDK download 
page[3] *must* be installed for the examples to work.   If you get errors about invalid
key lengths, the Unlimited Strength files are not installed.

[3] http://java.sun.com/javase/downloads/index.jsp


*** Running the WS-Security samples against the Microsoft servers ***

Each of the samples are setup to test a CXF client against the public servers provided
by Microsoft.    To build and run the client, run:

mvn -Pclient

This will download the WSDL and Certs from Microsoft, build the code, and runs the client 
testing all the known working test cases.   If you want to run a specific test, run:

mvn -Pclient -Dtest.method=XX

Where the range of values for test.method is defined in the Client.java file of that project.

In particular, test.method will be the prefix to the web service port being used (see each 
wsdl:port under the wsdl:service element in the WSDL above).  So -Dtest.method=UX will 
call the UX_IPingService listed in the WSDL.


*** Running the WS-Security samples against the local CXF servers ***

The wssec10, wssec11, and wssc samples contain working CXF servers.  (The WS-Trust 
samples do not yet have working servers.)  To start the server, just run:

mvn -Pserver


To run the CXF clients agains the local server, run:

mvn -Pclient -Dtest.server=local

