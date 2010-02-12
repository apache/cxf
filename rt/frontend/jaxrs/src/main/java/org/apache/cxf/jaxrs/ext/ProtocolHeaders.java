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
package org.apache.cxf.jaxrs.ext;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

/**
 * An injectable interface that provides access to protocol headers
 *
 */
public interface ProtocolHeaders {
    
    /**
     * Get the value of a request header.
     * @param name the header name, case insensitive
     * @return the header value
     */
    String getRequestHeaderValue(String name);
    
    /**
     * Get the values of a request header.
     * @param name the header name, case insensitive
     * @return a read-only list of header values.
     */
    List<String> getRequestHeader(String name);
    
    /**
     * Get the values of request headers. 
     * @return a read-only map of header names and values.
     */
    MultivaluedMap<String, String> getRequestHeaders();
    
    
}
