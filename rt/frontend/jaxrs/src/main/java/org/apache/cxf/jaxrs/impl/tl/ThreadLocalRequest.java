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

package org.apache.cxf.jaxrs.impl.tl;

import java.util.Date;
import java.util.List;

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Variant;

public class ThreadLocalRequest extends AbstractThreadLocalProxy<Request> 
    implements Request {

    public ResponseBuilder evaluatePreconditions(EntityTag eTag) {
        return get().evaluatePreconditions(eTag);
    }

    public ResponseBuilder evaluatePreconditions(Date lastModified) {
        return get().evaluatePreconditions(lastModified);
    }

    public ResponseBuilder evaluatePreconditions(Date lastModified, EntityTag eTag) {
        return get().evaluatePreconditions(lastModified, eTag);
    }

    public Variant selectVariant(List<Variant> vars) throws IllegalArgumentException {
        return get().selectVariant(vars);
    }

    public String getMethod() {
        return get().getMethod();
    }

}
