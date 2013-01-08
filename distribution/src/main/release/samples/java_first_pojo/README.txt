Java First POJO DEMO
====================

This demo illustrates how to develop a service using the "code first", pojo
based approach. This demo uses the JAXB Data binding by default, but you can
use Aegis Data binding by uncommenting the four lines below:

(Client.java)
//import org.apache.cxf.aegis.databinding.AegisDatabinding;
//factory.getServiceFactory().setDataBinding(new AegisDatabinding());

(Server.java)
//import org.apache.cxf.aegis.databinding.AegisDatabinding;
//svrFactory.getServiceFactory().setDataBinding(new AegisDatabinding());


Building and running the demo using Maven
---------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the pom.xml file is used to build and run the demo. 

Using either UNIX or Windows:

  mvn install   (builds the demo)
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)


To remove the code generated from the WSDL file and the .class
files, run "mvn clean".

