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

package org.apache.cxf.transports.http;

import org.apache.cxf.service.model.EndpointInfo;

public interface StemMatchingQueryHandler extends QueryHandler {
    
    /**
     * @param fullQueryString the target full query string (with params) of the request
     * @param ctx the context that was set for this invokation
     * @param endpoint the current endpoint for this context (e.g. the endpoint this
     * Destination was activated for). Null if no current endpoint.
     * @param contextMatchExact true if contextMatchStrategy is "exact"
false otherwise
     * @return true iff the URI is a recognized WSDL query
     */
    boolean isRecognizedQuery(String fullQueryString,
                              String ctx,
                              EndpointInfo endpoint,
                              boolean contextMatchExact);
}
