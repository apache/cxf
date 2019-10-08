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

package org.apache.cxf.transport.local;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.workqueue.WorkQueueManager;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl.http.AddressType;

@NoJSR250Annotations
public class LocalTransportFactory extends AbstractTransportFactory
    implements DestinationFactory, ConduitInitiator {

    public static final String TRANSPORT_ID = "http://cxf.apache.org/transports/local";
    public static final List<String> DEFAULT_NAMESPACES = Collections.singletonList(TRANSPORT_ID);

    public static final String MESSAGE_FILTER_PROPERTIES
        = LocalTransportFactory.class.getName() + ".filterProperties";
    public static final String MESSAGE_INCLUDE_PROPERTIES
        = LocalTransportFactory.class.getName() + ".includeProperties";

    private static final Logger LOG = LogUtils.getL7dLogger(LocalTransportFactory.class);
    private static final Set<String> URI_PREFIXES = Collections.singleton("local://");
    private static final String NULL_ADDRESS
        = LocalTransportFactory.class.getName() + ".nulladdress";

    private ConcurrentMap<String, LocalDestination> destinations
        = new ConcurrentHashMap<>();

    private Set<String> messageFilterProperties = new HashSet<>();
    private Set<String> messageIncludeProperties = new HashSet<>();
    private Set<String> uriPrefixes = new HashSet<>(URI_PREFIXES);
    private volatile Executor executor;

    public LocalTransportFactory() {
        super(DEFAULT_NAMESPACES);

        messageFilterProperties.add(Message.REQUESTOR_ROLE);

        messageIncludeProperties.add(Message.PROTOCOL_HEADERS);
        messageIncludeProperties.add(Message.ENCODING);
        messageIncludeProperties.add(Message.CONTENT_TYPE);
        messageIncludeProperties.add(Message.ACCEPT_CONTENT_TYPE);
        messageIncludeProperties.add(Message.RESPONSE_CODE);
        messageIncludeProperties.add(Message.REQUEST_URI);
        messageIncludeProperties.add(Message.ENDPOINT_ADDRESS);
        messageIncludeProperties.add(Message.HTTP_REQUEST_METHOD);
    }

    public LocalDestination getDestination(EndpointInfo ei, Bus bus) throws IOException {
        return getDestination(ei, createReference(ei), bus);
    }

    protected LocalDestination getDestination(EndpointInfo ei,
                                         EndpointReferenceType reference,
                                         Bus bus)
        throws IOException {
        String addr = reference.getAddress().getValue();
        if (addr == null) {
            AddressType tp = ei.getExtensor(AddressType.class);
            if (tp != null) {
                addr = tp.getLocation();
            }
        }
        if (addr == null) {
            addr = NULL_ADDRESS;
        }
        LocalDestination d = destinations.get(addr);
        if (d == null) {
            d = createDestination(ei, reference, bus);
            LocalDestination tmpD = destinations.putIfAbsent(addr, d);
            if (tmpD != null) {
                d = tmpD;
            }
        }
        return d;
    }

    private LocalDestination createDestination(EndpointInfo ei, EndpointReferenceType reference, Bus bus) {
        LOG.info("Creating destination for address " + reference.getAddress().getValue());
        return new LocalDestination(this, reference, ei, bus);
    }

    void remove(LocalDestination destination) {
        destinations.values().remove(destination);
    }

    public Executor getExecutor(Bus bus) {
        if (executor == null && bus != null) {
            WorkQueueManager manager = bus.getExtension(WorkQueueManager.class);
            if (manager != null) {
                Executor ex = manager.getNamedWorkQueue("local-transport");
                if (ex == null) {
                    ex = manager.getAutomaticWorkQueue();
                }
                return ex;
            }
        }
        return executor;
    }

    public void setExecutor(Executor executor) {
        this.executor = executor;
    }

    public Conduit getConduit(EndpointInfo ei, Bus bus) throws IOException {
        return new LocalConduit(this, getDestination(ei, bus));
    }

    public Conduit getConduit(EndpointInfo ei, EndpointReferenceType target, Bus bus) throws IOException {
        return new LocalConduit(this, getDestination(ei, target, bus));
    }

    private static EndpointReferenceType createReference(EndpointInfo ei) {
        EndpointReferenceType epr = new EndpointReferenceType();
        AttributedURIType address = new AttributedURIType();
        address.setValue(ei.getAddress());
        epr.setAddress(address);
        return epr;
    }

    public Set<String> getUriPrefixes() {
        return uriPrefixes;
    }
    public void setUriPrefixes(Set<String> s) {
        uriPrefixes = s;
    }

    public Set<String> getMessageFilterProperties() {
        return messageFilterProperties;
    }

    public void setMessageFilterProperties(Set<String> props) {
        this.messageFilterProperties = props;
    }
    public Set<String> getIncludeMessageProperties() {
        return messageIncludeProperties;
    }

    public void setMessageIncludeProperties(Set<String> props) {
        this.messageIncludeProperties = props;
    }


    public void copy(Message message, Message copy) {
        Set<String> filter = CastUtils.cast((Set<?>)message.get(MESSAGE_FILTER_PROPERTIES));
        if (filter == null) {
            filter = messageFilterProperties;
        }

        Set<String> includes = CastUtils.cast((Set<?>)message.get(MESSAGE_INCLUDE_PROPERTIES));
        if (includes == null) {
            includes = messageIncludeProperties;
        }

        // copy all the contents
        for (Map.Entry<String, Object> e : message.entrySet()) {
            if ((includes.contains(e.getKey())
                || messageIncludeProperties.contains(e.getKey()))
                && !filter.contains(e.getKey())) {
                copy.put(e.getKey(), e.getValue());
            }
        }
    }
}
