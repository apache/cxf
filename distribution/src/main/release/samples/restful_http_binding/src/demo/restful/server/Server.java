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
package demo.restful.server;

import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.binding.http.HttpBindingFactory;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.invoker.BeanInvoker;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.apache.cxf.transport.MessageObserver;
import org.codehaus.jettison.mapped.MappedXMLInputFactory;
import org.codehaus.jettison.mapped.MappedXMLOutputFactory;

public final class Server {
    private Server() { }

    public static void main(String[] args) throws Exception {
        CustomerServiceImpl bs = new CustomerServiceImpl();

        createSoapService(bs);

        createRestService(bs);

        createJsonRestService(bs);

        serveHTML();

        System.out.println("Started CustomerService!");

        System.out.println("Server ready...");

        Thread.sleep(5 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);

    }

    private static void createRestService(Object serviceObj) {
        // Build up the server factory bean
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(CustomerService.class);
        // Use the HTTP Binding which understands the Java Rest Annotations
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:8080/xml/");
        sf.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));

        // Turn the "wrapped" style off. This means that CXF won't generate
        // wrapper XML elements and we'll have prettier XML text. This
        // means that we need to stick to one request and one response
        // parameter though.
        sf.getServiceFactory().setWrapped(false);

        sf.create();
    }

    private static void createJsonRestService(Object serviceObj) {
        // Build up the server factory bean
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(CustomerService.class);
        // Use the HTTP Binding which understands the Java Rest Annotations
        sf.setBindingId(HttpBindingFactory.HTTP_BINDING_ID);
        sf.setAddress("http://localhost:8080/json");
        sf.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));

        // Turn the "wrapped" style off. This means that CXF won't generate
        // wrapper JSON elements and we'll have prettier JSON text. This
        // means that we need to stick to one request and one response
        // parameter though.
        sf.getServiceFactory().setWrapped(false);

        // Tell CXF to use a different Content-Type for the JSON endpoint
        // This should probably be application/json, but text/plain allows
        // us to view easily in a web browser.
        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put("Content-Type", "text/plain");

        // Set up the JSON StAX implementation
        Map<String, String> nstojns = new HashMap<String, String>();
        nstojns.put("http://demo.restful.server", "acme");

        MappedXMLInputFactory xif = new MappedXMLInputFactory(nstojns);
        properties.put(XMLInputFactory.class.getName(), xif);

        MappedXMLOutputFactory xof = new MappedXMLOutputFactory(nstojns);
        properties.put(XMLOutputFactory.class.getName(), xof);

        sf.setProperties(properties);

        sf.create();
    }

    private static void createSoapService(Object serviceObj) {
        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(CustomerService.class);
        sf.setAddress("http://localhost:8080/soap");
        sf.getServiceFactory().setInvoker(new BeanInvoker(serviceObj));
        sf.getServiceFactory().setWrapped(false);

        sf.create();
    }

    /**
     * Serve out a static HTTP file because the Javascript XMLHttpRequest can
     * only work within one domain.
     */
    private static void serveHTML() throws Exception {
        Bus bus = BusFactory.getDefaultBus();
        DestinationFactoryManager dfm = bus.getExtension(DestinationFactoryManager.class);
        DestinationFactory df = dfm
            .getDestinationFactory("http://cxf.apache.org/transports/http/configuration");

        EndpointInfo ei = new EndpointInfo();
        ei.setAddress("http://localhost:8080/test.html");

        Destination d = df.getDestination(ei);
        d.setMessageObserver(new MessageObserver() {

            public void onMessage(Message message) {
                try {
                    // HTTP seems to need this right now...
                    ExchangeImpl ex = new ExchangeImpl();
                    ex.setInMessage(message);

                    Conduit backChannel = message.getDestination().getBackChannel(message, null, null);

                    MessageImpl res = new MessageImpl();
                    res.put(Message.CONTENT_TYPE, "text/html");
                    backChannel.prepare(res);

                    OutputStream out = res.getContent(OutputStream.class);
                    FileInputStream is = new FileInputStream("test.html");
                    IOUtils.copy(is, out, 2048);

                    out.flush();

                    out.close();
                    is.close();

                    backChannel.close(res);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });
    }
}
