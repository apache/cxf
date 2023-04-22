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

import org.apache.cxf.hello_world_corba.Greeter;
import org.apache.cxf.hello_world_corba.PingMeFault;

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

@jakarta.jws.WebService(portName = "GreeterTimeoutCORBAPort",
        serviceName = "GreeterTimeoutCORBAService",
        targetNamespace = "http://cxf.apache.org/hello_world_corba",
        wsdlLocation = "classpath:/wsdl_systest/hello_world_corba_timeout.wsdl",
        endpointInterface = "org.apache.cxf.hello_world_corba.Greeter")

public class BaseGreeterTimeoutImpl implements Greeter {
    public static final String GREETME_OUT = "test out";
    static final String EX_STRING = "CXF RUNTIME EXCEPTION";

    public String greetMe(String me) {
        String timeout = System.getProperty("jacorb.connection.client.pending_reply_timeout", "0");
        if (timeout != null) {
            try {
                Integer ms = Integer.parseInt(timeout);
                if (ms > 0) {
                    Thread.sleep(ms + 1000);
                }
            } catch (InterruptedException ignore) {
            }
        }
        return "Hello " + me;
    }

    public void greetMeOneWay(String me) {
    }

    public String sayHi() {
        return GREETME_OUT;
    }

    public void pingMe(String faultType) throws PingMeFault {
    }
}
