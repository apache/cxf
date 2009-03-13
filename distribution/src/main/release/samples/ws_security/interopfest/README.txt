About these samples:

This CXF sample is being used to help implement WS-SecurityPolicy,
WS-SecureConversation, and WS-Trust within CXF.

CXF is presently relying on the test web services used by Microsoft's Web Services
Interoperability Plug-Fest[1].  The main scenario document[2] listed in the second 
column of the table at the top of [1] describes each test case.

[1] http://mssoapinterop.org/ilab/
[2] http://131.107.72.15/ilab/WSSecurity/WCFInteropPlugFest_Security.doc


**** Running the WS-Security samples:

Since these projects rely on the SNAPSHOT (2.2) version of CXF, you'll need 
to check out CXF from svn first and build[3] the CXF project. 

The wssec11 project can be run by entering mvn clean install from the
wssec11 folder, and then entering the following command:

mvn -Pclient -Dtest.method=XX

Where the range of values for test.method is defined in the Client.java file of that project.

In particular, test.method will be the prefix to the web service port being used (see each 
wsdl:port under the wsdl:service element in the WSDL above).  So -Dtest.method=UX will 
call the UX_IPingService listed in the WSDL.

SOAP clients for these web services are in the process of being developed and tested, but not 
yet CXF-based web service providers implementing these services.  Help is most welcome.

Note:  The Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files 
available on Sun's JDK download page[4] *must* be installed for the examples to work.

[3] http://cxf.apache.org/building.html
[4] http://java.sun.com/javase/downloads/index.jsp

