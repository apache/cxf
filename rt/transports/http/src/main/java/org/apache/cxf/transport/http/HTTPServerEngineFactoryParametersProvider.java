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

package org.apache.cxf.transport.http;

import java.util.Optional;

import jakarta.annotation.Nullable;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSServerParameters;

/**
 * Provides programmatic defaults to the different HTTP server engine
 * factories implementations (that do not share any common interfaces or other
 * abstractions).
 */
public interface HTTPServerEngineFactoryParametersProvider {
    /**
     * Returns the default {@link TLSServerParameters} instance that the HTTP server
     * engine could use if there are no {@link TLSServerParameters} provided to by the 
     * configuration.
     * @param bus {@link Bus} instance
     * @param host host name
     * @param port port
     * @param protocol protocol
     * @param id server transport identifier (if available)
     * @return the default {@link TLSServerParameters} instance, if available
     */
    Optional<TLSServerParameters> getDefaultTlsServerParameters(Bus bus, String host,
        int port, String protocol, @Nullable String id);
}
