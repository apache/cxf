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

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;

@WebServiceClient(name = "AddNumbersService", 
                  wsdlLocation = "/wsdl/addNumbers.wsdl",
                  targetNamespace = "http://apache.org/handlers") 
public class AddNumbersServiceUnwrap extends Service {

    public static final URL WSDL_LOCATION = null;
    public static final QName SERVICE = new QName("http://apache.org/handlers",
                                                  "AddNumbersService");
    public static final QName ADD_NUMBERS_PORT = new QName("http://apache.org/handlers",
                                                         "AddNumbersPort");


    public AddNumbersServiceUnwrap(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public AddNumbersServiceUnwrap(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public AddNumbersServiceUnwrap() {
        super(WSDL_LOCATION, SERVICE);
    }
    

    /**
     * 
     * @return
     *     returns AddNumbers
     */
    @WebEndpoint(name = "AddNumbersPort")
    public AddNumbersUnwrap getAddNumbersPort() {
        return super.getPort(ADD_NUMBERS_PORT, AddNumbersUnwrap.class);
    }

    /**
     * 
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the 
     *     proxy.  Supported features not in the <code>features</code> parameter 
     *     will have their default values.
     * @return
     *     returns AddNumbers
     */
    @WebEndpoint(name = "AddNumbersPort")
    public AddNumbersUnwrap getAddNumbersPort(WebServiceFeature... features) {
        return super.getPort(ADD_NUMBERS_PORT, AddNumbersUnwrap.class, features);
    }

}
