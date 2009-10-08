Basic Setup for Building and Running the performance test case 
==============================================

As described in the installation notes, extract the cxf
binary distribution archive into an installation directory
under the root drive.  This creates the sub-directory build,
which includes all of the product directories.

1. setup the build Enviroment

To build and run the performance test case , you must install 
the J2SE Development Kit (JDK) 5.0

If you want to use ant to build and run the performance test case,
you must install the Apache ant 1.6 build utility.

The CXF_HOME system variable need to be set to the cxf binary install
directory.


 2. Build the performance test case
There are two types of test case in the performance test case
directory.  The base directory provide a simple testcase base class for
the client to calculate the server reponse time and throughput. There
for you should build the base directory first, and then build the 
 othere directory files. 		
  
   cd base
   ant
   cd ../soap_http_doc_lit
   ant

 3. to run the performance test 
You can cd to soap_http_doc_lit/bin to run the test
run_server and run_server.bat just startup the server
run_client and run_client.bat can take these argument:
    -Operation  to invoke the wsdl defined operation
    -BasedOn Time   setup the invoking count with time
    -Amount   define the invoke times , if based on time it means second
    -PacketSize  define the packet size which client send to server
    -Threads   define the thread number to run the perform test
               Can be a range or comma separated list to run multiple time
	       with different thread counts.   For example:
	       -Threads 1-4,6,8,10 
    -WSDL      wsdl location (defaults to the perf.wsdl)
    -BuildFile location of alternative build.xml file (mostly for running clients/servers)
	
