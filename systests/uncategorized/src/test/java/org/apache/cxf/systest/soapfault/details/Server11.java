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

package org.apache.cxf.systest.soapfault.details;

import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server11 extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(Server11.class);


    protected void run()  {
        Object implementor = new GreeterImpl11();
        String address = "http://localhost:"
            + PORT + "/SoapContext/GreeterPort";
        // enable the options of stack trace and the exception cause message
        Map<String, Object> properties = new HashMap<>();
        properties.put("exceptionMessageCauseEnabled", "true");
        properties.put("faultStackTraceEnabled", "true");
        JaxWsServerFactoryBean factory = new JaxWsServerFactoryBean();
        factory.setAddress(address);
        factory.setServiceBean(implementor);
        factory.setProperties(properties);
        factory.create();
    }


    public static void main(String[] args) {
        try {
            Server11 s = new Server11();
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
