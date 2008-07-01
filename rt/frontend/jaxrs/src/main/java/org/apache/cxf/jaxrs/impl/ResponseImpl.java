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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public final class ResponseImpl extends Response {
    private final int status;
    private final Object entity;
    private MultivaluedMap<String, Object> metadata;
    
    
    ResponseImpl(int s, Object e) {
        this.status = s;
        this.entity = e;
    }

    public Object getEntity() {
        return entity;
    }

    public int getStatus() {
        return status;
    }

    void addMetadata(MultivaluedMap<String, Object> meta) { 
        this.metadata = meta;
    }
    
    public MultivaluedMap<String, Object> getMetadata() {
        // don't worry about cloning for now
        return metadata;
    }
    
}
