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

package org.apache.cxf.binding;

import java.util.Collection;

import javax.xml.namespace.QName;

import jakarta.annotation.Resource;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.ChainInitiationObserver;
import org.apache.cxf.transport.Destination;

public abstract class AbstractBindingFactory implements BindingFactory {

    protected Collection<String> activationNamespaces;

    protected Bus bus;

    public AbstractBindingFactory() {
    }

    public AbstractBindingFactory(Collection<String> ns) {
        activationNamespaces = ns;
    }
    public AbstractBindingFactory(Bus b) {
        bus = b;
        registerWithBindingManager();
    }

    public AbstractBindingFactory(Bus b, Collection<String> ns) {
        activationNamespaces = ns;
        bus = b;
        registerWithBindingManager();
    }

    private void registerWithBindingManager() {
        if (bus != null && activationNamespaces != null) {
            BindingFactoryManager manager = bus.getExtension(BindingFactoryManager.class);
            for (String ns : activationNamespaces) {
                manager.registerBindingFactory(ns, this);
            }
        }
    }

    /**
     * Creates a "default" BindingInfo object for the service.  Called by
     * createBindingInfo(Service service, String binding, Object config) to actually
     * create the BindingInfo.  Can return a subclass which can then process
     * the extensors within the subclass.
     */
    public BindingInfo createBindingInfo(ServiceInfo service, String namespace, Object config) {
        return new BindingInfo(service, namespace);
    }

    /**
     * Creates a "default" BindingInfo object for the service.  Can return a subclass
     * which can then process the extensors within the subclass.   By default, just
     * creates it for the first ServiceInfo in the service
     */
    public BindingInfo createBindingInfo(Service service, String namespace, Object config) {
        BindingInfo bi = createBindingInfo(service.getServiceInfos().get(0), namespace, config);
        if (bi.getName() == null) {
            bi.setName(new QName(service.getName().getNamespaceURI(),
                                 service.getName().getLocalPart() + "Binding"));
        }
        return bi;
    }


    public void addListener(Destination d, Endpoint e) {
        ChainInitiationObserver observer = new ChainInitiationObserver(e, bus);

        d.setMessageObserver(observer);
    }

    public Bus getBus() {
        return bus;
    }

    @Resource
    public void setBus(Bus bus) {
        if (this.bus != bus) {
            this.bus = bus;
            registerWithBindingManager();
        }
    }

    public Collection<String> getActivationNamespaces() {
        return activationNamespaces;
    }

    public void setActivationNamespaces(Collection<String> activationNamespaces) {
        this.activationNamespaces = activationNamespaces;
        registerWithBindingManager();
    }

}
