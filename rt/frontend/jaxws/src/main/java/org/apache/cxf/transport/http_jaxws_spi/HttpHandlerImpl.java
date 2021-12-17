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
package org.apache.cxf.transport.http_jaxws_spi;

import java.io.IOException;

import jakarta.xml.ws.spi.http.HttpExchange;
import jakarta.xml.ws.spi.http.HttpHandler;
import org.apache.cxf.binding.soap.wsdl.extensions.SoapAddress;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.wsdl.http.AddressType;

/**
 * A jakarta.xml.ws.spi.http.HttpHandler implementation that uses
 * a JAXWSHttpSpiDestination instance for message processing
 */
public class HttpHandlerImpl extends HttpHandler {

    private JAXWSHttpSpiDestination destination;

    public HttpHandlerImpl(JAXWSHttpSpiDestination destination) {
        this.destination = destination;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            //Update address in EndpointInfo; this can only happen here,
            //as the contextPath is provided in the HttpExchange only
            EndpointInfo ei = destination.getEndpointInfo();
            if (ei != null) {
                String ad = ei.getAddress();
                String path = exchange.getHttpContext().getPath();
                if (ad != null && ad.equals(path)) {
                    synchronized (ei) {
                        String contextPath = exchange.getContextPath();
                        ei.setAddress(contextPath + path);
                        if (ei.getExtensor(AddressType.class) != null) {
                            ei.getExtensor(AddressType.class).setLocation(contextPath + path);
                        } else if (ei.getExtensor(SoapAddress.class) != null) {
                            ei.getExtensor(SoapAddress.class).setLocationURI(contextPath + path);
                        }
                    }
                }
            }
            //service request
            destination.doService(new HttpServletRequestAdapter(exchange),
                                  new HttpServletResponseAdapter(exchange));
        } finally {
            exchange.close();
        }
    }

}
