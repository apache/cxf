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
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.ext.Provider;

import org.apache.cxf.jaxrs.utils.JAXRSUtils;

@Provider
public class PathSegmentImpl implements PathSegment {

    private String path;
    private boolean decode;
    
    public PathSegmentImpl(String path) {
        this(path, true);
    }
    
    public PathSegmentImpl(String path, boolean decode) {
        this.path = path;
        this.decode = decode;
    }
    
    public MultivaluedMap<String, String> getMatrixParameters() {
        return JAXRSUtils.getMatrixParams(path, decode);
    }

    public String getPath() {
        return path;
    }

}
