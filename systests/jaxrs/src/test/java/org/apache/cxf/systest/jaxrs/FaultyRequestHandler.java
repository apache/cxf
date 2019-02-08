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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;

public class FaultyRequestHandler implements ContainerRequestFilter {

    @Context
    private UriInfo uriInfo;

    @Override
    public void filter(ContainerRequestContext context) {
        if (uriInfo.getPath().endsWith("/propogateExceptionVar/1")) {
            MultivaluedMap<String, String> vars = uriInfo.getPathParameters();
            if (vars.size() == 1
                && vars.get("i") != null
                && vars.get("i").size() == 1
                && "1".equals(vars.getFirst("i"))) {

                JAXRSUtils.getCurrentMessage().getExchange()
                    .put("org.apache.cxf.systest.for-out-fault-interceptor", Boolean.TRUE);
                throw new RuntimeException();
            }
        }
    }

}
