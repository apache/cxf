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

package org.apache.cxf.systest.jaxrs.cors;

import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.apache.cxf.jaxrs.cors.CrossOriginInputFilter;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 
 */
public class ConfigServer implements ApplicationContextAware {
    private ApplicationContext appContext;

    @POST
    @Consumes("application/json")
    @Path("/setOriginList")
    @Produces("text/plain")
    public String setOriginList(String[] origins) {
        CrossOriginInputFilter inputFilter = appContext
            .getBean("cors-input", org.apache.cxf.jaxrs.cors.CrossOriginInputFilter.class);
        if (origins == null || origins.length == 0) {
            inputFilter.setAllowAllOrigins(true);
        } else {
            inputFilter.setAllowAllOrigins(false);
            inputFilter.setAllowedOrigins(Arrays.asList(origins));
        }
        return "ok";
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        appContext = context;
    }

}
