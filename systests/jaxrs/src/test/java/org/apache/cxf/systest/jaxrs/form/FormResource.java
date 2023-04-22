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
package org.apache.cxf.systest.jaxrs.form;

import java.util.logging.Logger;

import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;
import org.apache.cxf.common.logging.LogUtils;

@Path("/form")
public class FormResource {
    private static final Logger LOG = LogUtils.getL7dLogger(FormResource.class);

    @POST
    public Response processForm(@FormParam("value") String value, Form form) {
        String fromForm = form.asMap().getFirst("value");
        LOG.info("FromFormParam: " + value);
        LOG.info("FromForm: " + fromForm);
        return Response.ok()
                        .header("FromFormParam", value)
                        .header("FromForm", fromForm)
                        .build();
    }
}
