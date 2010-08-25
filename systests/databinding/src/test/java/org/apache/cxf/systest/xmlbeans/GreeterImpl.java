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

package org.apache.cxf.systest.xmlbeans;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.helloWorldSoapHttp.xmlbeans.types.FaultDetailDocument;
import org.apache.helloWorldSoapHttp.xmlbeans.types.FaultDetailDocument.FaultDetail;
import org.apache.hello_world_soap_http.xmlbeans.GreetMeFault;
import org.apache.hello_world_soap_http.xmlbeans.Greeter;
import org.apache.hello_world_soap_http.xmlbeans.PingMeFault;

@javax.jws.WebService(portName = "SoapPort", serviceName = "SOAPService", 
                      targetNamespace = "http://apache.org/hello_world_soap_http/xmlbeans", 
                      endpointInterface = "org.apache.hello_world_soap_http.xmlbeans.Greeter")
public class GreeterImpl implements Greeter {

    private static final Logger LOG = LogUtils.getL7dLogger(GreeterImpl.class);        
    
    /* (non-Javadoc)
     * @see org.apache.hello_world_soap_http.Greeter#greetMe(java.lang.String)
     */
    public String greetMe(String me) throws GreetMeFault {
        if ("fault".equals(me)) {
            org.apache.helloWorldSoapHttp.xmlbeans.types.GreetMeFaultDetailDocument detail
                = org.apache.helloWorldSoapHttp.xmlbeans.types.GreetMeFaultDetailDocument.Factory
                .newInstance();
            detail.setGreetMeFaultDetail("Some fault detail");
            throw new GreetMeFault("Fault String", detail);
        }
        LOG.info("Executing operation greetMe");        
        return "Hello " + me;
    }
    
    /* (non-Javadoc)
     * @see org.apache.hello_world_soap_http.Greeter#greetMeOneWay(java.lang.String)
     */
    public void greetMeOneWay(String me) {
        LOG.info("Executing operation greetMeOneWay");        
    }

    /* (non-Javadoc)
     * @see org.apache.hello_world_soap_http.Greeter#sayHi()
     */
    public String sayHi() {
        LOG.info("Executing operation sayHi");        
        return "Bonjour";
    }
    
    public void pingMe() throws PingMeFault {
        // here we need to put the FaultDetail into the FaultDetailDocument
        FaultDetailDocument faultDocument = 
            org.apache.helloWorldSoapHttp.xmlbeans.types.FaultDetailDocument.Factory.newInstance();        
        FaultDetail faultDetail = faultDocument.addNewFaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        LOG.info("Executing operation pingMe, throwing PingMeFault exception");        
        throw new PingMeFault("PingMeFault raised by server", faultDocument);
    }

    
}
