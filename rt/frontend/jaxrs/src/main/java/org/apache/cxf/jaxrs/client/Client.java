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
package org.apache.cxf.jaxrs.client;

import java.net.URI;
import java.util.Date;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

public interface Client {
    
    Client type(MediaType ct);
    Client type(String type);
    Client accept(MediaType... types);
    Client accept(String... types);
    
    Client language(String language);
    Client acceptLanguage(String ...languages);
    
    Client encoding(String enc);
    Client acceptEncoding(String ...encs);
    
    Client match(EntityTag tag, boolean ifNot);
    Client modified(Date date, boolean ifNot);
    
    Client cookie(Cookie cookie);
    
    Client header(String name, Object... values);
    Client headers(MultivaluedMap<String, String> map);
    
    Client reset();
    
    MultivaluedMap<String, String> getHeaders();
    URI getBaseURI();
    URI getCurrentURI();
    
    /**
     * Gets the response state if any
     * @return JAXRS Response response
     */
    Response getResponse();
}
