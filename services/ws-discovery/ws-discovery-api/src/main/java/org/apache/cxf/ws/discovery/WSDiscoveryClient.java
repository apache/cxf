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

package org.apache.cxf.ws.discovery;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.AsyncHandler;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Dispatch;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Response;
import javax.xml.ws.Service;
import javax.xml.ws.soap.AddressingFeature;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.impl.AddressingPropertiesImpl;
import org.apache.cxf.ws.discovery.wsdl.AppSequenceType;
import org.apache.cxf.ws.discovery.wsdl.ByeType;
import org.apache.cxf.ws.discovery.wsdl.HelloType;
import org.apache.cxf.ws.discovery.wsdl.ObjectFactory;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.apache.cxf.ws.discovery.wsdl.ScopesType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

/**
 * 
 */
public class WSDiscoveryClient implements Closeable {
    public static final QName SERVICE_QNAME 
        = new QName("http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01", "DiscoveryProxy");
    
    String address = "soap.udp://239.255.255.250:3702";
    boolean adHoc = true;
    AtomicInteger msgId = new AtomicInteger(1);
    long instanceId = System.currentTimeMillis();
    JAXBContext jaxbContext;
    Service service;
    Dispatch<Object> dispatch;
    ObjectFactory factory = new ObjectFactory();
    Bus bus;
    int defaultProbeTimeout = 1000;
    
    public WSDiscoveryClient() {
    }
    public WSDiscoveryClient(Bus bus) {
        this.bus = bus;
    }
    public WSDiscoveryClient(String address) {
        resetDispatch(address);
    }
    
    public void setDefaultProbeTimeout(int i) {
        defaultProbeTimeout = i;
    }
    public int getDefaultProbeTimeout() {
        return defaultProbeTimeout;
    }
    
