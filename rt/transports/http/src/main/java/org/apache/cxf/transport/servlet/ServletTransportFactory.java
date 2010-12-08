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


package org.apache.cxf.transport.servlet;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.cxf.transport.http.AbstractHTTPTransportFactory;
import org.apache.cxf.wsdl.http.AddressType;

public class ServletTransportFactory extends AbstractHTTPTransportFactory
    implements DestinationFactory {
   
    private ServletController controller;
    
    public ServletTransportFactory(Bus b) {
        super.setBus(b);
        List<String> ids = Arrays.asList(new String[] {
            "http://cxf.apache.org/transports/http",
            "http://cxf.apache.org/transports/http/configuration",
            "http://schemas.xmlsoap.org/wsdl/http",
            "http://schemas.xmlsoap.org/wsdl/http/",
        });
        this.setTransportIds(ids);
    }

    public ServletTransportFactory() {
    }
   
    public void setServletController(ServletController c) {
        controller = c;
    }

    @Resource(name = "cxf")
    public void setBus(Bus b) {
        super.setBus(b);
    }

    public Destination getDestination(EndpointInfo endpointInfo)
        throws IOException {
        AbstractHTTPDestination d = registry.getDestinationForPath(endpointInfo.getAddress());
        if (d == null) { 
            String path = registry.getTrimmedPath(endpointInfo.getAddress());
            d = new ServletDestination(getBus(), registry, endpointInfo, path);
            registry.addDestination(path, d);

            if (controller != null
                && !StringUtils.isEmpty(controller.getLastBaseURL())) {
                String ad = d.getEndpointInfo().getAddress();
                if (ad != null 
                    && (ad.equals(path)
                    || ad.equals(controller.getLastBaseURL() + path))) {
                    d.getEndpointInfo().setAddress(controller.getLastBaseURL() + path);
                    if (d.getEndpointInfo().getExtensor(AddressType.class) != null) {
                        d.getEndpointInfo().getExtensor(AddressType.class)
                            .setLocation(controller.getLastBaseURL() + path);
                    }
                }
            }
        }
        return d;
    }
  
}
