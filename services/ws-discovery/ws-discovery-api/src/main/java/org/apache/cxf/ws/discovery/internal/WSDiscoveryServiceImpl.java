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

package org.apache.cxf.ws.discovery.internal;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.util.JAXBSource;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.soap.Addressing;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import jakarta.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.jaxb.JAXBUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.phase.PhaseInterceptorChain;
import org.apache.cxf.service.model.ServiceModelUtil;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.staxutils.transform.InTransformReader;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.discovery.WSDVersion;
import org.apache.cxf.ws.discovery.WSDiscoveryClient;
import org.apache.cxf.ws.discovery.WSDiscoveryService;
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

public class WSDiscoveryServiceImpl implements WSDiscoveryService {
    private static final Logger LOG = LogUtils.getL7dLogger(WSDiscoveryService.class);

    Bus bus;
    Endpoint udpEndpoint;
    WSDiscoveryClient client;
    List<HelloType> registered = new CopyOnWriteArrayList<>();
    ObjectFactory factory = new ObjectFactory();
    boolean started;

    public WSDiscoveryServiceImpl(Bus b) {
        bus = b == null ? BusFactory.newInstance().createBus() : b;
        client = new WSDiscoveryClient();
        update(bus.getProperties());
    }
    public WSDiscoveryServiceImpl() {
        bus = BusFactory.newInstance().createBus();
        client = new WSDiscoveryClient();
        update(bus.getProperties());
    }
    public WSDiscoveryServiceImpl(Bus b, Map<String, Object> props) {
        bus = b;
        client = new WSDiscoveryClient();
        update(props);
    }

    public final synchronized void update(Map<String, Object> props) {
        String address = (String)props.get("org.apache.cxf.service.ws-discovery.address");
        if (address != null) {
            client.setAddress(address);
        }
        if (udpEndpoint != null && !client.isAdHoc()) {
            udpEndpoint.stop();
            udpEndpoint = null;
            started = false;
        }
    }
    public WSDiscoveryClient getClient() {
        return client;
    }

    public HelloType register(EndpointReference ref) {
        startup(false);
        HelloType ht = client.register(ref);
        registered.add(ht);
        return ht;
    }
    public void register(HelloType ht) {
        startup(false);
        client.register(ht);
        registered.add(ht);
    }

    private Object getProperty(Server server, String s) {
        Object o = server.getEndpoint().get(s);
        if (o == null) {
            o = server.getEndpoint().getEndpointInfo().getProperty(s);
        }
        return o;
    }

    public void serverStarted(Server server) {
        Object o = getProperty(server, "ws-discovery-disable");
        if (o == Boolean.TRUE || Boolean.valueOf((String)o)) {
            return;
        }
        if (!startup(true)) {
            return;
        }
        HelloType ht = new HelloType();
        ht.setScopes(new ScopesType());
        ht.setMetadataVersion(1);


        o = getProperty(server, "ws-discovery-types");
        if (o instanceof QName) {
            ht.getTypes().add((QName)o);
        } else if (o instanceof List) {
            for (Object o2 : (List<?>)o) {
                if (o2 instanceof QName) {
                    ht.getTypes().add((QName)o2);
                } else if (o2 instanceof String) {
                    ht.getTypes().add(QName.valueOf((String)o2));
                }
            }
        } else if (o instanceof String) {
            ht.getTypes().add(QName.valueOf((String)o));
        }
        if (ht.getTypes().isEmpty()) {
            QName sn = ServiceModelUtil.getServiceQName(server.getEndpoint().getEndpointInfo());
            ht.getTypes().add(sn);
        }


        o = getProperty(server, "ws-discovery-scopes");
        if (o != null) {
            setScopes(ht, o);
        }
        setXAddrs(ht, server);
        String uuid = (String)getProperty(server, "ws-discovery-uuid");
        if (uuid != null) {
            //persistent uuid
            W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
            builder.address(uuid);
            ht.setEndpointReference(builder.build());
        }
        ht = client.register(ht);
        registered.add(ht);
        server.getEndpoint().put(HelloType.class.getName(), ht);
    }

    private void setXAddrs(HelloType ht, Server server) {
        String s = (String)getProperty(server, "ws-discovery-published-url");
        if (s == null) {
            s = (String)getProperty(server, "publishedEndpointUrl");
        }
        if (s == null) {
            s = server.getEndpoint().getEndpointInfo().getAddress();
        }
        if (s != null) {
            ht.getXAddrs().add(s);
        }
    }

    private void setScopes(HelloType ht, Object o) {
        if (o instanceof List) {
            List<?> l = (List)o;
            for (Object o2 : l) {
                ht.getScopes().getValue().add(o2.toString());
            }
        } else {
            ht.getScopes().getValue().add(o.toString());
        }
    }

