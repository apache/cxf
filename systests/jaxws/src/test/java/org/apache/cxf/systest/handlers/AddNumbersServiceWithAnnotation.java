/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.systest.handlers;

import java.net.MalformedURLException;
import java.net.URL;

import javax.jws.HandlerChain;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import org.apache.handlers.AddNumbers;
/**
 * 
 */

@WebServiceClient(name = "AddNumbersService", 
                  targetNamespace = "http://apache.org/handlers", 
                  wsdlLocation = "file:/D:/svn/cxf/trunk/testutils/src/main/resources/wsdl/addNumbers.wsdl")
@HandlerChain(file = "./handlers_smallnumbers.xml", name = "TestHandlerChain")
public class AddNumbersServiceWithAnnotation extends Service {

    private static final URL WSDL_LOCATION;
    private static final QName SERVICE = new QName("http://apache.org/handlers", "AddNumbersService");
    private static final QName ADDNUMBERS_PORT = new QName("http://apache.org/handlers", "AddNumbersPort");
    
    static {
        URL url = null;
        try {
            url = new URL("file:/D:/svn/cxf/trunk/testutils/src/main/resources/wsdl/addNumbers.wsdl");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public AddNumbersServiceWithAnnotation(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public AddNumbersServiceWithAnnotation() {
        super(WSDL_LOCATION, SERVICE);
    }

    /**
     * 
     * @return
     *     returns AddNumbersPort
     */
    @WebEndpoint(name = "AddNumbersPort")
    public AddNumbers getAddNumbersPort() {
        return (AddNumbers)super.getPort(ADDNUMBERS_PORT, AddNumbers.class);
    }

}
