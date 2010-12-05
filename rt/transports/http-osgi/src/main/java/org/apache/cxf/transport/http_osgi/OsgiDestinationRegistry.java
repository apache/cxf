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
package org.apache.cxf.transport.http_osgi;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class OsgiDestinationRegistry implements OsgiDestinationRegistryIntf {

    private ConcurrentMap<String, AbstractHTTPDestination> destinations 
        = new ConcurrentHashMap<String, AbstractHTTPDestination>();

    public OsgiDestinationRegistry() {
    }

    public void addDestination(String path, AbstractHTTPDestination destination) {
        String p = getTrimmedPath(path);
        destinations.putIfAbsent(p, destination);
    }

    public void removeDestination(String path) {
        destinations.remove(path);
    }

    public AbstractHTTPDestination getDestinationForPath(String path) {
        // to use the url context match
        return destinations.get(getTrimmedPath(path));
    }
    
    public AbstractHTTPDestination checkRestfulRequest(String address) {
        for (String path : getDestinationsPaths()) {
            if (address.startsWith(path)) {
                return getDestinationForPath(path);
            }
        }
        return null;
    }

    public Collection<AbstractHTTPDestination> getDestinations() {
        return Collections.unmodifiableCollection(destinations.values());
    }

    public Set<String> getDestinationsPaths() {
        return Collections.unmodifiableSet(destinations.keySet());
    }

    /**
     * Remove the transport protocol from the path and make 
     * it starts with /
     * @param path 
     * @return trimmed path
     */
    static String getTrimmedPath(String path) {
        if (path == null) {
            return "/";
        }
        final String lh = "http://localhost/";
        final String lhs = "https://localhost/";

        if (path.startsWith(lh)) {
            path = path.substring(lh.length());
        } else if (path.startsWith(lhs)) {
            path = path.substring(lhs.length());
        }
        if (!path.startsWith("/")) {
            path = "/" + path;

        }
        return path;
    }

}
