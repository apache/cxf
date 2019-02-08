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

package org.apache.cxf.systest.jaxb;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.jws.WebService;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Service;
import javax.xml.ws.soap.MTOM;
import javax.xml.ws.soap.SOAPBinding;

import org.apache.cxf.ext.logging.Logging;
import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.ext.logging.LoggingOutInterceptor;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 */
public class MTOMBase64Test extends AbstractBusClientServerTestBase {
    static final String PORT = allocatePort(MTOMTest.class);
    static final String ADDRESS = "http://localhost:" + PORT + "/MTOMBase64";

    public static class ObjectWithHashMapData {
        private String name;
        private Map<String, byte[]> keyData = new LinkedHashMap<>();

        public ObjectWithHashMapData() {
        }

        @XmlJavaTypeAdapter(HashMapAdapter.class)
        public Map<String, byte[]> getKeyData() {
            return keyData;
        }

        public void setKeyData(Map<String, byte[]> d) {
            keyData = d;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @Logging(pretty = true)
    @WebService
    @MTOM(threshold = 1)
    public interface MTOMService {
        ObjectWithHashMapData getHashMapData(int y);
    }
    @WebService
    public static class MTOMServer implements MTOMService {
        public ObjectWithHashMapData getHashMapData(int y) {
            ObjectWithHashMapData ret = new ObjectWithHashMapData();
            ret.setName("Test");
            for (int x = 1; x < y; x++) {
                ret.getKeyData().put(Integer.toHexString(x), generateByteData(x));
            }
            return ret;
        }

        private byte[] generateByteData(int x) {
            byte[] bytes = new byte[x];
            for (int y = 0; y < x; y++) {
                int z = 'A' + y;
                if (z > 'z') {
                    z -= 'A';
                }
                bytes[y] = (byte)z;
            }
            return bytes;
        }
    }
    public static class Server extends AbstractBusTestServerBase {

        protected void run() {
            EndpointImpl endpointImpl = (EndpointImpl)Endpoint.publish(ADDRESS, new MTOMServer());
            endpointImpl.getProperties().put(Message.CONTENT_TRANSFER_ENCODING, "base64");
        }
        public static void main(String[] args) {
            try {
                Server s = new Server();
                s.start();
            } catch (Exception ex) {
                ex.printStackTrace();
                System.exit(-1);
            } finally {
                System.out.println("done!");
            }
        }
    }
    @BeforeClass
    public static void startServers() throws Exception {
        assertTrue("server did not launch correctly", launchServer(Server.class, true));
    }


    @Test
    public void testMTOMInHashMapWithBase64() throws Exception {
        Service service = Service.create(new QName("http://foo", "bar"));
        service.addPort(new QName("http://foo", "bar"), SOAPBinding.SOAP11HTTP_BINDING,
                        ADDRESS);
        MTOMService port = service.getPort(new QName("http://foo", "bar"),
                                           MTOMService.class);
        ClientProxy.getClient(port).getOutInterceptors().add(new LoggingOutInterceptor());
        ClientProxy.getClient(port).getInInterceptors().add(new LoggingInInterceptor());
        final int count = 99;
        ObjectWithHashMapData data = port.getHashMapData(count);
        for (int y = 1;  y < count; y++) {
            byte[] bytes = data.getKeyData().get(Integer.toHexString(y));
            assertEquals(y, bytes.length);
        }
    }
}
