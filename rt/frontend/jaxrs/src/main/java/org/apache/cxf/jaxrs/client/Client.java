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

/**
 * Represents common proxy and http-centric client capabilities
 *
 */
public interface Client {
    
    /**
     * sets HTTP Content-Type header
     * @param ct JAXRS MediaType representing Content-Type value  
     * @return the updated Client
     */
    Client type(MediaType ct);
    
    /**
     * sets HTTP Content-Type header
     * @param type Content-Type value  
     * @return the updated Client
     */
    Client type(String type);
    
    /**
     * sets HTTP Accept header
     * @param types list of JAXRS MediaTypes representing Accept header values  
     * @return the updated Client
     */
    Client accept(MediaType... types);
    
    /**
     * sets HTTP Accept header
     * @param types list of Accept header values  
     * @return the updated Client
     */
    Client accept(String... types);
    
    /**
     * sets HTTP Content-Language header 
     * @param language Content-Language header value  
     * @return the updated Client
     */    
    Client language(String language);
    
    /**
     * sets HTTP Accept-Language header 
     * @param languages list of Accept-Language header values  
     * @return the updated Client
     */
    Client acceptLanguage(String ...languages);
    
    /**
     * sets HTTP Content-Encoding header 
     * @param encoding Content-Encoding header value  
     * @return the updated Client
     */
    Client encoding(String encoding);
    
    /**
     * sets HTTP Accept-Encoding header 
     * @param encodings list of Accept-Encoding header value  
     * @return the updated Client
     */
    Client acceptEncoding(String ...encodings);
    
    /**
     * sets HTTP If-Match or If-None-Match header
     * @param tag ETag value
     * @param ifNot if true then If-None-Match is set, If-Match otherwise  
     * @return the updated Client
     */
    Client match(EntityTag tag, boolean ifNot);
    
    /**
     * sets HTTP If-Modified-Since or If-Unmodified-Since header
     * @param date Date value, will be formated as "EEE, dd MMM yyyy HH:mm:ss zzz" 
     * @param ifNot if true then If-Unmodified-Since is set, If-Modified-Since otherwise  
     * @return the updated Client
     */
    Client modified(Date date, boolean ifNot);
    
    /**
     * sets HTTP Cookie header 
     * @param cookie Cookie value  
     * @return the updated Client
     */
    Client cookie(Cookie cookie);
    
    /**
     * Sets arbitrary HTTP Header
     * @param name header name
     * @param values list of header values
     * @return the updated Client
     */
    Client header(String name, Object... values);
    
    /**
     * Sets HTTP Headers
     * @param map headers
     * @return the updated Client
     */
    Client headers(MultivaluedMap<String, String> map);

    /**
     * Resets the headers and response state if any
     * @return  the updated Client
     */
    Client reset();
    
    /**
     * Gets the copy of request headers
     * @return request headers
     */
    MultivaluedMap<String, String> getHeaders();
    
    /**
     * Gets the base URI this Client has been intialized with
     * @return base URI
     */
    URI getBaseURI();
    
    /**
     * Gets the current URI this Client is working with
     * @return current URI
     */
    URI getCurrentURI();
    
    /**
     * Gets the response state if any
     * @return JAXRS Response response
     */
    Response getResponse();
}
