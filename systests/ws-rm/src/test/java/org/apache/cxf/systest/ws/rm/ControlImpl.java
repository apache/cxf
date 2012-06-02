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


package org.apache.cxf.systest.ws.rm;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.jws.WebService;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.ws.Endpoint;
import javax.xml.ws.Provider;
import javax.xml.ws.Service.Mode;
import javax.xml.ws.ServiceMode;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.XMLUtils;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;


@WebService(serviceName = "ControlService", 
            portName = "ControlPort", 
            endpointInterface = "org.apache.cxf.greeter_control.Control", 
            targetNamespace = "http://cxf.apache.org/greeter_control")
public class ControlImpl  extends org.apache.cxf.greeter_control.ControlImpl {
    
    private static final Logger LOG = LogUtils.getLogger(ControlImpl.class);
    
    String dbName = "rmdb";
    public void setDbName(String s) {
        dbName = s;
    }

    @Override
    public boolean startGreeter(String cfgResource) {
        SpringBusFactory bf = new SpringBusFactory();
        System.setProperty("db.name", dbName);
        greeterBus = bf.createBus(cfgResource);
        System.clearProperty("db.name");
        BusFactory.setDefaultBus(greeterBus);
        LOG.info("Initialised bus " + greeterBus + " with cfg file resource: " + cfgResource);
        LOG.fine("greeterBus inInterceptors: " + greeterBus.getInInterceptors());

        LoggingInInterceptor logIn = new LoggingInInterceptor();
        LoggingOutInterceptor logOut = new LoggingOutInterceptor();
        greeterBus.getInInterceptors().add(logIn);
        greeterBus.getOutInterceptors().add(logOut);
        greeterBus.getOutFaultInterceptors().add(logOut);

        if (cfgResource.indexOf("provider") == -1) {
            endpoint = Endpoint.publish(address, implementor);
            LOG.info("Published greeter endpoint.");
        } else {
            endpoint = Endpoint.publish(address, new GreeterProvider());
            LOG.info("Published greeter provider.");
        }
        
        return true;        
    }

    @WebService(serviceName = "GreeterService",
                portName = "GreeterPort",
                targetNamespace = "http://cxf.apache.org/greeter_control",
                wsdlLocation = "/wsdl/greeter_control.wsdl")
    @ServiceMode(Mode.PAYLOAD)
    public static class GreeterProvider implements Provider<Source> {

        public Source invoke(Source obj) {

            Node el;
            try {
                el = XMLUtils.fromSource(obj);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            if (el instanceof Document) {
                el = ((Document)el).getDocumentElement();
            }
            
            Map<String, String> ns = new HashMap<String, String>();
            ns.put("ns", "http://cxf.apache.org/greeter_control/types");
            XPathUtils xp = new XPathUtils(ns);
            String s = (String)xp.getValue("/ns:greetMe/ns:requestType",
                                           el,
                                           XPathConstants.STRING);

            if (s == null || "".equals(s)) {
                s = (String)xp.getValue("/ns:greetMeOneWay/ns:requestType",
                                        el,
                                        XPathConstants.STRING);
                System.out.println("greetMeOneWay arg: " + s);
                return null;
            } else {
                System.out.println("greetMe arg: " + s);
                String resp =
                    "<greetMeResponse "
                        + "xmlns=\"http://cxf.apache.org/greeter_control/types\">"
                        + "<responseType>" + s.toUpperCase() + "</responseType>"
                    + "</greetMeResponse>";
                return new StreamSource(new StringReader(resp));
            }
        }
    }    
}
