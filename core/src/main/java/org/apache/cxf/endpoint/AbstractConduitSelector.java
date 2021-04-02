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

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import org.apache.cxf.BusException;
import org.apache.cxf.common.util.PropertyUtils;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.transport.Conduit;
import org.apache.cxf.transport.ConduitInitiator;
import org.apache.cxf.transport.ConduitInitiatorManager;
import org.apache.cxf.transport.MessageObserver;
import org.apache.cxf.ws.addressing.AttributedURIType;
import org.apache.cxf.ws.addressing.EndpointReferenceType;


/**
 * Abstract base class holding logic common to any ConduitSelector
 * that retrieves a Conduit from the ConduitInitiator.
 */
public abstract class AbstractConduitSelector implements ConduitSelector, Closeable {
    public static final String CONDUIT_COMPARE_FULL_URL
        = "org.apache.cxf.ConduitSelector.compareFullUrl";
    protected static final String KEEP_CONDUIT_ALIVE = "KeepConduitAlive";


    //collection of conduits that were created so we can close them all at the end
    protected List<Conduit> conduits = new CopyOnWriteArrayList<>();

   
    protected Endpoint endpoint;


    public AbstractConduitSelector() {
    }

    /**
     * Constructor, allowing a specific conduit to override normal selection.
     *
     * @param c specific conduit
     */
    public AbstractConduitSelector(Conduit c) {
        if (c != null) {
            conduits.add(c);
        }
    }

    public void close() {
        for (Conduit c : conduits) {
            c.close();
        }
        conduits.clear();
    }

    protected void removeConduit(Conduit conduit) {
        if (conduit != null) {
            conduit.close();
            conduits.remove(conduit);
        }
    }

