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
package org.apache.cxf.systest.corba;

import java.util.ResourceBundle;

import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.hello_world_corba.Greeter;
import org.apache.cxf.hello_world_corba.PingMeFault;
import org.apache.cxf.hello_world_corba.types.FaultDetail;
import org.apache.cxf.interceptor.Fault;
import org.junit.Assert;

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

@javax.jws.WebService(portName = "GreeterCORBAPort", 
        serviceName = "GreeterCORBAService",
        targetNamespace = "http://cxf.apache.org/hello_world_corba",
        wsdlLocation = "classpath:/wsdl_systest/hello_world_corba.wsdl",
        endpointInterface = "org.apache.cxf.hello_world_corba.Greeter")
        
public class BaseGreeterImpl extends Assert implements Greeter {
    public static final String GREETME_IN = "test in";
    public static final String GREETME_OUT = "test out";
    static final String EX_STRING = "CXF RUNTIME EXCEPTION";

    public String greetMe(String me) {
        return "Hello " + me;
    }

    public void greetMeOneWay(String me) {
        assertEquals("William", me);
    }

    public String sayHi() {
        return GREETME_OUT;
    }

    public void pingMe(String faultType) throws PingMeFault {
        if ("USER".equals(faultType)) {

            FaultDetail detail = new FaultDetail();
            detail.setMajor((short)1);
            detail.setMinor((short)2);
            throw new PingMeFault("USER FAULT TEST", detail);
        } else if ("SYSTEM".equals(faultType)) {
            throw new Fault(new Message(EX_STRING, (ResourceBundle)null,
                    new Object[]{"FAULT TEST"}));
        } else {
            throw new IllegalArgumentException(EX_STRING);
        }
    }
}
