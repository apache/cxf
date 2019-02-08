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
package org.apache.cxf.testutil.common;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.List;
import java.util.Map;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ReflectionUtil;
import org.apache.cxf.wsdl.WSDLManager;

public class EmbeddedJMSBrokerLauncher extends AbstractBusTestServerBase {
    public static final String PORT = allocatePort(EmbeddedJMSBrokerLauncher.class);

    String brokerUrl1;
    BrokerService broker;
    String brokerName;

    public EmbeddedJMSBrokerLauncher() {
        this(null);
    }
    public EmbeddedJMSBrokerLauncher(String url) {
        if (url == null) {
            url = "tcp://localhost:" + PORT;
        }
        brokerUrl1 = url;
    }
    public void setBrokerName(String s) {
        brokerName = s;
    }

    public String getBrokerURL() {
        return brokerUrl1;
    }
    public String getEncodedBrokerURL() {
        StringBuilder b = new StringBuilder(brokerUrl1.length());
        for (int x = 0; x < brokerUrl1.length(); x++) {
            char c = brokerUrl1.charAt(x);
            switch (c) {
            case '?':
                b.append("%3F");
                break;
            default:
                b.append(c);
            }
        }
        return b.toString();
    }

    public void updateWsdl(Bus b, URL wsdlLocation) {
        updateWsdl(b, wsdlLocation.toString());
    }

    public void updateWsdl(Bus b, String wsdlLocation) {
        updateWsdlExtensors(b, wsdlLocation, brokerUrl1, getEncodedBrokerURL());
    }

    public static void updateWsdlExtensors(Bus bus,
                                           String wsdlLocation) {
        updateWsdlExtensors(bus, wsdlLocation, "tcp://localhost:" + PORT, null);
    }
    public static void updateWsdlExtensors(Bus bus,
                                           String wsdlLocation,
                                           String url,
                                           String encodedUrl) {
        try {
            if (encodedUrl == null) {
                encodedUrl = url;
            }
            if (bus == null) {
                bus = BusFactory.getThreadDefaultBus();
            }
            Definition def = bus.getExtension(WSDLManager.class)
                .getDefinition(wsdlLocation);
            Map<?, ?> map = def.getAllServices();
            for (Object o : map.values()) {
                Service service = (Service)o;
                Map<?, ?> ports = service.getPorts();
                adjustExtensibilityElements(service.getExtensibilityElements(), url, encodedUrl);

                for (Object p : ports.values()) {
                    Port port = (Port)p;
                    adjustExtensibilityElements(port.getExtensibilityElements(), url, encodedUrl);
                    adjustExtensibilityElements(port.getBinding().getExtensibilityElements(), url, encodedUrl);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void adjustExtensibilityElements(List<?> l,
                                                    String url,
                                                    String encodedUrl) {
        for (Object e : l) {
            if (e instanceof SOAPAddress) {
                String add = ((SOAPAddress)e).getLocationURI();
                int idx = add.indexOf("jndiURL=");
                if (idx != -1) {
                    int idx2 = add.indexOf("&", idx);
                    add = add.substring(0, idx)
                        + "jndiURL=" + encodedUrl
                        + (idx2 == -1 ? "" : add.substring(idx2));
                    ((SOAPAddress)e).setLocationURI(add);
                }
            } else if (e.getClass().getSimpleName().startsWith("JndiURLType")) {
                try {
                    e.getClass().getMethod("setValue", String.class).invoke(e, url);
                } catch (Exception ex) {
                    //ignore
                }
            } else {
                try {
                    Field f = e.getClass().getDeclaredField("jmsNamingProperty");
                    ReflectionUtil.setAccessible(f);
                    List<?> props = (List<?>)f.get(e);
                    for (Object prop : props) {
                        f = prop.getClass().getDeclaredField("name");
                        ReflectionUtil.setAccessible(f);
                        if ("java.naming.provider.url".equals(f.get(prop))) {
                            f = prop.getClass().getDeclaredField("value");
                            ReflectionUtil.setAccessible(f);
                            String value = (String)f.get(prop);
                            if (value == null || !value.startsWith("classpath")) {
                                f.set(prop, url);
                            }
                        }
                    }
                } catch (Exception ex) {
                    //ignore
                }
            }
        }
    }
    public void stop() throws Exception {
        tearDown();
    }
    public void tearDown() throws Exception {
        if (broker != null) {
            broker.stop();
        }
    }

    //START SNIPPET: broker
    public final void run() {
        try {
            broker = new BrokerService();
            broker.setPersistent(false);
            broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
            broker.setTmpDataDirectory(new File("./target"));
            broker.setUseJmx(false);
            if (brokerName != null) {
                broker.setBrokerName(brokerName);
            }
            broker.addConnector(brokerUrl1);
            broker.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    //END SNIPPET: broker

    public static void main(String[] args) {
        try {
            String url = null;
            if (args.length > 0) {
                url = args[0];
            }
            EmbeddedJMSBrokerLauncher s = new EmbeddedJMSBrokerLauncher(url);
            s.start();
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(-1);
        } finally {
            System.out.println("done!");
        }
    }
}
