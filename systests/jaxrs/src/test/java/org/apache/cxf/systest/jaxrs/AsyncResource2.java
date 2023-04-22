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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;

@Path("resource2")
public class AsyncResource2 {
    private volatile AsyncResponse asyncResponse;

    @GET
    @Path("/suspend")
    public void getSuspendResponse(@Suspended AsyncResponse async) {
        asyncResponse = async;
    }

    @GET
    @Path("/setTimeOut")
    public boolean setTimeOut() {
        int i = 0;
        while (asyncResponse == null && ++i < 20) {
            LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(10));
        }

        if (asyncResponse == null) {
            throw new InternalServerErrorException("Unable to retrieve the AsyncResponse, was it suspended?");
        } 

        return asyncResponse.setTimeout(2L, TimeUnit.SECONDS);
    }
}
