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
package org.apache.cxf.transport.jbi;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.jbi.JBIException;
import javax.jbi.messaging.DeliveryChannel;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.configuration.Configurer;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;

public class JBITransportFactory extends AbstractTransportFactory implements ConduitInitiator,
    DestinationFactory {

    public static final String TRANSPORT_ID = "http://cxf.apache.org/transports/jbi";

    private static final Logger LOG = LogUtils.getL7dLogger(JBITransportFactory.class);


    private DeliveryChannel deliveryChannel;
    private Bus bus;
    private final Map<String, JBIDestination> destinationMap =  new HashMap<String, JBIDestination>();


    private Collection<String> activationNamespaces;

    @Resource(name = "cxf")
    public void setBus(Bus b) {
        bus = b;
    }

    public Bus getBus() {
        return bus;
    }

    public Set<String> getUriPrefixes() {
        return Collections.singleton("jbi");
    }


    public void setActivationNamespaces(Collection<String> ans) {
        activationNamespaces = ans;
    }


    @PostConstruct
    void registerWithBindingManager() {
        if (null == bus) {
            return;
        }
        ConduitInitiatorManager cim = bus.getExtension(ConduitInitiatorManager.class);
        if (null != cim && null != activationNamespaces) {
            for (String ns : activationNamespaces) {
                cim.registerConduitInitiator(ns, this);
            }
        }
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        if (null != dfm && null != activationNamespaces) {
            for (String ns : activationNamespaces) {
                dfm.registerDestinationFactory(ns, this);
            }
        }
    }




    public DeliveryChannel getDeliveryChannel() {
        return deliveryChannel;
    }

    public void setDeliveryChannel(DeliveryChannel newDeliverychannel) {
        LOG.info(new org.apache.cxf.common.i18n.Message(
            "CONFIG.DELIVERY.CHANNEL", LOG).toString() + newDeliverychannel);
        deliveryChannel = newDeliverychannel;
    }

    public Conduit getConduit(EndpointInfo targetInfo) throws IOException {
        return getConduit(targetInfo, null);
    }

    public Conduit getConduit(EndpointInfo endpointInfo, EndpointReferenceType target) throws IOException {
        Conduit conduit = new JBIConduit(target, getDeliveryChannel());
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(conduit);
        }
        return conduit;
    }

    public Destination getDestination(EndpointInfo ei) throws IOException {
        JBIDestination destination = new JBIDestination(ei,
                                         JBIDispatcherUtil.getInstance(this, getDeliveryChannel()),
                                         getDeliveryChannel());
        Configurer configurer = bus.getExtension(Configurer.class);
        if (null != configurer) {
            configurer.configureBean(destination);
        }
        try {
            putDestination(ei.getService().getName().toString()
                + ei.getInterface().getName().toString(), destination);
        } catch (JBIException e) {
            throw new IOException(e.getMessage());
        }
        return destination;
    }

    public void putDestination(String epName, JBIDestination destination) throws JBIException {
        if (destinationMap.containsKey(epName)) {
            throw new JBIException("JBIDestination for Endpoint "
                                   + epName + " has already been created");
        } else {
            destinationMap.put(epName, destination);
        }
    }

    public JBIDestination getDestination(String epName) {
        return destinationMap.get(epName);
    }

    public void removeDestination(String epName) {
        destinationMap.remove(epName);
    }

}
