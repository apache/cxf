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

import javax.xml.namespace.QName;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.ws.AsyncHandler;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Dispatch;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.Holder;
import jakarta.xml.ws.Response;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.soap.AddressingFeature;
import jakarta.xml.ws.soap.SOAPBinding;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import jakarta.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.ServiceImpl;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.discovery.wsdl.AppSequenceType;
import org.apache.cxf.ws.discovery.wsdl.ByeType;
import org.apache.cxf.ws.discovery.wsdl.HelloType;
import org.apache.cxf.ws.discovery.wsdl.ObjectFactory;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.apache.cxf.ws.discovery.wsdl.ResolveMatchType;
import org.apache.cxf.ws.discovery.wsdl.ResolveMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ResolveType;
import org.apache.cxf.ws.discovery.wsdl.ScopesType;

/**
 *
 */
public class WSDiscoveryClient implements Closeable {

    public static final QName SERVICE_QNAME = new QName(WSDVersion.NS_1_1, "DiscoveryProxy");
    String address = "soap.udp://239.255.255.250:3702";
    String ipv6Address = "soap.udp://[FF02::C]:3702";
    boolean adHoc = true;
    AtomicInteger msgId = new AtomicInteger(1);
    long instanceId = System.currentTimeMillis();
    JAXBContext jaxbContext;
    ServiceImpl service;
    Dispatch<Object> dispatch;
    ObjectFactory factory = new ObjectFactory();
    final Bus bus;
    int defaultProbeTimeout = 1000;
    WSDVersion version = WSDVersion.INSTANCE_1_1;
    String soapVersion = SOAPBinding.SOAP12HTTP_BINDING;

