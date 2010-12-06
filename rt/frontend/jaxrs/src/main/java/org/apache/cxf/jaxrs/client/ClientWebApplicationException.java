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

package org.apache.cxf.jaxrs.client;

import javax.ws.rs.core.Response;

/**
 * This exception indicates that the problem has occurred on the client side only,
 * possibly as part of processing the successful server response or even before
 * the request has been sent
 */
public class ClientWebApplicationException extends RuntimeException {

    private Response response;
    
    public ClientWebApplicationException() {
        
    }
    
    public ClientWebApplicationException(String message) {
        super(message);
    }
    
    public ClientWebApplicationException(Throwable cause) {
        super(cause);
    }
    
    public ClientWebApplicationException(String message, Throwable cause, Response response) {
        super(message, cause);
        this.response = response;
    }
    
    /**
     * Returns the server response if any, for example, if the problem has 
     * occurred during reading the response then this method will return
     * JAX-RS Response object representing the actual server response 
     * @return server response, can be null
     */
    public Response getResponse() {
        return response;    
    }
}