    public void serverStopped(Server server) {
        HelloType ht = (HelloType)server.getEndpoint().get(HelloType.class.getName());
        if (ht != null) {
            unregister(ht);
        }
    }


    public void unregister(HelloType ht) {
        registered.remove(ht);
        client.unregister(ht);
    }


    public synchronized void startup() {
        startup(false);
    }
    public synchronized boolean startup(boolean optional) {
        String preferIPv4StackValue = System.getProperty("java.net.preferIPv4Stack");
        String preferIPv6AddressesValue = System.getProperty("java.net.preferIPv6Addresses");
        if (!started && client.isAdHoc()) {
            Bus b = BusFactory.getAndSetThreadDefaultBus(bus);
            try {
                udpEndpoint = new EndpointImpl(bus, new WSDiscoveryProvider());
                Map<String, Object> props = new HashMap<>();
                props.put("jaxws.provider.interpretNullAsOneway", "true");
                udpEndpoint.setProperties(props);
                if ("true".equals(preferIPv6AddressesValue) && "false".equals(preferIPv4StackValue)) {
                    try {
                        udpEndpoint.publish("soap.udp://[FF02::C]:3702");
                        started = true;
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Could not start WS-Discovery Service with ipv6 address", e);
                    }
                }
                if (!started) {
                    udpEndpoint.publish("soap.udp://239.255.255.250:3702");
                    started = true;
                }
            } catch (RuntimeException ex) {
                if (!optional) {
                    throw ex;
                }
                LOG.log(Level.WARNING, "Could not start WS-Discovery Service.", ex);
            } finally {
                if (b != bus) {
                    BusFactory.setThreadDefaultBus(b);
                }
            }
        }
        return true;
    }


    public ProbeMatchesType handleProbe(ProbeType pt) {
        List<HelloType> consider = new LinkedList<>(registered);
        //step one, consider the "types"
        //ALL types in the probe must be in the registered type
        if (pt.getTypes() != null && !pt.getTypes().isEmpty()) {
            ListIterator<HelloType> cit = consider.listIterator();
            while (cit.hasNext()) {
                HelloType ht = cit.next();
                boolean matches = true;
                for (QName qn : pt.getTypes()) {
                    if (!ht.getTypes().contains(qn)) {
                        matches = false;
                    }
                }
                if (!matches) {
                    cit.remove();
                }
            }
        }
        //next, consider the scopes
        matchScopes(pt, consider);

        if (consider.isEmpty()) {
            return null;
        }
        ProbeMatchesType pmt = new ProbeMatchesType();
        for (HelloType ht : consider) {
            ProbeMatchType m = new ProbeMatchType();
            m.setEndpointReference(ht.getEndpointReference());
            m.setScopes(ht.getScopes());
            m.setMetadataVersion(ht.getMetadataVersion());
            m.getTypes().addAll(ht.getTypes());
            m.getXAddrs().addAll(ht.getXAddrs());
            pmt.getProbeMatch().add(m);
        }
        return pmt;
    }


    private UUID toUUID(String scope) {
        URI uri = URI.create(scope);
        if (uri.getScheme() == null) {
            return UUID.fromString(scope);
        }
        if ("urn".equals(uri.getScheme())) {
            uri = URI.create(uri.getSchemeSpecificPart());
        }
        if ("uuid".equals(uri.getScheme())) {
            return UUID.fromString(uri.getSchemeSpecificPart());
        }
        return null;
    }