    public WSDiscoveryClient() {
        this((Bus)null);
        setIpV6Address();
    }
    public WSDiscoveryClient(Bus b) {
        this.bus = b == null ? BusFactory.getThreadDefaultBus() : b;
        setIpV6Address();
    }
    public WSDiscoveryClient(String address) {
        this((Bus)null);
        resetDispatch(address);
    }
    public WSDiscoveryClient(Bus b, String address) {
        this(b);
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
            uncache();
            resetDispatch(a);
        }
    }


    /**
     * By default, CXF's WS-Discovery implementation is based on WS-Discovery 1.1.  Some devices will
     * not respond to 1.1 probes.  This allows CXF to use the WS-Discovery 1.0 namespaces and actions
     * which will allow older devices to be discovered.
     */
    public void setVersion10() {
        setVersion(true);
    }

    public void setVersion(boolean version10) {
        WSDVersion newv = version10 ? WSDVersion.INSTANCE_1_0 : WSDVersion.INSTANCE_1_1;
        if (newv != version) {
            version = newv;
            uncache();
        }
    }


    /**
     * WS-Discovery will use SOAP 1.2 by default.  This allows forcing the use of SOAP 1.1.
     */
    public void setSoapVersion11() {
        setSoapVersion(true);
    }


    public void setSoapVersion(boolean do11) {
        String newVer = do11 ? SOAPBinding.SOAP11HTTP_BINDING : SOAPBinding.SOAP12HTTP_BINDING;
        if (!soapVersion.equals(newVer)) {
            soapVersion = newVer;
            uncache();
        }
    }

    private void setIpV6Address() {
        String preferIPv4StackValue = System.getProperty("java.net.preferIPv4Stack");
        String preferIPv6AddressesValue = System.getProperty("java.net.preferIPv6Addresses");
        if ("true".equals(preferIPv6AddressesValue) && "false".equals(preferIPv4StackValue)) {
            address = "soap.udp://[FF02::C]:3702";
        }
    }
    private void uncache() {
        if (dispatch instanceof Closeable) {
            try {
                ((Closeable)dispatch).close();
            } catch (IOException e) {
                //ignorable
            }
        }
        dispatch = null;
        service = null;
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
    private synchronized ServiceImpl getService() {
        if (service == null) {
            Bus b = BusFactory.getAndSetThreadDefaultBus(bus);
            try {
                service = new ServiceImpl(bus, null,
                                          version.getServiceName(),
                                          Service.class);
                service.addPort(version.getServiceName(),
                                soapVersion, address);
            } finally {
                BusFactory.setThreadDefaultBus(b);
            }
        }
        return service;
    }
    private synchronized void resetDispatch(String newad) {
        address = newad;
        dispatch = null;
        service = null;
        adHoc = false;
        try {
            URI uri = new URI(address);
            if (StringUtils.isEmpty(uri.getHost())) {
                adHoc = true;
            } else {
                InetSocketAddress isa = new InetSocketAddress(uri.getHost(), uri.getPort());
                if (isa.getAddress().isMulticastAddress()) {
                    adHoc = true;
                }
            }
        } catch (URISyntaxException e) {
            //ignore
        }
    }

    private synchronized Dispatch<Object> getDispatchInternal(boolean addSeq, String action) {
        if (dispatch == null) {
            AddressingFeature f = new AddressingFeature(true, true);
            dispatch = getService().createDispatch(version.getServiceName(), getJAXBContext(), Service.Mode.PAYLOAD, f);
            dispatch.getRequestContext().put("thread.local.request.context", Boolean.TRUE);
            version.addVersionTransformer(dispatch);
        }
        addAddressing(dispatch, addSeq, action);
        return dispatch;
    }
    private void addAddressing(BindingProvider p, boolean addSeq, String action) {
        AddressingProperties addrProperties = new AddressingProperties();
        if (action != null) {
            AttributedURIType act = new AttributedURIType();
            act.setValue(action);
            addrProperties.setAction(act);
        }
        if (adHoc) {
            EndpointReferenceType to = new EndpointReferenceType();
            addrProperties.exposeAs(version.getAddressingNamespace());
            AttributedURIType epr = new AttributedURIType();
            epr.setValue(version.getToAddress());
            to.setAddress(epr);
            addrProperties.setTo(to);

            if (addSeq) {
                AppSequenceType s = new AppSequenceType();
                s.setInstanceId(instanceId);
                s.setMessageNumber(msgId.getAndIncrement());
                JAXBElement<AppSequenceType> seq = new ObjectFactory().createAppSequence(s);
                Header h = new Header(seq.getName(),
                                      seq,
                                      new JAXBDataBinding(getJAXBContext()));
                List<Header> headers = new ArrayList<>();
                headers.add(h);
                p.getRequestContext()
                    .put(Header.HEADER_LIST, headers);
            }
        } else {
            addrProperties.exposeAs(version.getAddressingNamespace());
        }
        p.getRequestContext().put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);
    }

    public synchronized void close() throws IOException {
        if (dispatch != null) {
            ((Closeable)dispatch).close();
            dispatch = null;
        }
        service = null;
    }
    @SuppressWarnings("deprecation")
    protected void finalize() throws Throwable {
        close();
        super.finalize();
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
        getDispatchInternal(true, version.getHelloAction()).invokeOneWay(factory.createHello(hello));
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
        getDispatchInternal(true, version.getByeAction()).invokeOneWay(factory.createBye(bye));
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
        List<EndpointReference> er = new ArrayList<>();
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
        Dispatch<Object> disp = this.getDispatchInternal(false, version.getProbeAction());
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
                            QName sn = version.getServiceName();
                            if (h.getTypes().contains(sn)
                                || h.getTypes().contains(new QName("", sn.getLocalPart()))) {
                                // A DiscoveryProxy wants us to flip to managed mode
                                uncache();
                                resetDispatch(h.getXAddrs().get(0));
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
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

    public ResolveMatchType resolve(W3CEndpointReference ref) {
        return resolve(ref, defaultProbeTimeout);
    }
    public ResolveMatchType resolve(W3CEndpointReference ref, int timeout) {
        Dispatch<Object> disp = this.getDispatchInternal(false, version.getResolveAction());
        ResolveType rt = new ResolveType();
        rt.setEndpointReference(ref);
        if (adHoc) {
            disp.getRequestContext().put("udp.multi.response.timeout", timeout);
            final Holder<ResolveMatchesType> response = new Holder<>();
            AsyncHandler<Object> handler = new AsyncHandler<Object>() {
                public void handleResponse(Response<Object> res) {
                    try {
                        Object o = res.get();
                        while (o instanceof JAXBElement) {
                            o = ((JAXBElement)o).getValue();
                        }
                        if (o instanceof ResolveMatchesType) {
                            response.value = (ResolveMatchesType)o;
                        } else if (o instanceof HelloType) {
                            HelloType h = (HelloType)o;
                            QName sn = version.getServiceName();
                            if (h.getTypes().contains(sn)
                                || h.getTypes().contains(new QName("", sn.getLocalPart()))) {
                                // A DiscoveryProxy wants us to flip to managed mode
                                uncache();
                                resetDispatch(h.getXAddrs().get(0));
                            }
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        // ?
                    }
                }
            };
            disp.invokeAsync(new ObjectFactory().createResolve(rt), handler);
            return response.value == null ? null : response.value.getResolveMatch();
        }
        Object o = disp.invoke(new ObjectFactory().createResolve(rt));
        while (o instanceof JAXBElement) {
            o = ((JAXBElement)o).getValue();
        }
        return o == null ? null : ((ResolveMatchesType)o).getResolveMatch();
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
