JAX-RS Spring Security Demo 
===========================

The demo shows how to use Spring Security to secure a JAXRS-based RESTful service.
 
Two approaches toward securing a service are shown :
- using Spring Security @Secured annotations
- using AspectJ pointcut expressions

Additionally, JAXRS annotations inheritance is demonstrated, from both interface 
and abstract class definitions.


Building and running the demo using Maven
-----------------------------------------

From the base directory of this sample (i.e., where this README file is
located), the maven pom.xml file can be used to build and run the demo. 

Using either UNIX or Windows:

  mvn clean install
  mvn -Pserver  (from one command line window)
  mvn -Pclient  (from a second command line window)

To remove the target directory, run "mvn clean".


What happens when the demo is run
---------------------------------

The demo web application located in a webapp folder is configured for two users, Fred and Bob, 
to be able to access various methods of a customer service bean. 

Fred is in both ROLE_CUSTOMER and ROLE_ADMIN roles, while Bob is in the ROLE_CUSTOMER role only.
After the server starts, the client is run and it's shown that Fred can access all the methods
while Bob can access only those which ROLE_CUSTOMER users are permitted to. 

By default, the demo is configured to use AspectJ pointcut expressions to apply ACL rules to a service bean.
Please see src/main/webapp/WEB-INF/beans.xml as well as src/demo/jaxrs/service.

demo.jaxrs.service.CustomerServiceImpl bean implements the CustomerService interface. AspectJ 
expressions are applied to interface methods. Note neither CustomerService interface nor 
its CustomerServiceImpl implementation have security-specific annotations. CustomerService
interface does have JAXRS annotations which are inherited by the service bean.

To see the @Secured annotations in action, please uncomment 

<bean id="customerservice" class="demo.jaxrs.service.CustomerServiceSecuredImpl"/>

and comment the one used by default:

<bean id="customerservice" class="demo.jaxrs.service.CustomerServiceImpl"/>

Note this time @Secured annotations are coming from a CustomerServiceSecured interface,
while JAXRS annotations are inherited from AbstractCustomerServiceSecured class. Also
the secure annotations have to be explictly enabled in the configuration:

<security:global-method-security secured-annotations="enabled"/>

Basic authentication is used to provide user credentials to a service. 
For simplicity, the HTTPS protocol is avoided in this sample but should be used
in production.


