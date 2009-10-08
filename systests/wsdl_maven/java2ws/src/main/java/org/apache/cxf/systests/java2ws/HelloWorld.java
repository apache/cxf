package org.apache.cxf.systests.java2ws;

import javax.jws.WebService;

@WebService
public interface HelloWorld {
    String sayHi(String text);
}

