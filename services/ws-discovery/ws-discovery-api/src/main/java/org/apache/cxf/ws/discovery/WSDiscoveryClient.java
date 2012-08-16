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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.wsaddressing.W3CEndpointReference;
import javax.xml.ws.wsaddressing.W3CEndpointReferenceBuilder;

import org.apache.cxf.Bus;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.spi.ProviderImpl;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.ContextUtils;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.impl.AddressingPropertiesImpl;
import org.apache.cxf.ws.discovery.wsdl.AppSequenceType;
import org.apache.cxf.ws.discovery.wsdl.ByeType;
import org.apache.cxf.ws.discovery.wsdl.DiscoveryProxy;
import org.apache.cxf.ws.discovery.wsdl.HelloType;
import org.apache.cxf.ws.discovery.wsdl.ObjectFactory;
import org.apache.cxf.ws.discovery.wsdl.ScopesType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

/**
 * 
 */
public class WSDiscoveryClient implements Closeable {
    
    DiscoveryProxy client;
    String address = "soap.udp://239.255.255.250:3702";
    boolean adHoc = true;
    AtomicInteger msgId = new AtomicInteger(1);
    long instanceId = System.currentTimeMillis();
    JAXBContext jaxbContext;
    Bus bus;
    
    public WSDiscoveryClient() {
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
    private synchronized DiscoveryProxy getClientInternal() {
        if (client == null) {
            JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
            if (bus != null) {
                factory.setBus(bus);
            }
            factory.setBindingId(SOAPBinding.SOAP12HTTP_BINDING);
            factory.setAddress(address);
            client = factory.create(DiscoveryProxy.class);
            ((BindingProvider)client).getRequestContext()
                .put("thread.local.request.context", Boolean.TRUE);
        }
        return client;
    }
    
    private DiscoveryProxy getClient() {
        DiscoveryProxy c = getClientInternal();
    
        if (adHoc) {
            EndpointReferenceType to = new EndpointReferenceType();
            AddressingProperties addrProperties = new AddressingPropertiesImpl();
            AttributedURIType epr = new AttributedURIType();
            epr.setValue("urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01");
            to.setAddress(epr);
            addrProperties.setTo(to);
        
            ((BindingProvider)c).getRequestContext()
                .put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);
            
            AppSequenceType s = new AppSequenceType();
            s.setInstanceId(instanceId);
            s.setMessageNumber(msgId.getAndIncrement());
            JAXBElement<AppSequenceType> seq = new ObjectFactory().createAppSequence(s);
            Header h = new Header(seq.getName(),
                                  seq,
                                  new JAXBDataBinding(getJAXBContext()));
            List<Header> headers = new ArrayList<Header>();
            headers.add(h);
            ((BindingProvider)c).getRequestContext()
                .put(Header.HEADER_LIST, headers);
        }
        return c;
    }
    
    public synchronized void close() throws IOException {
        if (client != null) {
            ((Closeable)client).close();
            client = null;
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
        DiscoveryProxy c = getClient();
        c.helloOp(hello);
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
        String add = EndpointReferenceUtils.getAddress(ref);
        hello.setEndpointReference(generateW3CEndpointReference(add));
        return register(hello);
    }

    
    
    public void unregister(ByeType bye) {
        DiscoveryProxy c = getClient();
        c.byeOp(bye);
    }
    public void unregister(HelloType hello) {
        ByeType bt = new ByeType();
        bt.setScopes(hello.getScopes());
        bt.setEndpointReference(hello.getEndpointReference());
        unregister(bt);
    }
    public void unregister(EndpointReference ert) {
        ByeType bt = new ByeType();
        bt.setScopes(new ScopesType());
        EndpointReferenceType ref = ProviderImpl.convertToInternal(ert);
        proccessEndpointReference(ref, bt.getScopes(),
                                  bt.getTypes(),
                                  bt.getXAddrs());
        String add = EndpointReferenceUtils.getAddress(ref);
        bt.setEndpointReference(generateW3CEndpointReference(add));
        unregister(bt);
    }
    
    
    private W3CEndpointReference generateW3CEndpointReference(String add) {
        W3CEndpointReferenceBuilder builder = new W3CEndpointReferenceBuilder();
        if (StringUtils.isEmpty(add)) {
            builder.address(ContextUtils.generateUUID());
        } else {
            builder.address(add);
        }
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

    

    
}
