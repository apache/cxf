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

import java.util.concurrent.Future;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.greeter_control.PingMeFault;
import org.apache.cxf.greeter_control.types.FaultDetail;
import org.apache.cxf.greeter_control.types.GreetMeResponse;
import org.apache.cxf.greeter_control.types.PingMeResponse;
import org.apache.cxf.greeter_control.types.SayHiResponse;
import org.apache.cxf.interceptor.Fault;


@WebService(serviceName = "GreeterService",
            portName = "GreeterPort",
            endpointInterface = "org.apache.cxf.greeter_control.Greeter",
            targetNamespace = "http://cxf.apache.org/greeter_control")
public class GreeterImpl11 {
    private static final Logger LOG = LogUtils.getLogger(GreeterImpl11.class);

    public String greetMe(String me) {
        return "Hello " + me;
    }

    public String sayHi() {
        // throw the exception out with some cause
        Exception cause = new IllegalArgumentException("Get a wrong name <sayHi>"
                                                       , new NullPointerException("Test cause."));
        cause.fillInStackTrace();
        throw new Fault("sayHiFault", LOG, cause);
    }

    public void greetMeOneWay(String requestType) {
        //System.out.println("*********  greetMeOneWay: " + requestType);
    }

    public void pingMe() throws PingMeFault {
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        LOG.info("Executing operation pingMe, throwing PingMeFault exception");
        //System.out.println("Executing operation pingMe, throwing PingMeFault exception\n");
        throw new PingMeFault("PingMeFault raised by server", faultDetail);        
    }

    public Future<?> greetMeAsync(String requestType, AsyncHandler<GreetMeResponse> asyncHandler) {
        return null;
        /*not called */
    }

    public Response<GreetMeResponse> greetMeAsync(String requestType) {
        return null;
        /*not called */
    }

    public Future<?> sayHiAsync(AsyncHandler<SayHiResponse> asyncHandler) {
        return null;
        /*not called */
    }

    public Response<SayHiResponse> sayHiAsync() {
        return null;
        /*not called */
    }

    public Response<PingMeResponse> pingMeAsync() {
        return null;
    }
    
    public Future<?> pingMeAsync(AsyncHandler<PingMeResponse> asyncHandler) {
        return null;
    }

}
