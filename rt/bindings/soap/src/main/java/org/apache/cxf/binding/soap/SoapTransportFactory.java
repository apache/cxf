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

package org.apache.cxf.binding.soap;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.wsdl.Port;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.tcp.TCPConduit;
import org.apache.cxf.binding.soap.wsdl11.SoapAddressPlugin;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.tools.common.extensions.soap.SoapAddress;
import org.apache.cxf.tools.util.SOAPBindingUtil;
import org.apache.cxf.transport.AbstractTransportFactory;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.wsdl11.WSDLEndpointFactory;

public class SoapTransportFactory extends AbstractTransportFactory implements DestinationFactory,
    WSDLEndpointFactory, ConduitInitiator {
    
    public static final String SOAP_11_HTTP_BINDING = "http://schemas.xmlsoap.org/soap/http";
    public static final String SOAP_12_HTTP_BINDING = "http://www.w3.org/2003/05/soap/bindings/HTTP/";
    
    public static final String TRANSPORT_ID = "http://schemas.xmlsoap.org/soap/";
    
    private Bus bus;
    private Collection<String> activationNamespaces;
    
    public SoapTransportFactory() {
        super();
    }
    public Set<String> getUriPrefixes() {
        return Collections.singleton("soap.tcp");
    }
    public String mapTransportURI(String s) {
        if ("http://www.w3.org/2008/07/soap/bindings/JMS/".equals(s)) {
            s = "http://cxf.apache.org/transports/jms";
        }
        return s;
    }

    public Destination getDestination(EndpointInfo ei) throws IOException {
        SoapBindingInfo binding = (SoapBindingInfo)ei.getBinding();
        DestinationFactory destinationFactory;
        try {
            destinationFactory = bus.getExtension(DestinationFactoryManager.class)
                .getDestinationFactory(mapTransportURI(binding.getTransportURI()));
            return destinationFactory.getDestination(ei);
        } catch (BusException e) {
            throw new RuntimeException("Could not find destination factory for transport "
                                       + binding.getTransportURI());
        }
    }

    public void createPortExtensors(EndpointInfo ei, Service service) {
        if (ei.getBinding() instanceof SoapBindingInfo) {
            SoapBindingInfo bi = (SoapBindingInfo)ei.getBinding();
            createSoapExtensors(ei, bi, bi.getSoapVersion() instanceof Soap12);
        }
    }

    private void createSoapExtensors(EndpointInfo ei, SoapBindingInfo bi, boolean isSoap12) {
        try {
            // We need to populate the soap extensibilityelement proxy for soap11 and soap12
            ExtensionRegistry extensionRegistry = WSDLFactory.newInstance().newPopulatedExtensionRegistry();
            SoapAddressPlugin addresser = new SoapAddressPlugin();
            addresser.setExtensionRegistry(extensionRegistry);
                //SoapAddress soapAddress = SOAPBindingUtil.createSoapAddress(extensionRegistry, isSoap12);
            String address = ei.getAddress();
            if (address == null) {
                address = "http://localhost:9090";
            }

            //soapAddress.setLocationURI(address);
            ei.addExtensor(addresser.createExtension(isSoap12, address));

            //createSoapBinding(isSoap12, extensionRegistry, bi);
            
        } catch (WSDLException e) {
            e.printStackTrace();
        }
    }

    public EndpointInfo createEndpointInfo(ServiceInfo serviceInfo, BindingInfo b, Port port) {
        String transportURI = "http://schemas.xmlsoap.org/wsdl/soap/";
        if (b instanceof SoapBindingInfo) {
            SoapBindingInfo sbi = (SoapBindingInfo)b;
            transportURI = sbi.getTransportURI();
        }
        EndpointInfo info = new SoapEndpointInfo(serviceInfo, transportURI);
        if (port != null) {
            List ees = port.getExtensibilityElements();
            for (Iterator itr = ees.iterator(); itr.hasNext();) {
                Object extensor = itr.next();
    
                if (SOAPBindingUtil.isSOAPAddress(extensor)) {
                    final SoapAddress sa = SOAPBindingUtil.getSoapAddress(extensor);
    
                    info.addExtensor(sa);
                    info.setAddress(sa.getLocationURI());
                } else {
                    info.addExtensor(extensor);
                }
            }
        }
        return info;
    }


    public Conduit getConduit(EndpointInfo ei, EndpointReferenceType target) throws IOException {
        return getConduit(ei);
    }

    public Conduit getConduit(EndpointInfo ei) throws IOException {
        if (!StringUtils.isEmpty(ei.getAddress()) && ei.getAddress().startsWith("soap.tcp://")) {
            //TODO - examine policies and stuff to look for the sun tcp policies
            return new TCPConduit(ei);
        }
        
        SoapBindingInfo binding = (SoapBindingInfo)ei.getBinding();
        ConduitInitiator conduitInit;
        try {
            conduitInit = bus.getExtension(ConduitInitiatorManager.class)
                .getConduitInitiator(mapTransportURI(binding.getTransportURI()));

            return conduitInit.getConduit(ei);
        } catch (BusException e) {
            throw new RuntimeException("Could not find conduit initiator for transport "
                                       + binding.getTransportURI());
        }
    }

    public Bus getBus() {
        return bus;
    }

    @Resource(name = "cxf")
    public void setBus(Bus bus) {
        this.bus = bus;
    }
    
    public void setActivationNamespaces(Collection<String> ans) {
        activationNamespaces = ans;
    }

    @PostConstruct
    void registerWithBindingManager() {
        if (null == bus) {
            return;
        }

        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        if (null != dfm && activationNamespaces != null) {
            for (String ns : activationNamespaces) {
                dfm.registerDestinationFactory(ns, this);
            }
        }
    }
    
    private static class SoapEndpointInfo extends EndpointInfo {
        SoapAddress saddress;
        SoapEndpointInfo(ServiceInfo serv, String trans) {
            super(serv, trans);
        }
        public void setAddress(String s) {
            super.setAddress(s);
            if (saddress != null) {
                saddress.setLocationURI(s);
            }
        }

        public void addExtensor(Object el) {
            super.addExtensor(el);
            if (el instanceof SoapAddress) {
                saddress = (SoapAddress)el;
            }
        }
    }

}
