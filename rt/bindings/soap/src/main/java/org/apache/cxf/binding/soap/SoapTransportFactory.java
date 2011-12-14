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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.annotation.Resource;
import javax.wsdl.WSDLException;
import javax.wsdl.extensions.ExtensionRegistry;
import javax.wsdl.factory.WSDLFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.soap.jms.interceptor.SoapJMSConstants;
import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.binding.soap.tcp.SoapTcpDestination;
import org.apache.cxf.binding.soap.tcp.TCPConduit;
import org.apache.cxf.binding.soap.wsdl11.SoapAddressPlugin;
import org.apache.cxf.common.injection.NoJSR250Annotations;
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

@NoJSR250Annotations(unlessNull = { "bus" })
public class SoapTransportFactory extends AbstractTransportFactory implements DestinationFactory,
    WSDLEndpointFactory, ConduitInitiator {
    
    public static final String SOAP_11_HTTP_BINDING = "http://schemas.xmlsoap.org/soap/http";
    public static final String SOAP_12_HTTP_BINDING = "http://www.w3.org/2003/05/soap/bindings/HTTP/";
    
    public static final String TRANSPORT_ID = "http://schemas.xmlsoap.org/soap/";
    
    public static final List<String> DEFAULT_NAMESPACES = Arrays.asList(
            "http://schemas.xmlsoap.org/soap/",
            "http://schemas.xmlsoap.org/wsdl/soap/",
            "http://schemas.xmlsoap.org/wsdl/soap12/",
            "http://schemas.xmlsoap.org/soap/http/",
            "http://schemas.xmlsoap.org/wsdl/soap/http",
            "http://www.w3.org/2010/soapjms/",
            "http://www.w3.org/2003/05/soap/bindings/HTTP/",
            "http://schemas.xmlsoap.org/soap/http");
    
    public SoapTransportFactory() {
        super(DEFAULT_NAMESPACES, null);
    }

    public SoapTransportFactory(Bus b) {
        super(DEFAULT_NAMESPACES, b);
    }
    
    public Set<String> getUriPrefixes() {
        return Collections.singleton("soap.tcp");
    }
    public String mapTransportURI(String s, String address) {
        if (SoapJMSConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID.equals(s)
            || (address != null && address.startsWith("jms"))) {
            s = "http://cxf.apache.org/transports/jms";
        } else if (SOAP_11_HTTP_BINDING.equals(s)
            || SOAP_12_HTTP_BINDING.equals(s)
            || "http://schemas.xmlsoap.org/wsdl/soap/".equals(s)
            || "http://schemas.xmlsoap.org/wsdl/http".equals(s)
            || "http://schemas.xmlsoap.org/wsdl/soap/http".equals(s)
            || "http://schemas.xmlsoap.org/wsdl/soap/http/".equals(s)
            || "http://schemas.xmlsoap.org/wsdl/http/".equals(s)) {
            s = "http://cxf.apache.org/transports/http";
        }
        return s;
    }
    private boolean isJMSSpecAddress(String address) {
        return address != null && address.startsWith("jms:") && !"jms://".equals(address);
    }

    public Destination getDestination(EndpointInfo ei) throws IOException {
        String address = ei.getAddress();
        if (!StringUtils.isEmpty(address) && address.startsWith("soap.tcp")) {
            return new SoapTcpDestination(ei.getTarget(), ei);
        }
        BindingInfo bi = ei.getBinding();
        String transId = ei.getTransportId();
        if (bi instanceof SoapBindingInfo) {
            transId = ((SoapBindingInfo)bi).getTransportURI();
            if (transId == null) {
                transId = ei.getTransportId();
            }
        }
        DestinationFactory destinationFactory;
        try {
            DestinationFactoryManager mgr = bus.getExtension(DestinationFactoryManager.class);
            if (StringUtils.isEmpty(address) 
                || address.startsWith("http") 
                || address.startsWith("jms")
                || address.startsWith("/")) {
                destinationFactory = mgr.getDestinationFactory(mapTransportURI(transId, address));
            } else {
                destinationFactory = mgr.getDestinationFactoryForUri(address);
            }
            return destinationFactory.getDestination(ei);
        } catch (BusException e) {
            IOException ex = new IOException("Could not find destination factory for transport " + transId);
            ex.initCause(e);
            throw ex;
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

    public EndpointInfo createEndpointInfo(ServiceInfo serviceInfo, BindingInfo b, 
                                           List<?> ees) {
        String transportURI = "http://schemas.xmlsoap.org/wsdl/soap/";
        if (b instanceof SoapBindingInfo) {
            SoapBindingInfo sbi = (SoapBindingInfo)b;
            transportURI = sbi.getTransportURI();
        }
        EndpointInfo info = new SoapEndpointInfo(serviceInfo, transportURI);
        
        if (ees != null) {
            for (Iterator itr = ees.iterator(); itr.hasNext();) {
                Object extensor = itr.next();
    
                if (SOAPBindingUtil.isSOAPAddress(extensor)) {
                    final SoapAddress sa = SOAPBindingUtil.getSoapAddress(extensor);
    
                    info.addExtensor(sa);
                    info.setAddress(sa.getLocationURI());
                    if (isJMSSpecAddress(sa.getLocationURI())) {
                        info.setTransportId(SoapJMSConstants.SOAP_JMS_SPECIFICIATION_TRANSPORTID);
                    }
                } else {
                    info.addExtensor(extensor);
                }
            }
        }
        
        return info;
    }


    public Conduit getConduit(EndpointInfo ei, EndpointReferenceType target) throws IOException {
        String address = target.getAddress().getValue();
        if (!StringUtils.isEmpty(address) && address.startsWith("soap.tcp://")) {
            //TODO - examine policies and stuff to look for the sun tcp policies
            return new TCPConduit(ei);
        }
        BindingInfo bi = ei.getBinding();
        String transId = ei.getTransportId();
        if (bi instanceof SoapBindingInfo) {
            transId = ((SoapBindingInfo)bi).getTransportURI();
            if (transId == null) {
                transId = ei.getTransportId();
            }
        }
        ConduitInitiator conduitInit;
        try {
            ConduitInitiatorManager mgr = bus.getExtension(ConduitInitiatorManager.class);
            if (StringUtils.isEmpty(address) || address.startsWith("http") || address.startsWith("jms")) {
                conduitInit = mgr.getConduitInitiator(mapTransportURI(transId, address));
            } else {
                conduitInit = mgr.getConduitInitiatorForUri(address);
            }
            if (conduitInit == null) {
                throw new RuntimeException("Could not find conduit initiator for transport "
                        + transId);
            }
            return conduitInit.getConduit(ei, target);
        } catch (BusException e) {
            throw new RuntimeException("Could not find conduit initiator for transport "
                                       + transId);
        }
    }

    public Conduit getConduit(EndpointInfo ei) throws IOException {
        return getConduit(ei, ei.getTarget());
    }

    @Resource(name = "cxf")
    public void setBus(Bus bus) {
        super.setBus(bus);
    }
    
    public void setActivationNamespaces(Collection<String> ans) {
        super.setTransportIds(new ArrayList<String>(ans));
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
