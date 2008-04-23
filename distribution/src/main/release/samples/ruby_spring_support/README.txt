Ruby script demo

=================

This example shows how to create ruby web service 
implemented with Spring. 
You'll learn how to write a simple ruby script web service. 

For more information
see the documentation for this example in the
user's guide.



Prerequisite
------------

If your environment already includes cxf-manifest.jar on the
CLASSPATH, and the JDK and ant bin directories on the PATH
it is not necessary to set the environment as described in
the samples directory's README.  If your environment is not
properly configured, or if you are planning on using wsdl2java,
javac, and java to build and run the demos, you must set the
environment.





Some third-party jars need to be installed to make this sample working. 


You can run the following command to get these third-party jars

:  

ant get.dep




Building and running the demo using Ant

---------------------------------------


From the base directory of this sample (i.e., where this README file is
located), the Ant build.xml file can be used to build and run the demo. 
The server and client targets automatically build the demo.

Using either UNIX or Windows:

  ant server  (from one command line window)
  ant client  (from a second command line window)


To remove the code generated from the WSDL file and the .class
files, run "ant clean".