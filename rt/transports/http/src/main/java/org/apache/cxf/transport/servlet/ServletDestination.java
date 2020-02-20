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

package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.DestinationRegistry;


public class ServletDestination extends AbstractHTTPDestination implements ServletConfigAware {

    static final Logger LOG = LogUtils.getL7dLogger(ServletDestination.class);

    /**
     * Constructor, allowing subsititution of configuration.
     *
     * @param b the associated Bus
     * @param registry the destination registry
     * @param ei the endpoint info of the destination
     * @param path the path
     * @throws IOException
     */
    public ServletDestination(Bus b,
                              DestinationRegistry registry,
                              EndpointInfo ei,
                              String path)
        throws IOException {
        // would add the default port to the address
        super(b, registry, ei, path, false);
    }
    public ServletDestination(Bus b,
                              DestinationRegistry registry,
                              EndpointInfo ei,
                              String path,
                              boolean dp)
        throws IOException {
        // would add the default port to the address
        super(b, registry, ei, path, dp);
    }

    protected Logger getLogger() {
        return LOG;
    }

    protected String getBasePath(String contextPath) throws IOException {

        String address = getAddress().getAddress().getValue();
        if (address == null) {
            return contextPath;
        }
        if (address.startsWith("http")) {
            return URI.create(address.replaceAll(" ", "%20")).getPath();
        }

        return contextPath + address;
    }

}
