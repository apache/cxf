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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.util.JAXBSource;
import javax.xml.transform.Source;
import javax.xml.ws.Endpoint;
import javax.xml.ws.EndpointReference;
import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.soap.Addressing;

import org.apache.cxf.Bus;
import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.ws.discovery.wsdl.HelloType;
import org.apache.cxf.ws.discovery.wsdl.ObjectFactory;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;

public class WSDiscoveryService {
    Bus bus;
    Endpoint udpEndpoint;
    WSDiscoveryClient client;
    List<HelloType> registered = new ArrayList<HelloType>();
    ObjectFactory factory = new ObjectFactory();
    
    public WSDiscoveryService(Bus b) {
        bus = b;
        client = new WSDiscoveryClient();
    }
    
    public HelloType register(EndpointReference ref) {
        HelloType ht = client.register(ref); 
        registered.add(ht);
        return ht;
    }
    public void unregister(HelloType ht) {
        registered.remove(ht);
        client.unregister(ht); 
    }
    
    
    
    public void startup() {
        udpEndpoint = Endpoint.create(new WSDiscoveryProvider());
        udpEndpoint.publish("soap.udp://239.255.255.250:3702");
    }

    @WebServiceProvider(wsdlLocation = "classpath:/org/apache/cxf/ws/discovery/wsdl/wsdd-discovery-1.1-wsdl-os.wsdl",
        targetNamespace = "http://docs.oasis-open.org/ws-dd/ns/discovery/2009/01",
        serviceName = "Discovery",
        portName = "DiscoveryUDP")
    @XmlSeeAlso(ObjectFactory.class)
    @Addressing(required = true)
    class WSDiscoveryProvider implements Provider<Source> {
        
        JAXBContext context;
        public WSDiscoveryProvider() {
            try {
                context = JAXBContextCache.getCachedContextAndSchemas(ObjectFactory.class).getContext();
            } catch (JAXBException e) {
                e.printStackTrace();
            }
        }
        
        public Source invoke(Source request) {
            try {
                Object obj = context.createUnmarshaller().unmarshal(request);
                if (obj instanceof JAXBElement) {
                    obj = ((JAXBElement)obj).getValue();
                }
                if (obj instanceof ProbeType) {
                    ProbeMatchesType pmt = new ProbeMatchesType();
                    for (HelloType ht : registered) {
                        ProbeMatchType m = new ProbeMatchType();
                        m.setEndpointReference(ht.getEndpointReference());
                        m.setScopes(ht.getScopes());
                        m.setMetadataVersion(ht.getMetadataVersion());
                        m.getTypes().addAll(ht.getTypes());
                        m.getXAddrs().addAll(ht.getXAddrs());
                        pmt.getProbeMatch().add(m);
                    }
                    return new JAXBSource(context, factory.createProbeMatches(pmt));
                }
            } catch (JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
    }

}