    public String getAddress() {
        return address;
    }
    public void setAddress(String a) {
        if (!address.equals(a)) {
            resetDispatch(a);
        }
    }
    
    
    private synchronized JAXBContext getJAXBContext() {
        if (jaxbContext == null) {
            try {
                jaxbContext = JAXBContextCache.getCachedContextAndSchemas(ObjectFactory.class).getContext();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        return jaxbContext;
    }
    private synchronized Service getService() {
        if (service == null) {
            Bus b = BusFactory.getAndSetThreadDefaultBus(bus);
            try {
                service = Service.create(SERVICE_QNAME);
                service.addPort(SERVICE_QNAME, SOAPBinding.SOAP12HTTP_BINDING, address);
            } finally {
                BusFactory.setThreadDefaultBus(b);
            }
        } 
        return service;
    }
    private synchronized void resetDispatch(String newad) {
        address = newad;
        service = null;
        dispatch = null;
        adHoc = false;
        try {
            URI uri = new URI(address);
            if (StringUtils.isEmpty(uri.getHost())) {
                adHoc = true;
            } else {
                InetSocketAddress isa = null;
                isa = new InetSocketAddress(uri.getHost(), uri.getPort());
                if (isa.getAddress().isMulticastAddress()) {
                    adHoc = true;
                }
            }        
        } catch (URISyntaxException e) {
            //ignore
        }
    }
    
    private synchronized Dispatch<Object> getDispatchInternal(boolean addSeq) {
        if (dispatch == null) {
            AddressingFeature f = new AddressingFeature(true, true);
            dispatch = getService().createDispatch(SERVICE_QNAME, getJAXBContext(), Service.Mode.PAYLOAD, f);
            dispatch.getRequestContext().put("thread.local.request.context", Boolean.TRUE);
        }
        addAddressing(dispatch, false);
        return dispatch;
    }
    private void addAddressing(BindingProvider p, boolean addSeq) {
        if (adHoc) {
            EndpointReferenceType to = new EndpointReferenceType();
            AddressingProperties addrProperties = new AddressingPropertiesImpl();
            AttributedURIType epr = new AttributedURIType();
            epr.setValue("urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01");
            to.setAddress(epr);
            addrProperties.setTo(to);
        
            p.getRequestContext()
                .put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);
            
            if (addSeq) {
                AppSequenceType s = new AppSequenceType();
                s.setInstanceId(instanceId);
                s.setMessageNumber(msgId.getAndIncrement());
                JAXBElement<AppSequenceType> seq = new ObjectFactory().createAppSequence(s);
                Header h = new Header(seq.getName(),
                                      seq,
                                      new JAXBDataBinding(getJAXBContext()));
                List<Header> headers = new ArrayList<Header>();
                headers.add(h);
                p.getRequestContext()
                    .put(Header.HEADER_LIST, headers);
            }
        }
    }
    
    public synchronized void close() throws IOException {
        if (dispatch != null) {
            ((Closeable)dispatch).close();
            dispatch = null;
        }
    }
    public void finalize() throws Throwable {
        super.finalize();
        close();
    }

    /**
     * Sends the "Hello" to broadcast the availability of a service
     * @param hello
     * @return the hello
     */
    public HelloType register(HelloType hello) {
        if (hello.getEndpointReference() == null) {
            hello.setEndpointReference(generateW3CEndpointReference());
        }
        getDispatchInternal(true).invokeOneWay(factory.createHello(hello));
        return hello;
    }
    
    /**
     * Sends the "Hello" to broadcast the availability of a service
     * @param ert The endpoint reference of the Service itself
     * @return the Hello that was used to broadcast the availability.
     */
    public HelloType register(EndpointReference ert) {
        HelloType hello = new HelloType();
        hello.setScopes(new ScopesType());
        hello.setMetadataVersion(1);
        EndpointReferenceType ref = ProviderImpl.convertToInternal(ert);
        proccessEndpointReference(ref, hello.getScopes(),
                                  hello.getTypes(),
                                  hello.getXAddrs());
        hello.setEndpointReference(generateW3CEndpointReference());
        return register(hello);
    }

    
    
    public void unregister(ByeType bye) {
        getDispatchInternal(true).invokeOneWay(factory.createBye(bye));
    }
    public void unregister(HelloType hello) {
        ByeType bt = new ByeType();
        bt.setScopes(hello.getScopes());
        bt.setEndpointReference(hello.getEndpointReference());
        unregister(bt);
    }
    
    public List<EndpointReference> probe() {
        return probe((QName)null);
    }
    public List<EndpointReference> probe(QName type) {
        ProbeType p = new ProbeType();
        if (type != null) {
            p.getTypes().add(type);
        }
        ProbeMatchesType pmt = probe(p, defaultProbeTimeout);
        List<EndpointReference> er = new ArrayList<EndpointReference>();
        for (ProbeMatchType pm : pmt.getProbeMatch()) {
            for (String add : pm.getXAddrs()) {
                W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
                builder.address(add);
                //builder.serviceName(type);
                //builder.endpointName(type);
                er.add(builder.build());
            }
        }
        return er;
    }    
    
    
    
    
    public ProbeMatchesType probe(ProbeType params) {
        return probe(params, defaultProbeTimeout);
    }    
    public ProbeMatchesType probe(ProbeType params, int timeout) {
        Dispatch<Object> disp = this.getDispatchInternal(false);
        if (adHoc) {
            disp.getRequestContext().put("udp.multi.response.timeout", timeout);
            final ProbeMatchesType response = new ProbeMatchesType();
            AsyncHandler<Object> handler = new AsyncHandler<Object>() {
                public void handleResponse(Response<Object> res) {
                    try {
                        Object o = res.get();
                        while (o instanceof JAXBElement) {
                            o = ((JAXBElement)o).getValue();
                        }
                        if (o instanceof ProbeMatchesType) {
                            response.getProbeMatch().addAll(((ProbeMatchesType)o).getProbeMatch());
                        } else if (o instanceof HelloType) {
                            HelloType h = (HelloType)o;
                            if (h.getTypes().contains(SERVICE_QNAME)
                                || h.getTypes().contains(new QName("", SERVICE_QNAME.getLocalPart()))) {
                                // A DiscoveryProxy wants us to flip to managed mode
                                resetDispatch(h.getXAddrs().get(0));
                            }
                        }
                    } catch (InterruptedException e) {
                        // ?
                    } catch (ExecutionException e) {
                        // ?
                    }
                }
            };
            disp.invokeAsync(new ObjectFactory().createProbe(params), handler);
            return response;
        }
        Object o = disp.invoke(new ObjectFactory().createProbe(params));
        while (o instanceof JAXBElement) {
            o = ((JAXBElement)o).getValue();
        }
        return (ProbeMatchesType)o;
    }
    
    
    private W3CEndpointReference generateW3CEndpointReference() {
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        builder.address(ContextUtils.generateUUID());
        return builder.build();
    }
    private void proccessEndpointReference(EndpointReferenceType ref,
                                           ScopesType scopes, 
                                           List<QName> types,
                                           List<String> xAddrs) {
        QName nm = EndpointReferenceUtils.getPortQName(ref, bus);
        scopes.getValue().add(nm.getNamespaceURI());
        types.add(nm);
        
        String wsdlLocation = EndpointReferenceUtils.getWSDLLocation(ref);
        if (!StringUtils.isEmpty(wsdlLocation)) {
            xAddrs.add(wsdlLocation);
        }
        String add = EndpointReferenceUtils.getAddress(ref);
        if (!StringUtils.isEmpty(add)
            && !xAddrs.contains(add)) {
            xAddrs.add(add);
        }
    }
    public boolean isAdHoc() {
        return adHoc;
    }
}
