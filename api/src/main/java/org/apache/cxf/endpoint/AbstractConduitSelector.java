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

package org.apache.cxf.endpoint;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.cxf.BusException;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;


/**
 * Abstract base class holding logic common to any ConduitSelector
 * that retreives a Conduit from the ConduitInitiator.
 */
public abstract class AbstractConduitSelector implements ConduitSelector {

    protected Conduit selectedConduit;
    protected Endpoint endpoint;

    /**
     * Constructor, allowing a specific conduit to override normal selection.
     * 
     * @param c specific conduit
     */
    public AbstractConduitSelector(Conduit c) {
        selectedConduit = c;
    }
        
    /**
     * Mechanics to actually get the Conduit from the ConduitInitiator
     * if necessary.
     * 
     * @param message the current Message
     */
    protected synchronized Conduit getSelectedConduit(Message message) {
        if (selectedConduit == null) {
            Exchange exchange = message.getExchange();
            EndpointInfo ei = endpoint.getEndpointInfo();
            String transportID = ei.getTransportId();
            try {
                ConduitInitiatorManager conduitInitiatorMgr = exchange.getBus()
                    .getExtension(ConduitInitiatorManager.class);
                if (conduitInitiatorMgr != null) {
                    ConduitInitiator conduitInitiator =
                        conduitInitiatorMgr.getConduitInitiator(transportID);
                    if (conduitInitiator != null) {
                        String add = (String)message.get(Message.ENDPOINT_ADDRESS);
                        if (StringUtils.isEmpty(add)
                            || add.equals(ei.getAddress())) {
                            selectedConduit = conduitInitiator.getConduit(ei);
                        } else {
                            EndpointReferenceType epr = new EndpointReferenceType();
                            AttributedURIType ad = new AttributedURIType();
                            ad.setValue(add);
                            epr.setAddress(ad);
                            selectedConduit = conduitInitiator.getConduit(ei, epr);
                        }
                        MessageObserver observer = 
                            exchange.get(MessageObserver.class);
                        if (observer != null) {
                            selectedConduit.setMessageObserver(observer);
                        } else {
                            getLogger().warning("MessageObserver not found");
                        }
                    } else {
                        getLogger().warning("ConduitInitiator not found: "
                                            + ei.getAddress());
                    }
                } else {
                    getLogger().warning("ConduitInitiatorManager not found");
                }
            } catch (BusException ex) {
                throw new Fault(ex);
            } catch (IOException ex) {
                throw new Fault(ex);
            }
        }
        return selectedConduit;
    }

    /**
     * @return the encapsulated Endpoint
     */
    public Endpoint getEndpoint() {
        return endpoint;
    }

    /**
     * @param ep the endpoint to encapsulate
     */
    public void setEndpoint(Endpoint ep) {
        endpoint = ep;
    }
    
    /**
     * Called on completion of the MEP for which the Conduit was required.
     * 
     * @param exchange represents the completed MEP
     */
    public void complete(Exchange exchange) {
        try {
            if (exchange.getInMessage() != null) {
                getSelectedConduit(exchange.getInMessage()).close(exchange.getInMessage());
            }
        } catch (IOException e) {
            //IGNORE
        }
    }    
    /**
     * @return the logger to use
     */
    protected abstract Logger getLogger();
}
