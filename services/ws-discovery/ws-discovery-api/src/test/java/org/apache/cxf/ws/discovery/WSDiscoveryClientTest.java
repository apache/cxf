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

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.xml.ws.Endpoint;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.feature.LoggingFeature;
import org.apache.cxf.ws.discovery.internal.WSDiscoveryServiceImpl;
import org.apache.cxf.ws.discovery.wsdl.HelloType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.apache.cxf.ws.discovery.wsdl.ResolveMatchType;
import org.apache.cxf.ws.discovery.wsdl.ScopesType;


/**
 * 
 */
public final class WSDiscoveryClientTest {

    private WSDiscoveryClientTest() {
        
    }
    
    
    public static void main(String[] arg) throws Exception {
        try {
            Bus bus = BusFactory.getDefaultBus();
            Endpoint ep = Endpoint.publish("http://localhost:51919/Foo/Snarf", new FooImpl());
            WSDiscoveryServiceImpl service = new WSDiscoveryServiceImpl(bus);
            service.startup();
            
            //this service will just generate an error.  However, the probes should still
            //work to probe the above stuff.
            WSDiscoveryServiceImpl s2 = new WSDiscoveryServiceImpl() {
                public ProbeMatchesType handleProbe(ProbeType pt) {
                    throw new RuntimeException("Error!!!");
                }
            };
            s2.startup();
            HelloType h = service.register(ep.getEndpointReference());

            
            
            bus  = BusFactory.newInstance().createBus();
            new LoggingFeature().initialize(bus);
            WSDiscoveryClient c = new WSDiscoveryClient(bus);
            c.setVersion10();
            
            
            System.out.println("1");
            ProbeType pt = new ProbeType();
            ScopesType scopes = new ScopesType();
            pt.setScopes(scopes);
            ProbeMatchesType pmts = c.probe(pt, 1000);
            System.out.println("2");
            if  (pmts != null) {
                for (ProbeMatchType pmt : pmts.getProbeMatch()) {
                    System.out.println("Found " + pmt.getEndpointReference());
                    System.out.println(pmt.getTypes());
                    System.out.println(pmt.getXAddrs());
                }
            }
            if (pmts.getProbeMatch().size() == 0) {
                System.exit(0);
            }
            pmts = c.probe(pt);
            
            System.out.println("Size:" + pmts.getProbeMatch().size());
            
            System.out.println("3");

            W3CEndpointReference ref = null;
            if  (pmts != null) {
                for (ProbeMatchType pmt : pmts.getProbeMatch()) {
                    ref = pmt.getEndpointReference();
                    System.out.println("Found " + pmt.getEndpointReference());
                    System.out.println(pmt.getTypes());
                    System.out.println(pmt.getXAddrs());
                }
            }
            
            ResolveMatchType rmt = c.resolve(ref);
            System.out.println("Resolved " + rmt.getEndpointReference());
            System.out.println(rmt.getTypes());
            System.out.println(rmt.getXAddrs());

            service.unregister(h);
            System.out.println("4");
            c.close();
            
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
    

    @WebService
    public static class FooImpl {
        @WebMethod
        public int echo(int i) {
            return i;
        }
    }

    
}
