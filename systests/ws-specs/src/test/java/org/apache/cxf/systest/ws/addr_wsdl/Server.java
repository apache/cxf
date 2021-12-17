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

package org.apache.cxf.systest.ws.addr_wsdl;

import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Element;

import jakarta.annotation.Resource;
import jakarta.xml.ws.BindingProvider;
import jakarta.xml.ws.Endpoint;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service.Mode;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.soap.Addressing;
import org.apache.cxf.BusFactory;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxws.EndpointImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.testutil.common.AbstractBusTestServerBase;

public class Server extends AbstractBusTestServerBase {
    static final String PORT = allocatePort(Server.class);
    List<Endpoint> eps = new LinkedList<>();

    protected void run() {
        Object implementor = new AddNumberImpl();
        String address = "http://localhost:" + PORT + "/jaxws/add";

        EndpointImpl ep = new EndpointImpl(BusFactory.getThreadDefaultBus(),
                                           implementor,
                                           null,
                                           getWsdl());

        ep.publish(address);
        eps.add(ep);

        eps.add(Endpoint.publish(address + "-provider", new AddNumberProvider()));
        eps.add(Endpoint.publish(address + "-providernows", new AddNumberProviderNoWsdl()));
    }
    public void tearDown() {
        for (Endpoint ep : eps) {
            ep.stop();
        }
        eps = null;
    }

    private String getWsdl() {
        try {
            java.net.URL wsdl = getClass().getResource("/wsdl_systest_wsspec/add_numbers.wsdl");
            return wsdl.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
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


    @WebServiceProvider(serviceName = "AddNumbersService",
                        portName = "AddNumbersOnlyAnonPort",
                        targetNamespace = "http://apache.org/cxf/systest/ws/addr_feature/",
                        wsdlLocation = "/wsdl_systest_wsspec/add_numbers.wsdl")
    @ServiceMode(Mode.PAYLOAD)
    public static class AddNumberProvider implements Provider<Source> {

        public Source invoke(Source obj) {
            //CHECK the incoming
            Element el;
            try {
                el = StaxUtils.read(obj).getDocumentElement();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            Map<String, String> ns = new HashMap<>();
            ns.put("ns", "http://apache.org/cxf/systest/ws/addr_feature/");
            XPathUtils xp = new XPathUtils(ns);
            String o = (String)xp.getValue("/ns:addNumbers/ns:number1", el, XPathConstants.STRING);
            String o2 = (String)xp.getValue("/ns:addNumbers/ns:number2", el, XPathConstants.STRING);
            int i = Integer.parseInt(o);
            int i2 = Integer.parseInt(o2);

            String resp = "<addNumbersResponse xmlns=\"http://apache.org/cxf/systest/ws/addr_feature/\">"
                + "<return>" + (i + i2) + "</return></addNumbersResponse>";
            return new StreamSource(new StringReader(resp));
        }
    }

    @WebServiceProvider(serviceName = "AddNumbersService",
                        portName = "AddNumbersOnlyAnonPort",
                        targetNamespace = "http://apache.org/cxf/systest/ws/addr_feature/")
    @ServiceMode(Mode.PAYLOAD)
    @Addressing(enabled = true, required = true)
    public static class AddNumberProviderNoWsdl implements Provider<Source> {
        @Resource
        WebServiceContext ctx;

        public Source invoke(Source obj) {
            //CHECK the incoming

            Element el;
            try {
                el = StaxUtils.read(obj).getDocumentElement();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            Map<String, String> ns = new HashMap<>();
            ns.put("ns", "http://apache.org/cxf/systest/ws/addr_feature/");
            XPathUtils xp = new XPathUtils(ns);
            String o = (String)xp.getValue("/ns:addNumbers/ns:number1", el, XPathConstants.STRING);
            String o2 = (String)xp.getValue("/ns:addNumbers/ns:number2", el, XPathConstants.STRING);
            int i = Integer.parseInt(o);
            int i2 = Integer.parseInt(o2);


            ctx.getMessageContext()
                .put(BindingProvider.SOAPACTION_URI_PROPERTY,
                    "http://apache.org/cxf/systest/ws/addr_feature/AddNumbersPortType/addNumbersResponse");

            String resp = "<addNumbersResponse xmlns=\"http://apache.org/cxf/systest/ws/addr_feature/\">"
                + "<return>" + (i + i2) + "</return></addNumbersResponse>";
            return new StreamSource(new StringReader(resp));
        }
    }
}
