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
package org.apache.cxf.systest.jaxrs;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

@Path("resource")
public class AsyncResource {

    public static final String RESUMED = "Response resumed";
    public static final String FALSE = "A method returned false";
    public static final String TRUE = "A method return true";

    private static final AsyncResponseQueue[] ASYNC_RESPONSES = {
        new AsyncResponseQueue(), new AsyncResponseQueue(), new AsyncResponseQueue() };

    @GET
    @Path("suspend")
    public void suspend(@Suspended AsyncResponse asyncResponse) { 
        ASYNC_RESPONSES[0].add(asyncResponse); 
    }

    @GET
    @Path("suspendthrow")
    public void suspendthrow(@Suspended AsyncResponse asyncResponse) {
        throw new WebApplicationException("Oh Dear", 502);
    }
    
    @GET
    @Path("cancelvoid")
    public String cancel(@QueryParam("stage") String stage) { 
        AsyncResponse response = takeAsyncResponse(stage); 
        boolean ret = response.cancel(); 
        ret &= response.cancel(); 
        addResponse(response, stage); 
        return ret ? TRUE : FALSE; 
    }
    
    @POST
    @Path("resume")
    public String resume(@QueryParam("stage") String stage, String response) { 
        AsyncResponse async = takeAsyncResponse(stage); 
        boolean b = resume(async, response); 
        addResponse(async, stage); 
        return b ? TRUE : FALSE; 
    }
    
    protected static AsyncResponse takeAsyncResponse(String stageId) { 
        return takeAsyncResponse(Integer.parseInt(stageId)); 
    }
    
    protected static AsyncResponse takeAsyncResponse(int stageId) {
        AsyncResponse asyncResponse = null;
        asyncResponse = ASYNC_RESPONSES[stageId].take();
        return asyncResponse;
    }
    
    protected static final void addResponse(AsyncResponse response, String stageId) { 
        int id = Integer.parseInt(stageId) + 1; 
        ASYNC_RESPONSES[id].add(response); 
    }
    
    protected static boolean resume(AsyncResponse takenResponse, Object response) { 
        return takenResponse.resume(response); 
    }
    
    protected static ResponseBuilder createErrorResponseBuilder() { 
        return Response.status(Status.EXPECTATION_FAILED); 
    }
    
    private static class AsyncResponseQueue {
        Queue<AsyncResponse> queue = new ArrayBlockingQueue<AsyncResponse>(1);
    
        public void add(AsyncResponse asyncResponse) {
            queue.add(asyncResponse);
            
        }
    
        public AsyncResponse take() {
            return queue.remove();
        }
        
    }

}
