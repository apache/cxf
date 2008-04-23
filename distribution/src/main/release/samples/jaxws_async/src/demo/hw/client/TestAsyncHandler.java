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

package demo.hw.client;

import javax.xml.ws.AsyncHandler;
import javax.xml.ws.Response;

import org.apache.hello_world_async_soap_http.types.GreetMeSometimeResponse;


public class TestAsyncHandler implements AsyncHandler<GreetMeSometimeResponse> {
    
    private GreetMeSometimeResponse reply;

    public void handleResponse(Response<GreetMeSometimeResponse> response) {
        try {
            System.err.println("handleResponse called");
            reply = response.get();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public String getResponse() {
        return reply.getResponseType();
    }
    
}
