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
package org.apache.cxf.jaxrs.impl;

import java.util.Date;

import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;

public class SimpleService {

    private EntityTag tag;
    private Date modified;

    public Response perform(final Request request) {
        final Date lastModified = getLastModified();
        final EntityTag entityTag = getEntityTag();
        ResponseBuilder rb = request.evaluatePreconditions(lastModified, entityTag);
        if (rb == null) {
            rb = Response.ok();
        } else {
            // okay
        }
        return rb.build();
    }

    private EntityTag getEntityTag() {
        return tag;
    }

    private Date getLastModified() {
        return modified;
    }

    public void setEntityTag(final EntityTag value) {
        tag = value;
    }

    public void setLastModified(final Date value) {
        modified = value;
    }

}
