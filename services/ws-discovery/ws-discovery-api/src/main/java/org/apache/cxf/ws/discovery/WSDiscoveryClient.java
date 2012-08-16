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

import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.soap.SOAPBinding;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.Bus;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.impl.AddressingPropertiesImpl;
import org.apache.cxf.ws.discovery.wsdl.ByeType;
import org.apache.cxf.ws.discovery.wsdl.DiscoveryProxy;
import org.apache.cxf.ws.discovery.wsdl.HelloType;

/**
 * 
 */
public class WSDiscoveryClient implements Closeable {
    DiscoveryProxy client;
    String address = "soap.udp://:3702";
    Bus bus;
    
    public WSDiscoveryClient() {
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
    
        EndpointReferenceType to = new EndpointReferenceType();
        AddressingProperties addrProperties = new AddressingPropertiesImpl();
        AttributedURIType epr = new AttributedURIType();
        epr.setValue("urn:docs-oasis-open-org:ws-dd:ns:discovery:2009:01");
        to.setAddress(epr);
        addrProperties.setTo(to);
    
        ((BindingProvider)c).getRequestContext()
            .put(JAXWSAConstants.CLIENT_ADDRESSING_PROPERTIES, addrProperties);
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

    public void register(HelloType hello) {
        DiscoveryProxy c = getClient();
        c.helloOp(hello);
    }
    
    public void register(EndpointReference ert) {
        HelloType hello = new HelloType();
        hello.setEndpointReference(toW3CEndpointReference(ert));
        register(hello);
    }

    
    
    public void unregister(ByeType bye) {
        DiscoveryProxy c = getClient();
        c.byeOp(bye);
    }
    public void unregister(EndpointReference ert) {
        ByeType bt = new ByeType();
        bt.setEndpointReference(toW3CEndpointReference(ert));
        unregister(bt);
    }
    
    private W3CEndpointReference toW3CEndpointReference(EndpointReference ert) {
        if (ert instanceof W3CEndpointReference) {
            return (W3CEndpointReference)ert;
        }
        DOMResult res = new DOMResult();
        ert.writeTo(res);
        return new W3CEndpointReference(new DOMSource(res.getNode()));
    }
    
}
