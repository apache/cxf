Basic Setup for Building and Running the performance test case 
==============================================

As described in the installation notes, extract the cxf
binary distribution archive into an installation directory
under the root drive.  This creates the sub-directory build,
which includes all of the product directories.

1. Setup the build environment

To build and run the performance test case , you must install 
the J2SE Development Kit (JDK) 5.0 or later.

The performance test cases in the /bin folder under each test require 
Apache Ant, V1.6 or later.

The CXF_HOME system variable needs to be set to the cxf binary install
directory.


2. Build the performance test case
There are two types of test case in the performance test case
directory.  The base directory provide a simple testcase base class for
the client to calculate the server response time and throughput. There
you should build the base directory first, and then build the 
other directory files. 		
  
   cd base
   ant
   cd ../soap_http_doc_lit (the best one, but /basic_type, /complex_type also available)
   ant

3. To run the performance tests
 
You can cd to soap_http_doc_lit/bin to run the test
run_server and run_server.bat just startup the server
run_client and run_client.bat can take these argument:
    -Operation  to invoke the wsdl defined operation
    -BasedOn Time   setup the invoking count with time
    -Amount   define the invoke times , if based on time it means second
    -PacketSize  define the packet size which client send to server
    -Threads   define the number of threads to run the perform test
               For soap_http_doc_lit, can be a range or comma separated 
               list to run multiple time  with different thread counts.   For example: 
  	           -Threads 1-4,6,8,10 
    -WSDL      wsdl location (defaults to the perf.wsdl)
    -BuildFile location of alternative build.xml file (mostly for running clients/servers)
	
You can alternatively run them directly using the build.xml file in each
test subdirectory (view the contents of the run_xxx(.bat) files 
for the format of those calls--they call the Ant build file directly.)
