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

package org.apache.cxf.jaxws22;

import javax.xml.ws.EndpointContext;
import javax.xml.ws.WebServiceFeature;

import org.apache.cxf.Bus;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.transport.http_jaxws_spi.JAXWSHttpSpiTransportFactory;

/**
 * 
 */
public class EndpointImpl extends org.apache.cxf.jaxws.EndpointImpl {
    //What really is the point of the EndpointContext in JAX-WS 2.2?  
    //There is a setter, but no getter.
    Object endpointContext;
    
    /**
     * @param implementor
     */
    public EndpointImpl(Object implementor) {
        super(implementor);
    }

    /**
     * @param b
     * @param implementor
     * @param sf
     */
    public EndpointImpl(Bus b, Object implementor, JaxWsServerFactoryBean sf) {
        super(b, implementor, sf);
    }

    /**
     * @param b
     * @param i
     * @param bindingUri
     * @param wsdl
     */
    public EndpointImpl(Bus b, Object i, String bindingUri, String wsdl) {
        super(b, i, bindingUri, wsdl);
    }

    /**
     * @param b
     * @param i
     * @param bindingUri
     * @param wsdl
     * @param f
     */
    public EndpointImpl(Bus b, Object i, String bindingUri, String wsdl, WebServiceFeature[] f) {
        super(b, i, bindingUri, wsdl, f);
    }

    /**
     * @param b
     * @param i
     * @param bindingUri
     */
    public EndpointImpl(Bus b, Object i, String bindingUri) {
        super(b, i, bindingUri);
    }

    /**
     * @param b
     * @param i
     * @param bindingUri
     * @param features
     */
    public EndpointImpl(Bus b, Object i, String bindingUri, WebServiceFeature[] features) {
        super(b, i, bindingUri, features);
    }

    /**
     * @param bus
     * @param implementor
     */
    public EndpointImpl(Bus bus, Object implementor) {
        super(bus, implementor);
    }
    
    //new in 2.2, but introduces a new class not found in 2.1
    public void setEndpointContext(EndpointContext ctxt) {
        endpointContext = ctxt;
    }
    
    //new in 2.2, but introduces a new class not found in 2.1
    public void publish(javax.xml.ws.spi.http.HttpContext context) {
        ServerFactoryBean serverFactory = getServerFactory();
        if (serverFactory.getDestinationFactory() == null) {
            serverFactory.setDestinationFactory(new JAXWSHttpSpiTransportFactory(context));
        }
        super.publish(context.getPath());
    }
    
}
