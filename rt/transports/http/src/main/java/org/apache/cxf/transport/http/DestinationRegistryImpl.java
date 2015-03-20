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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.cxf.transport.AbstractDestination;

public class DestinationRegistryImpl implements DestinationRegistry {
    private static final String SLASH = "/";
    private ConcurrentMap<String, AbstractHTTPDestination> destinations 
        = new ConcurrentHashMap<String, AbstractHTTPDestination>();
    private Map<String, AbstractHTTPDestination> decodedDestinations = 
        new ConcurrentHashMap<String, AbstractHTTPDestination>();

    public DestinationRegistryImpl() {
    }

    public synchronized void addDestination(AbstractHTTPDestination destination) {
        String path = getTrimmedPath(destination.getEndpointInfo().getAddress());
        AbstractHTTPDestination dest = destinations.putIfAbsent(path, destination);
        if (dest != null && dest != destination) {
            throw new RuntimeException("Already a destination on " + path);
        }
        try {
            String path2 = URLDecoder.decode(path, "ISO-8859-1");
            if (!path.equals(path2)) {
                decodedDestinations.put(URLDecoder.decode(path, "ISO-8859-1"), destination);
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported Encoding", e);
        }
    }

    public synchronized void removeDestination(String path) {
        destinations.remove(path);
        try {
            String path2 = URLDecoder.decode(path, "ISO-8859-1");
            if (!path.equals(path2)) {
                decodedDestinations.remove(URLDecoder.decode(path, "ISO-8859-1"));
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unsupported Encoding", e);
        }
    }

    public AbstractHTTPDestination getDestinationForPath(String path) {
        return getDestinationForPath(path, false);
    }
    
    public AbstractHTTPDestination getDestinationForPath(String path, boolean tryDecoding) {
        // to use the url context match  
        String m = getTrimmedPath(path);
        AbstractHTTPDestination s = destinations.get(m);
        if (s == null) {
            s = decodedDestinations.get(m);
        }
        return s;
    }
    
    public AbstractHTTPDestination checkRestfulRequest(String address) {
        AbstractHTTPDestination ret = getRestfulDestination(getDestinationsPaths(), address);
        if (ret == null) {
            ret = getRestfulDestination(decodedDestinations.keySet(), address);
        }
        if (ret != null && ret.getMessageObserver() == null) {
            return null;
        }
        return ret; 
    }
    private AbstractHTTPDestination getRestfulDestination(Set<String> destPaths, String address) {
        int len = -1;
        AbstractHTTPDestination ret = null;
        for (String path : destPaths) {
            String thePath = path.length() > 1 && path.endsWith(SLASH) 
                ? path.substring(0, path.length() - 1) : path;
            if ((address.equals(thePath) 
                || SLASH.equals(thePath)
                || (address.length() > thePath.length() 
                    && address.startsWith(thePath) && address.charAt(thePath.length()) == '/'))
                && thePath.length() > len) {
                ret = getDestinationForPath(path);
                len = path.length();
            }
        }
        return ret;
    }

    public Collection<AbstractHTTPDestination> getDestinations() {
        return Collections.unmodifiableCollection(destinations.values());
    }
    

    public AbstractDestination[] getSortedDestinations() {
        List<AbstractHTTPDestination> dest2 = new LinkedList<AbstractHTTPDestination>(
                getDestinations());
        Collections.sort(dest2, new Comparator<AbstractHTTPDestination>() {
            public int compare(AbstractHTTPDestination o1, AbstractHTTPDestination o2) {
                if (o1.getEndpointInfo().getInterface() == null) {
                    return -1;
                }
                if (o2.getEndpointInfo().getInterface() == null) {
                    return 1;
                }
                return o1.getEndpointInfo().getInterface().getName()
                        .getLocalPart().compareTo(
                                o2.getEndpointInfo().getInterface().getName()
                                        .getLocalPart());
            }
        });

        return dest2.toArray(new AbstractDestination[dest2.size()]);
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
    public String getTrimmedPath(String path) {
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
        if (!path.contains("://") && !path.startsWith("/")) {
            path = "/" + path;

        }
        return path;
    }

}
