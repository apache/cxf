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

import java.util.List;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.message.Message;
import org.apache.cxf.systests.cdi.base.context.CustomContext;

@RequestScoped
public class BookStoreVersion {
    @Inject
    private String version;
    @Inject
    private HttpHeaders httpHeaders;
    @Inject
    private CustomContext customContext;
    @GET
    public Response getVersion() {
        List<MediaType> mediaTypeList = httpHeaders.getAcceptableMediaTypes();

        String requestMethod = (String)customContext.getMessage().get(Message.HTTP_REQUEST_METHOD);
        String pathInfo = (String)customContext.getMessage().get(Message.PATH_INFO);

        return Response.ok(version, mediaTypeList.get(0))
                .header(Message.HTTP_REQUEST_METHOD, requestMethod)
                .header(Message.PATH_INFO, pathInfo)
                .build();
    }
}
