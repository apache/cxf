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

import org.xmlsoap.ping.PingResponseBody;

import interopbaseaddress.interop.EchoDataSet.Request;
import interopbaseaddress.interop.EchoDataSetResponse.EchoDataSetResult;
import interopbaseaddress.interop.EchoXmlResponse.EchoXmlResult;
import interopbaseaddress.interop.IPingService;
import interopbaseaddress.interop.PingRequest;
import interopbaseaddress.interop.PingResponse;




/**
 * 
 */
public class PingServiceImpl implements IPingService {

    public String echo(String request) {
        return request;
    }

    public EchoDataSetResult echoDataSet(Request request) {
        // TODO Auto-generated method stub
        return null;
    }

    public EchoXmlResult echoXml(interopbaseaddress.interop.EchoXml.Request request) {
        // TODO Auto-generated method stub
        return null;
    }

    public String fault(String request) {
        // TODO Auto-generated method stub
        return null;
    }

    public String header(String request) {
        // TODO Auto-generated method stub
        return null;
    }

    public PingResponse ping(PingRequest parameters) {
        PingResponse resp = new PingResponse();
        PingResponseBody body = new PingResponseBody();
        body.setOrigin("CXF");
        body.setScenario(parameters.getPing().getScenario());
        body.setText(parameters.getPing().getOrigin() + " : "
                     + parameters.getPing().getText());
        resp.setPingResponse(body);
        return resp;
    }



}
