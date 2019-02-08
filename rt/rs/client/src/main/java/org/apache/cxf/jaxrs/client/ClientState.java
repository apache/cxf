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

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;


/**
 * Represents the client state :
 *  - baseURI
 *  - current uri builder
 *  - current requestHeaders
 *  - current template parameters map
 *  - last response
 */
public interface ClientState {

    /**
     * Sets the current builder
     * @param currentBuilder the builder
     */
    void setCurrentBuilder(UriBuilder currentBuilder);

    /**
     * Gets the current builder
     * @return
     */
    UriBuilder getCurrentBuilder();

    /**
     * Sets the base URI
     * @param baseURI baseURI
     */
    void setBaseURI(URI baseURI);

    /**
     * Gets the base URI
     * @return baseURI
     */
    URI getBaseURI();

    /**
     * Sets Response
     * @param response response
     */
    void setResponse(Response response);

    /**
     * Gets Response
     * @return response
     */
    Response getResponse();

    /**
     * Sets the request headers
     * @param requestHeaders request headers
     */
    void setRequestHeaders(MultivaluedMap<String, String> requestHeaders);

    /**
     * Gets the request headers
     * @return request headers, may be immutable
     */
    MultivaluedMap<String, String> getRequestHeaders();

    /**
     * Sets the map containing template name and value pairs
     * @param templates
     */
    void setTemplates(MultivaluedMap<String, String> map);

    /**
     * Gets the templates map
     * @return templates
     */
    MultivaluedMap<String, String> getTemplates();


    /**
     * Resets the current state to the baseURI
     *
     */
    void reset();

    /**
     * The factory method for creating a new state.
     * Example, proxy and WebClient.fromClient will use this method when creating
     * subresource proxies and new web clients respectively to ensure thet stay
     * thread-local if needed
     * @param baseURI baseURI
     * @param headers request headers, can be null
     * @param templates initial templates map, can be null
     * @return client state
     */
    ClientState newState(URI baseURI,
                         MultivaluedMap<String, String> headers,
                         MultivaluedMap<String, String> templates);
}
