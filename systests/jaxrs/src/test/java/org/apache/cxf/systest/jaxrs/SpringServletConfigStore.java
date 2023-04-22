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

import jakarta.servlet.ServletConfig;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import org.springframework.web.context.ServletConfigAware;

@Path("servlet")
public class SpringServletConfigStore implements ServletConfigAware {
    private ServletConfig servletConfig;
    public void setServletConfig(ServletConfig sc) {
        this.servletConfig = sc;
    }

    @GET
    @Produces("text/plain")
    @Path("config/{name}")
    public String getServletConfigInitParam(@PathParam("name") String name) {
        return servletConfig.getInitParameter(name);
    }

    @GET
    @Produces("text/plain")
    @Path("config/query")
    public String getServletConfigInitParamQuery(@QueryParam("name") String name) {
        return servletConfig.getInitParameter(name);
    }
}
