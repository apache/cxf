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
package org.apache.cxf.systest.http;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.List;

import javax.wsdl.Definition;
import javax.wsdl.Port;
import javax.wsdl.Service;
import javax.wsdl.extensions.soap.SOAPAddress;
import javax.wsdl.extensions.soap12.SOAP12Address;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;

import junit.framework.Assert;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.hello_world_soap_http.Greeter;
import org.junit.Test;


public class PublishedEndpointUrlTest extends Assert {
    
    @Test
    public void testPublishedEndpointUrl() throws Exception {
        
        Greeter implementor = new org.apache.hello_world_soap_http.GreeterImpl();
        String publishedEndpointUrl = "http://cxf.apache.org/publishedEndpointUrl";
        
        JaxWsServerFactoryBean svrFactory = new JaxWsServerFactoryBean();
        svrFactory.setServiceClass(Greeter.class);
        svrFactory.setAddress("http://localhost:9000/publishedEndpointUrl");
        svrFactory.setPublishedEndpointUrl(publishedEndpointUrl);
        svrFactory.setServiceBean(implementor);
        
        Server server = svrFactory.create();

        WSDLReader wsdlReader = WSDLFactory.newInstance().newWSDLReader();
        wsdlReader.setFeature("javax.wsdl.verbose", false);
        
        URL url = new URL(svrFactory.getAddress() + "?wsdl=1");
        HttpURLConnection connect = (HttpURLConnection)url.openConnection();
        assertEquals(500, connect.getResponseCode());
        
        Definition wsdl = wsdlReader.readWSDL(svrFactory.getAddress() + "?wsdl");
        assertNotNull(wsdl);
        
        Collection<Service> services = CastUtils.cast(wsdl.getAllServices().values());
        final String failMesg = "WSDL provided incorrect soap:address location";
        
        for (Service service : services) {
            Collection<Port> ports = CastUtils.cast(service.getPorts().values());
            for (Port port : ports) {
                List extensions = port.getExtensibilityElements();
                for (Object extension : extensions) {
                    String actualUrl = null;
                    if (extension instanceof SOAP12Address) {
                        actualUrl = ((SOAP12Address)extension).getLocationURI();
                    } else if (extension instanceof SOAPAddress) {
                        actualUrl = ((SOAPAddress)extension).getLocationURI();
                    }
                    
                    //System.out.println("Checking url: " + actualUrl + " against " + publishedEndpointUrl);
                    assertEquals(failMesg, publishedEndpointUrl, actualUrl);
                }
            }
        }
        
        server.stop();
    }


}