    /**
     * Mechanics to actually get the Conduit from the ConduitInitiator
     * if necessary.
     *
     * @param message the current Message
     */
    protected Conduit getSelectedConduit(Message message) {
        Conduit c = findCompatibleConduit(message);
        if (c == null) {
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
                        c = createConduit(message, exchange, conduitInitiator);
                    } else {
                        getLogger().warning("ConduitInitiator not found: "
                                            + ei.getAddress());
                    }
                } else {
                    getLogger().warning("ConduitInitiatorManager not found");
                }
            } catch (BusException | IOException ex) {
                throw new Fault(ex);
            }
        }
        if (c != null && c.getTarget() != null && c.getTarget().getAddress() != null) {
            replaceEndpointAddressPropertyIfNeeded(message, c.getTarget().getAddress().getValue(), c);
        }
        //the search for the conduit could cause extra properties to be reset/loaded.
        message.resetContextCache();
        message.put(Conduit.class, c);
        return c;
    }

    protected Conduit createConduit(Message message, Exchange exchange, ConduitInitiator conduitInitiator)
        throws IOException {
        Conduit c;
        synchronized (endpoint) {
            if (!conduits.isEmpty()) {
                c = findCompatibleConduit(message);
                if (c != null) {
                    return c;
                }
            }
            EndpointInfo ei = endpoint.getEndpointInfo();
            String add = (String)message.get(Message.ENDPOINT_ADDRESS);
            String basePath = (String)message.get(Message.BASE_PATH);
            if (StringUtils.isEmpty(add)
                || add.equals(ei.getAddress())) {
                c = conduitInitiator.getConduit(ei, exchange.getBus());
                replaceEndpointAddressPropertyIfNeeded(message, add, c);
            } else {
                EndpointReferenceType epr = new EndpointReferenceType();
                AttributedURIType ad = new AttributedURIType();
                ad.setValue(StringUtils.isEmpty(basePath) ? add : basePath);
                epr.setAddress(ad);
                c = conduitInitiator.getConduit(ei, epr, exchange.getBus());
            }
            MessageObserver observer =
                exchange.get(MessageObserver.class);
            if (observer != null) {
                c.setMessageObserver(observer);
            } else {
                getLogger().warning("MessageObserver not found");
            }
            conduits.add(c);
        }
        return c;
    }

    // Some conduits may replace the endpoint address after it has already been prepared
    // but before the invocation has been done (ex, org.apache.cxf.clustering.LoadDistributorTargetSelector)
    // which may affect JAX-RS clients where actual endpoint address property may include additional path
    // segments.
    protected boolean replaceEndpointAddressPropertyIfNeeded(Message message,
                                                             String endpointAddress,
                                                             Conduit cond) {
        return false;
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
        // Clients expecting explicit InputStream responses
        // will need to keep low level conduits operating on InputStreams open
        // and will be responsible for closing the streams

        if (PropertyUtils.isTrue(exchange.get(KEEP_CONDUIT_ALIVE))) {
            return;
        }
        try {
            if (exchange.getInMessage() != null) {
                Conduit c = exchange.getOutMessage().get(Conduit.class);
                if (c == null) {
                    getSelectedConduit(exchange.getInMessage()).close(exchange.getInMessage());
                } else {
                    c.close(exchange.getInMessage());
                }
            }
        } catch (IOException e) {
            //IGNORE
        }
    }
    /**
     * @return the logger to use
     */
    protected abstract Logger getLogger();

    /**
     * If address protocol was changed, conduit should be re-initialised
     *
     * @param message the current Message
     */
    protected Conduit findCompatibleConduit(Message message) {
        Conduit c = message.get(Conduit.class);
        if (c == null
            && message.getExchange() != null
            && message.getExchange().getOutMessage() != null
            && message.getExchange().getOutMessage() != message) {
            c = message.getExchange().getOutMessage().get(Conduit.class);
        }
        if (c != null) {
            return c;
        }
        ContextualBooleanGetter cbg = new ContextualBooleanGetter(message);
        for (Conduit c2 : conduits) {
            if (c2.getTarget() == null
                || c2.getTarget().getAddress() == null
                || c2.getTarget().getAddress().getValue() == null) {
                continue;
            }
            String conduitAddress = c2.getTarget().getAddress().getValue();

            EndpointInfo ei = endpoint.getEndpointInfo();
            String actualAddress = ei.getAddress();

            String messageAddress = (String)message.get(Message.ENDPOINT_ADDRESS);
            if (messageAddress != null) {
                actualAddress = messageAddress;
            }

            if (matchAddresses(conduitAddress, actualAddress, cbg)) {
                return c2;
            }
        }
        for (Conduit c2 : conduits) {
            if (c2.getTarget() == null
                || c2.getTarget().getAddress() == null
                || c2.getTarget().getAddress().getValue() == null) {
                return c2;
            }
        }
        return null;
    }

    private boolean matchAddresses(String conduitAddress, String actualAddress, ContextualBooleanGetter cbg) {
        if (conduitAddress.length() == actualAddress.length()) {
            //let's be optimistic and try full comparison first, regardless of CONDUIT_COMPARE_FULL_URL value,
            //which can be expensive to fetch; as a matter of fact, anyway, if the addresses fully match,
            //their hosts also match
            if (conduitAddress.equalsIgnoreCase(actualAddress)) {
                return true;
            }
            return !cbg.isFullComparison() && matchAddressSubstrings(conduitAddress, actualAddress);
        }
        return !cbg.isFullComparison() && matchAddressSubstrings(conduitAddress, actualAddress);
    }

    //smart address substring comparison that tries to avoid building and comparing substrings unless strictly required
    private boolean matchAddressSubstrings(String conduitAddress, String actualAddress) {
        int idx = conduitAddress.indexOf(':');
        if (idx == actualAddress.indexOf(':')) {
            if (idx <= 0) {
                return true;
            }
            return conduitAddress.substring(0, idx).equalsIgnoreCase(actualAddress.substring(0, idx));
        }
        //no possible match as for sure the substrings before idx will be different
        return false;
    }

    private static final class ContextualBooleanGetter {
        private Boolean value;
        private final Message message;

        ContextualBooleanGetter(Message message) {
            this.message = message;
        }

        public boolean isFullComparison() {
            if (value == null) {
                value = MessageUtils.getContextualBoolean(message, CONDUIT_COMPARE_FULL_URL, false);
            }
            return value;
        }
    }
}
