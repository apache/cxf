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

package org.apache.cxf.ws.mex;


import javax.xml.namespace.QName;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.binding.soap.SoapBindingConstants;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxws.JAXWSMethodInvoker;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.addressing.AddressingProperties;
import org.apache.cxf.ws.addressing.JAXWSAConstants;
import org.apache.cxf.ws.addressing.MAPAggregator;
import org.apache.cxf.ws.addressing.WSAddressingFeature;
import org.apache.cxf.ws.policy.AssertionInfoMap;

/**
 * 
 */
public class MEXInInterceptor extends AbstractPhaseInterceptor<Message> {
    final MEXEndpoint ep;
    Endpoint mexEndpoint;
    
    public MEXInInterceptor(Server serv) {
        super(Phase.PRE_PROTOCOL);
        ep = new MEXEndpoint(serv);
    }

    public void handleMessage(Message message) throws Fault {
        String action = (String)message.get(SoapBindingConstants.SOAP_ACTION);
        if (action == null) {
            AddressingProperties inProps = (AddressingProperties)message
                .getContextualProperty(JAXWSAConstants.SERVER_ADDRESSING_PROPERTIES_INBOUND);
            if (inProps != null && inProps.getAction() != null) {
                action = inProps.getAction().getValue();
            }
        }
        if ("http://schemas.xmlsoap.org/ws/2004/09/transfer/Get".equals(action)
            || "http://schemas.xmlsoap.org/ws/2004/09/mex/GetMetadata/Request".equals(action)) {
            message.remove(AssertionInfoMap.class);
            Exchange ex = message.getExchange();
            Endpoint endpoint = createEndpoint(message);
            ex.put(Endpoint.class, endpoint);
            ex.put(Service.class, endpoint.getService());
            ex.put(org.apache.cxf.binding.Binding.class, endpoint.getBinding());
            ex.put(BindingOperationInfo.class, 
                   endpoint.getBinding().getBindingInfo()
                       .getOperation(new QName("http://mex.ws.cxf.apache.org/", "Get2004")));
            ex.remove(BindingOperationInfo.class);
            message.put(MAPAggregator.ACTION_VERIFIED, Boolean.TRUE);
            message.getInterceptorChain().add(endpoint.getInInterceptors());
            message.getInterceptorChain().add(endpoint.getBinding().getInInterceptors());
        }

    }

    private static class MEXJaxWsServerFactoryBean extends JaxWsServerFactoryBean {
        public MEXJaxWsServerFactoryBean(Bus b) {
            setServiceClass(MEXEndpoint.class);
            setServiceName(new QName("http://mex.ws.cxf.apache.org/", "MEXEndpoint"));
            setBus(b);
        }
        public Endpoint createEndpoint() throws BusException, EndpointException {
            Endpoint ep = super.createEndpoint();
            new WSAddressingFeature().initialize(ep, getBus());
            return ep;
        }        
    }
    private synchronized Endpoint createEndpoint(Message message) {
        if (mexEndpoint == null) {
            MEXJaxWsServerFactoryBean factory 
                = new MEXJaxWsServerFactoryBean(message.getExchange().getBus());
            try {
                Endpoint endpoint = factory.createEndpoint();
                endpoint.getService().setInvoker(new JAXWSMethodInvoker(ep));
                
                mexEndpoint = endpoint;
            } catch (Exception ex) {
                throw new Fault(ex);
            }
        }
        return mexEndpoint;
    }
}
