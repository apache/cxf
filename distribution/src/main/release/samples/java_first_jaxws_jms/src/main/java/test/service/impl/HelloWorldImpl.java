package test.service.impl;

import javax.jws.WebService;

import test.service.HelloWorld;

@WebService
public class HelloWorldImpl implements HelloWorld {
  /* (non-Javadoc)
 * @see test.IHello#sayHi(java.lang.String)
 */
public String sayHi(String name) {
    return "Hello " + name;
  }
}