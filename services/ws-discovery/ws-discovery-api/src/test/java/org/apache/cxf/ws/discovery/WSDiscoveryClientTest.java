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

import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import jakarta.jws.WebMethod;
import jakarta.jws.WebService;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.wsaddressing.W3CEndpointReference;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.TestUtil;
import org.apache.cxf.ws.discovery.internal.WSDiscoveryServiceImpl;
import org.apache.cxf.ws.discovery.wsdl.HelloType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchType;
import org.apache.cxf.ws.discovery.wsdl.ProbeMatchesType;
import org.apache.cxf.ws.discovery.wsdl.ProbeType;
import org.apache.cxf.ws.discovery.wsdl.ResolveMatchType;
import org.apache.cxf.ws.discovery.wsdl.ScopesType;

import org.junit.Assert;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assume.assumeThat;

/**
 *
 */
public final class WSDiscoveryClientTest {
    public static final String PORT = TestUtil.getPortNumber(WSDiscoveryClientTest.class);

    static NetworkInterface findIpv4Interface() throws Exception {
        Enumeration<NetworkInterface> ifcs = NetworkInterface.getNetworkInterfaces();
        List<NetworkInterface> possibles = new ArrayList<>();
        if (ifcs != null) {
            while (ifcs.hasMoreElements()) {
                NetworkInterface ni = ifcs.nextElement();
                if (ni.supportsMulticast() && ni.isUp()) {
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        if (ia.getAddress() instanceof java.net.Inet4Address && !ia.getAddress().isLoopbackAddress()
                                && !ni.getDisplayName().startsWith("vnic") 
                                && !ni.getDisplayName().startsWith("tailscale")) {
                            possibles.add(ni);
                            System.out.println("Found possible network interface:" + ni.getDisplayName());
                        }
                    }
                }
            }
        }
        for (NetworkInterface p : possibles) {
            if (p.isPointToPoint()) {
                System.out.println("Using p2p network interface:" + p.getDisplayName());
                return p;
            }
        }
        if (possibles.isEmpty())  {
            return null;
        } else {
            final NetworkInterface ni = possibles.get(possibles.size() - 1);
            System.out.println("Using network interface:" + ni.getDisplayName());
            return ni;
        }
    }

    @Test
    public void testMultiResponses() throws Exception {
        assumeThat("The UDP multicast is disabled on Windows by default",
            System.getProperties().getProperty("os.name"), not(startsWith("Windows")));

        // Disable the test on Redhat Enterprise Linux which doesn't enable the UDP broadcast by default
        if ("Linux".equals(System.getProperties().getProperty("os.name"))
            && System.getProperties().getProperty("os.version").indexOf("el") > 0) {
            System.out.println("Skipping MultiResponse test for REL");
            return;
        }

        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
        int count = 0;
        if (interfaces != null) {
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (!ni.isUp() || ni.isLoopback() || !ni.supportsMulticast()) {
                    continue;
                }
                count++;
            }
        }
        if (count == 0) {
            //no non-loopbacks, cannot do broadcasts
            System.out.println("Skipping MultiResponse test");
            return;
        }

        new Thread(new Runnable() {
            public void run() {
                try {
                    //fake a discovery server to send back some canned messages.
                    InetAddress address = InetAddress.getByName("239.255.255.250");
                    MulticastSocket s = new MulticastSocket(Integer.parseInt(PORT));
                    s.setBroadcast(true);
                    NetworkInterface ni = findIpv4Interface();
                    s.setNetworkInterface(ni);
                    s.setOption(StandardSocketOptions.IP_MULTICAST_LOOP, false);
                    s.setReuseAddress(true);
                    s.joinGroup(new InetSocketAddress(address, 0), ni);
                    s.setReceiveBufferSize(64 * 1024);
                    s.setSoTimeout(5000);
                    byte[] bytes = new byte[64 * 1024];
                    DatagramPacket p = new DatagramPacket(bytes, bytes.length, address, Integer.parseInt(PORT));
                    s.receive(p);
                    SocketAddress sa = p.getSocketAddress();
                    String incoming = new String(p.getData(), 0, p.getLength(), StandardCharsets.UTF_8);
                    int idx = incoming.indexOf("MessageID");
                    idx = incoming.indexOf('>', idx);
                    incoming = incoming.substring(idx + 1);
                    idx = incoming.indexOf("</");
                    incoming = incoming.substring(0, idx);
                    for (int x = 1; x < 4; x++) {
                        InputStream ins = WSDiscoveryClientTest.class.getResourceAsStream("msg" + x + ".xml");
                        String msg = IOUtils.readStringFromStream(ins);
                        msg = msg.replace("urn:uuid:883d0d53-92aa-4066-9b6f-9eadb1832366",
                                          incoming);
                        byte[] out = msg.getBytes(StandardCharsets.UTF_8);
                        DatagramPacket outp = new DatagramPacket(out, 0, out.length, sa);
                        s.send(outp);
                    }

                    s.close();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        }).start();

        Bus bus = BusFactory.newInstance().createBus();
        WSDiscoveryClient c = new WSDiscoveryClient(bus);
        c.setVersion10();
        c.setAddress("soap.udp://239.255.255.250:" + PORT);

        ProbeType pt = new ProbeType();
        ScopesType scopes = new ScopesType();
        pt.setScopes(scopes);
        ProbeMatchesType pmts = c.probe(pt, 1000);

        Assert.assertEquals(2, pmts.getProbeMatch().size());
        c.close();
        bus.shutdown(true);
    }


    //this is a standalone test
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

            bus = BusFactory.newInstance().createBus();
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
            if (pmts.getProbeMatch().isEmpty()) {
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
