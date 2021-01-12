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

import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;

/**
 * Represents common proxy and http-centric client capabilities
 *
 */
public interface Client {

    /**
     * Set HTTP Content-Type header
     * @param ct JAXRS MediaType representing Content-Type value
     * @return the updated Client
     */
    Client type(MediaType ct);

    /**
     * Set HTTP Content-Type header
     * @param type Content-Type value
     * @return the updated Client
     */
    Client type(String type);

    /**
     * Set HTTP Accept header
     * @param types list of JAXRS MediaTypes representing Accept header values
     * @return the updated Client
     */
    Client accept(MediaType... types);

    /**
     * Set HTTP Accept header
     * @param types list of Accept header values
     * @return the updated Client
     */
    Client accept(String... types);

    /**
     * Set HTTP Content-Language header
     * @param language Content-Language header value
     * @return the updated Client
     */
    Client language(String language);

    /**
     * Set HTTP Accept-Language header
     * @param languages list of Accept-Language header values
     * @return the updated Client
     */
    Client acceptLanguage(String ...languages);

    /**
     * Set HTTP Content-Encoding header
     * @param encoding Content-Encoding header value
     * @return the updated Client
     */
    Client encoding(String encoding);

    /**
     * Set HTTP Accept-Encoding header
     * @param encodings list of Accept-Encoding header value
     * @return the updated Client
     */
    Client acceptEncoding(String ...encodings);

    /**
     * Set HTTP If-Match or If-None-Match header
     * @param tag ETag value
     * @param ifNot if true then If-None-Match is set, If-Match otherwise
     * @return the updated Client
     */
    Client match(EntityTag tag, boolean ifNot);

    /**
     * Set HTTP If-Modified-Since or If-Unmodified-Since header
     * @param date Date value, will be formated as "EEE, dd MMM yyyy HH:mm:ss zzz"
     * @param ifNot if true then If-Unmodified-Since is set, If-Modified-Since otherwise
     * @return the updated Client
     */
    Client modified(Date date, boolean ifNot);

    /**
     * Set HTTP Cookie header
     * @param cookie Cookie value
     * @return the updated Client
     */
    Client cookie(Cookie cookie);

    /**
     * Set HTTP Authorization header
     * @param auth Authorization value
     * @return the updated Client
     */
    Client authorization(Object auth);

    /**
     * Update the current URI query parameters
     * @param name query name
     * @param values query values
     * @return updated WebClient
     */
    Client query(String name, Object ...values);

    /**
     * Set arbitrary HTTP Header
     * @param name header name
     * @param values list of header values
     * @return the updated Client
     */
    Client header(String name, Object... values);

    /**
     * Set HTTP Headers
     * @param map headers
     * @return the updated Client
     */
    Client headers(MultivaluedMap<String, String> map);

    /**
     * Reset the headers and response state if any
     * @return the updated Client
     */
    Client reset();

    /**
     * Get the copy of request headers
     * @return request headers
     */
    MultivaluedMap<String, String> getHeaders();

    /**
     * Get the base URI this Client has been intialized with
     * @return base URI
     */
    URI getBaseURI();

    /**
     * Get the current URI this Client is working with
     * @return current URI
     */
    URI getCurrentURI();

    /**
     * Get the response state if any
     * @return JAXRS Response response
     */
    Response getResponse();

    /**
     * Release the internal state and configuration associated with this client
     */
    void close();
}