    private boolean compare(String s, String s2) {
        if (s != null) {
            return s.equalsIgnoreCase(s2);
        }
        return false;
    }
    private boolean matchURIs(URI probe, URI target) {
        if (compare(target.getScheme(), probe.getScheme())
            && compare(target.getAuthority(), probe.getAuthority())) {
            String[] ppath = probe.getPath().split("/");
            String[] tpath = target.getPath().split("/");

            if (ppath.length <= tpath.length) {
                for (int i = 0; i < ppath.length; i++) {
                    if (!ppath[i].equals(tpath[i])) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }


    private void matchScopes(ProbeType pt, List<HelloType> consider) {
        if (pt.getScopes() == null || pt.getScopes().getValue().isEmpty()) {
            return;
        }
        String mb = pt.getScopes().getMatchBy();
        if (mb == null) {
            mb = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01/rfc3986";
        }

        if (mb.startsWith(WSDVersion.NS_1_0)) {
            mb = mb.substring(WSDVersion.NS_1_0.length());
        } else if (mb.startsWith(WSDVersion.NS_1_1)) {
            mb = mb.substring(WSDVersion.NS_1_1.length());
        }

        ListIterator<HelloType> cit = consider.listIterator();
        while (cit.hasNext()) {
            HelloType ht = cit.next();
            boolean matches = false;

            if ("/rfc3986".equals(mb)) {
                matches = true;
                if (!pt.getScopes().getValue().isEmpty()) {
                    for (String ps : pt.getScopes().getValue()) {
                        boolean foundOne = false;
                        URI psuri = URI.create(ps);
                        for (String hts : ht.getScopes().getValue()) {
                            URI hturi = URI.create(hts);
                            if (matchURIs(psuri, hturi)) {
                                foundOne = true;
                            }
                        }
                        matches &= foundOne;
                    }
                }
            } else if ("/uuid".equals(mb)) {
                matches = true;
                if (!pt.getScopes().getValue().isEmpty()) {
                    for (String ps : pt.getScopes().getValue()) {
                        boolean foundOne = false;
                        UUID psuuid = toUUID(ps);
                        for (String hts : ht.getScopes().getValue()) {
                            UUID htuuid = toUUID(hts);
                            if (!htuuid.equals(psuuid)) {
                                foundOne = true;
                            }
                        }
                        matches &= foundOne;
                    }
                }
            } else if ("/ldap".equals(mb)) {
                //LDAP not supported
                if (!pt.getScopes().getValue().isEmpty()) {
                    matches = false;
                }
            } else if ("/strcmp0".equals(mb)) {
                matches = true;
                if (!pt.getScopes().getValue().isEmpty()) {
                    for (String s : pt.getScopes().getValue()) {
                        if (!ht.getScopes().getValue().contains(s)) {
                            matches = false;
                        }
                    }
                }
            } else if ("/none".equals(mb)
                && (ht.getScopes() == null || ht.getScopes().getValue().isEmpty())) {
                matches = true;
            }
            if (!matches) {
                cit.remove();
            }
        }
    }


    @WebServiceProvider(wsdlLocation = "classpath:/org/apache/cxf/ws/discovery/wsdl/wsdd-discovery-1.1-wsdl-os.wsdl",
        targetNamespace = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01",
        serviceName = "Discovery",
        portName = "DiscoveryUDP")
    @XmlSeeAlso(ObjectFactory.class)
    @Addressing(required = true)
    class WSDiscoveryProvider implements Provider<Source> {

        JAXBContext context;
        WSDiscoveryProvider() {
            try {
                context = JAXBContextCache.getCachedContextAndSchemas(ObjectFactory.class).getContext();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }

        private Source mapToOld(Document doc, JAXBElement<?> mt) throws JAXBException, XMLStreamException {
            doc.removeChild(doc.getDocumentElement());
            DOMResult result = new DOMResult(doc);
            XMLStreamWriter r = StaxUtils.createXMLStreamWriter(result);
            context.createMarshaller().marshal(mt, r);

            XMLStreamReader domReader = StaxUtils.createXMLStreamReader(doc);
            Map<String, String> inMap = new HashMap<>();
            inMap.put("{" + WSDVersion.INSTANCE_1_1.getNamespace() + "}*",
                      "{" + WSDVersion.INSTANCE_1_0.getNamespace() + "}*");
            inMap.put("{" + WSDVersion.INSTANCE_1_1.getAddressingNamespace() + "}*",
                      "{" + WSDVersion.INSTANCE_1_0.getAddressingNamespace() + "}*");

            InTransformReader reader = new InTransformReader(domReader, inMap, null, false);
            doc = StaxUtils.read(reader);
            return new DOMSource(doc);
        }
        private void updateOutputAction(String append) {
            AddressingProperties p = ContextUtils.retrieveMAPs(PhaseInterceptorChain.getCurrentMessage(),
                                                               false, false);
            AddressingProperties pout = new AddressingProperties();
            AttributedURIType action = new AttributedURIType();
            action.setValue(p.getAction().getValue() + append);
            pout.exposeAs(p.getNamespaceURI());
            pout.setAction(action);
            ContextUtils.storeMAPs(pout, PhaseInterceptorChain.getCurrentMessage(), true);

        }

        private Document mapFromOld(Document doc) throws XMLStreamException {
            XMLStreamReader domReader = StaxUtils.createXMLStreamReader(doc);
            Map<String, String> inMap = new HashMap<>();
            inMap.put("{" + WSDVersion.INSTANCE_1_0.getNamespace() + "}*",
                      "{" + WSDVersion.INSTANCE_1_1.getNamespace() + "}*");
            inMap.put("{" + WSDVersion.INSTANCE_1_0.getAddressingNamespace() + "}*",
                      "{" + WSDVersion.INSTANCE_1_1.getAddressingNamespace() + "}*");
            InTransformReader reader = new InTransformReader(domReader, inMap, null, false);
            doc = StaxUtils.read(reader);
            //System.out.println(StaxUtils.toString(doc));

            return doc;
        }

        public Source invoke(Source request) {
            Source ret = null;
            try {
                //Bug in JAXB - if you pass the StaxSource or SaxSource into unmarshall,
                //the namespaces for the QNames for the Types elements are lost.
                //Since WS-Discovery messages are small (UDP datagram size), parsing to DOM
                //is not a HUGE deal
                Document doc = StaxUtils.read(request);
                boolean mapToOld = false;
                if ("http://schemas.xmlsoap.org/ws/2005/04/discovery"
                    .equals(doc.getDocumentElement().getNamespaceURI())) {
                    //old version of ws-discovery, we'll transform this to newer version
                    doc = mapFromOld(doc);
                    mapToOld = true;
                }

                if (!"http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01"
                    .equals(doc.getDocumentElement().getNamespaceURI())) {
                    //not a proper ws-discovery message, ignore it
                    return null;
                }

                Object obj = JAXBUtils.unmarshall(context, doc.getDocumentElement());
                if (obj instanceof JAXBElement) {
                    obj = ((JAXBElement)obj).getValue();
                }
                if (obj instanceof ProbeType) {
                    ProbeMatchesType pmt = handleProbe((ProbeType)obj);
                    if (pmt == null) {
                        return null;
                    }
                    updateOutputAction("Matches");
                    if (mapToOld) {
                        ret = mapToOld(doc, factory.createProbeMatches(pmt));
                    } else {
                        ret = new JAXBSource(context, factory.createProbeMatches(pmt));
                    }
                } else if (obj instanceof ResolveType) {
                    ResolveMatchesType rmt = handleResolve((ResolveType)obj);
                    if (rmt == null) {
                        return null;
                    }
                    updateOutputAction("Matches");
                    if (mapToOld) {
                        ret = mapToOld(doc, factory.createResolveMatches(rmt));
                    } else {
                        ret = new JAXBSource(context, factory.createResolveMatches(rmt));
                    }
                } else if (obj instanceof HelloType) {
                    //check if it's a DiscoveryProxy
                    HelloType h = (HelloType)obj;
                    if (h.getTypes().contains(WSDiscoveryClient.SERVICE_QNAME)
                        || h.getTypes().contains(new QName("", WSDiscoveryClient.SERVICE_QNAME.getLocalPart()))) {
                        // A DiscoveryProxy wants us to flip to managed mode
                        try {
                            client.close();
                            client = new WSDiscoveryClient(h.getXAddrs().get(0));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (obj instanceof ByeType) {
                    ByeType h = (ByeType)obj;
                    if (h.getTypes().contains(WSDiscoveryClient.SERVICE_QNAME)
                        || h.getTypes().contains(new QName("", WSDiscoveryClient.SERVICE_QNAME.getLocalPart()))) {
                        // Our proxy has gone away, flip to ad-hoc
                        try {
                            if (client.getAddress().equals(h.getXAddrs().get(0))) {
                                client.close();
                                client = new WSDiscoveryClient();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (XMLStreamException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            return ret;
        }

        private ResolveMatchesType handleResolve(ResolveType resolve) {
            ResolveMatchType rmt = new ResolveMatchType();
            EndpointReference ref = resolve.getEndpointReference();
            EndpointReferenceType iref = ProviderImpl.convertToInternal(ref);
            for (HelloType hello : registered) {
                W3CEndpointReference r = hello.getEndpointReference();
                if (matches(iref, r)) {
                    rmt.setEndpointReference(r);
                    rmt.setScopes(hello.getScopes());
                    rmt.getTypes().addAll(hello.getTypes());
                    rmt.getXAddrs().addAll(hello.getXAddrs());
                    rmt.getOtherAttributes().putAll(hello.getOtherAttributes());
                    rmt.setMetadataVersion(hello.getMetadataVersion());
                    ResolveMatchesType rmts = new ResolveMatchesType();
                    rmts.setResolveMatch(rmt);
                    return rmts;
                }
            }
            return null;
        }

        private boolean matches(EndpointReferenceType ref, W3CEndpointReference r) {
            EndpointReferenceType cref = ProviderImpl.convertToInternal(r);
            QName snr = EndpointReferenceUtils.getServiceName(ref, bus);
            QName snc = EndpointReferenceUtils.getServiceName(cref, bus);
            String addr = EndpointReferenceUtils.getAddress(ref);
            String addc = EndpointReferenceUtils.getAddress(cref);

            if (addr == null) {
                return false;
            }
            if (addr.equals(addc)) {
                return !(snr != null && !snr.equals(snc));
            }
            return false;
        }
    }

}
