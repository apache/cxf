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


package org.apache.cxf.systest.ws.rm;

import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.Endpoint;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;

@WebService(serviceName = "ControlService", 
            portName = "ControlPort", 
            endpointInterface = "org.apache.cxf.greeter_control.Control", 
            targetNamespace = "http://cxf.apache.org/greeter_control")
public class ControlImpl  extends org.apache.cxf.greeter_control.ControlImpl {
    
    private static final Logger LOG = LogUtils.getLogger(ControlImpl.class);

    @Override
    public boolean startGreeter(String cfgResource) {
        String derbyHome = System.getProperty("derby.system.home"); 
        try {
            System.setProperty("derby.system.home", derbyHome + "-server");   
            SpringBusFactory bf = new SpringBusFactory();
            greeterBus = bf.createBus(cfgResource);
            BusFactory.setDefaultBus(greeterBus);
            LOG.info("Initialised bus " + greeterBus + " with cfg file resource: " + cfgResource);
            LOG.fine("greeterBus inInterceptors: " + greeterBus.getInInterceptors());

            Interceptor logIn = new LoggingInInterceptor();
            Interceptor logOut = new LoggingOutInterceptor();
            greeterBus.getInInterceptors().add(logIn);
            greeterBus.getOutInterceptors().add(logOut);
            greeterBus.getOutFaultInterceptors().add(logOut);

            Endpoint.publish(address, implementor);
            LOG.info("Published greeter endpoint.");
        } finally {
            if (derbyHome != null) {
                System.setProperty("derby.system.home", derbyHome);
            } else {
                System.clearProperty("derby.system.home");
            }
        }
        
        return true;        
    }
    
}
