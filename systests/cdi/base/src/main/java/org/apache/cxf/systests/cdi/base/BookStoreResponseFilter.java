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

package org.apache.cxf.systests.cdi.base;

import java.io.IOException;

import jakarta.enterprise.inject.Vetoed;
import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;

@Vetoed
public class BookStoreResponseFilter implements ContainerResponseFilter {
    @Inject private BookStoreAuthenticator authenticator;
    
    @Override
    public void filter(ContainerRequestContext requestContext,
            ContainerResponseContext responseContext) throws IOException {
        if (authenticator != null) {
            // This filter should not be created using CDI runtime (it is vetoed) as 
            // such the injection should not be performed.
            responseContext.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        }
    }
}
