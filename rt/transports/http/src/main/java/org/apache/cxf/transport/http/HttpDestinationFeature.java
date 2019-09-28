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

import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.AbstractPortableFeature;
import org.apache.cxf.feature.DelegatingFeature;
import org.apache.cxf.transport.Destination;

/**
 * Programmatically configure a http destination. This can also be used as a DOSGi
 * intent.
 */
public class HttpDestinationFeature extends DelegatingFeature<HttpDestinationFeature.Portable> {
    public HttpDestinationFeature() {
        super(new Portable());
    }

    public void setDestinationConfig(HttpDestinationConfig destinationConfig) {
        delegate.setDestinationConfig(destinationConfig);
    }

    public static class Portable implements AbstractPortableFeature {
        private HttpDestinationConfig destinationConfig;

        @Override
        public void initialize(Server server, Bus bus) {
            Destination destination = server.getDestination();
            if (destinationConfig != null && destination instanceof AbstractHTTPDestination) {
                destinationConfig.apply((AbstractHTTPDestination)destination);
            }
        }

        public void setDestinationConfig(HttpDestinationConfig destinationConfig) {
            this.destinationConfig = destinationConfig;
        }
    }
}
