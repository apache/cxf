**** About this project:

This CXF sandbox project is being used to help implement 
WS-SecurityPolicy and WS-Trust within CXF 2.2.

This project is available from SVN at 
https://svn.apache.org/repos/asf/cxf/sandbox/interopfest.

CXF is presently relying on the test web services used by Microsoft's Web Services
Interoperability Plug-Fest[1].  The WSDL for these services is located at:
http://131.107.72.15/Security_WsSecurity_Service_Indigo/WsSecurity11.svc?wsdl

The main scenario document[2] listed in the second column of the 
table at the top of [1] describes each test case, however, there appears to be no
immediate mapping between test case given in that document and its
corresponding port element in the wsdl:service section of the WSDL above.

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

