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
package interop.server;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;

// import com.iona.cxf.security.credential.InCredentialsMap;
import interopbaseaddress.interop.IPingService;

public abstract class PingServiceBase implements IPingService {
    
    @Resource
    protected WebServiceContext ctx;

    protected PingServiceBase() {
    }

    /*
    protected static InCredentialsMap
    getInCredentialsMap(
        final WebServiceContext ctx
    ) {
        return (InCredentialsMap) ctx.getMessageContext().get(
            InCredentialsMap.class.getName()
        );
    }
    */

    public interopbaseaddress.interop.EchoXmlResponse.EchoXmlResult 
    echoXml(
        interopbaseaddress.interop.EchoXml.Request request
    ) {
        throw new RuntimeException("Unimplemented");
    }

    public java.lang.String 
    echo(
        java.lang.String request
    ) {
        System.out.println("ping(" + request + ")");
        return request;
    }

    public java.lang.String 
    fault(
        java.lang.String request
    ) {
        throw new RuntimeException("Unimplemented");
    }

    public interopbaseaddress.interop.PingResponse 
    ping(
        interopbaseaddress.interop.PingRequest parameters
    ) {
        throw new RuntimeException("Unimplemented");
    }

    public String header(
        String parameters
    ) {
        throw new RuntimeException("Unimplemented");
    }

    public interopbaseaddress.interop.EchoDataSetResponse.EchoDataSetResult 
    echoDataSet(
        interopbaseaddress.interop.EchoDataSet.Request request
    ) {
        throw new RuntimeException("Unimplemented");
    }
}
