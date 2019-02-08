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

import javax.xml.ws.spi.http.HttpContext;

import org.apache.cxf.Bus;
import org.apache.cxf.binding.soap.SoapTransportFactory;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.http.DestinationRegistryImpl;

public class JAXWSHttpSpiTransportFactory extends SoapTransportFactory implements DestinationFactory {

    private HttpContext context;
    private JAXWSHttpSpiDestination destination;

    public JAXWSHttpSpiTransportFactory(HttpContext context) {
        super();
        this.context = context;
    }

    public Destination getDestination(EndpointInfo endpointInfo, Bus bus) throws IOException {
        if (destination == null) {
            destination = new JAXWSHttpSpiDestination(bus, new DestinationRegistryImpl(), endpointInfo);
            // set handler into the provided HttpContext, our Destination hook into the server container
            HttpHandlerImpl handler = new HttpHandlerImpl(destination);
            context.setHandler(handler);
        }
        return destination;
    }

}
