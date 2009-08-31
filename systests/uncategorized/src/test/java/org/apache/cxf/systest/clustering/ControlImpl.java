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


package org.apache.cxf.systest.clustering;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Response;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.Control;
import org.apache.cxf.greeter_control.Greeter;
import org.apache.cxf.greeter_control.types.FaultLocation;
import org.apache.cxf.greeter_control.types.StartGreeterResponse;
import org.apache.cxf.greeter_control.types.StopGreeterResponse;


@WebService(serviceName = "ControlService", 
            portName = "ControlPort", 
            endpointInterface = "org.apache.cxf.greeter_control.Control", 
            targetNamespace = "http://cxf.apache.org/greeter_control")
public class ControlImpl implements Control {
    
    private static final Logger LOG = LogUtils.getLogger(ControlImpl.class);
    
    private Map<String, Greeter> implementors; 
    private Map<String, Endpoint> endpoints;
    
    ControlImpl() {
        implementors = new HashMap<String, Greeter>();
        implementors.put(FailoverTest.REPLICA_A, new GreeterImplA());
        implementors.put(FailoverTest.REPLICA_B, new GreeterImplB());
        implementors.put(FailoverTest.REPLICA_C, new GreeterImplC());
        implementors.put(FailoverTest.REPLICA_D, new GreeterImplD());
        endpoints = new HashMap<String, Endpoint>();
    }
    
    public boolean startGreeter(String address) {
        endpoints.put(address,
                      Endpoint.publish(address, implementors.get(address)));
        LOG.info("Published greeter endpoint on: " + address);
        return true;        
    }

    public boolean stopGreeter(String address) {  
        Endpoint endpoint = endpoints.get(address);
        if (null != endpoint) {
            LOG.info("Stopping Greeter endpoint on: " + address);
            endpoint.stop();
        } else {
            LOG.info("No endpoint active for: " + address);
        }
        endpoint = null;
        return true;
    }
    
    //--Irrelevant Boilerplate

    public void setFaultLocation(FaultLocation fl) {
        // never called
    }
    
    public Future<?> startGreeterAsync(String requestType, AsyncHandler<StartGreeterResponse> asyncHandler) {
        // never called
        return null;
    }

    public Response<StartGreeterResponse> startGreeterAsync(String requestType) {
        // never called
        return null;
    }

    public Response<StopGreeterResponse> stopGreeterAsync(String requestType) {
        // never called
        return null;
    }

    public Future<?> stopGreeterAsync(String requestType,
                                      AsyncHandler<StopGreeterResponse> asyncHandler) {
        // never called
        return null;
    }

    public Response<?> setFaultLocationAsync(FaultLocation in) {
        // TODO Auto-generated method stub
        return null;
    }

    public Future<?> setFaultLocationAsync(FaultLocation in, AsyncHandler<?> asyncHandler) {
        // TODO Auto-generated method stub
        return null;
    }
}
