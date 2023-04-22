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

package org.apache.cxf.ws.addressing;

import java.util.Collection;

import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.phase.PhaseInterceptor;

/**
 * Logical Handler responsible for aggregating the Message Addressing
 * Properties for outgoing messages.
 */
public class MAPAggregator extends AbstractPhaseInterceptor<Message> {
    public static final String USING_ADDRESSING = MAPAggregator.class.getName() + ".usingAddressing";
    public static final String ADDRESSING_DISABLED = MAPAggregator.class.getName() + ".addressingDisabled";
    public static final String DECOUPLED_DESTINATION = MAPAggregator.class.getName()
        + ".decoupledDestination";
    public static final String ACTION_VERIFIED = MAPAggregator.class.getName() + ".actionVerified";
    public static final String ADDRESSING_NAMESPACE = MAPAggregator.class.getName() + ".addressingNamespace";

    public interface MAPAggregatorLoader {
        MAPAggregator createImplementation(MAPAggregator mag);
    }

    protected MessageIdCache messageIdCache;
    protected boolean usingAddressingAdvisory = true;
    protected boolean addressingRequired;
    protected boolean allowDuplicates = true;
    protected WSAddressingFeature.AddressingResponses addressingResponses
        = WSAddressingFeature.AddressingResponses.ALL;

    /**
     * The real implementation of the MAPAggregator interceptor
     */
    private MAPAggregator impl;

    /**
     * Constructor.
     */
    public MAPAggregator() {
        super(MAPAggregator.class.getName(), Phase.PRE_LOGICAL);
        addBefore("org.apache.cxf.interceptor.OneWayProcessorInterceptor");
    }

    /**
     * Indicates if duplicate messageIDs are allowed.
     * @return true if duplicate messageIDs are allowed
     */
    public boolean allowDuplicates() {
        if (impl != null) {
            return impl.allowDuplicates();
        }
        return allowDuplicates;
    }

    /**
     * Allows/disallows duplicate messageIdDs.
     * @param ad whether duplicate messageIDs are allowed
     */
    public void setAllowDuplicates(boolean ad) {
        if (impl != null) {
            impl.setAllowDuplicates(ad);
        }
        allowDuplicates = ad;
    }

    /**
     * Whether the presence of the <wsaw:UsingAddressing> element
     * in the WSDL is purely advisory, i.e. its absence doesn't prevent
     * the encoding of WS-A headers.
     *
     * @return true if the presence of the <wsaw:UsingAddressing> element is
     * advisory
     */
    public boolean isUsingAddressingAdvisory() {
        if (impl != null) {
            return impl.isUsingAddressingAdvisory();
        }
        return usingAddressingAdvisory;
    }

    /**
     * Controls whether the presence of the <wsaw:UsingAddressing> element
     * in the WSDL is purely advisory, i.e. its absence doesn't prevent
     * the encoding of WS-A headers.
     *
     * @param advisory true if the presence of the <wsaw:UsingAddressing>
     * element is to be advisory
     */
    public void setUsingAddressingAdvisory(boolean advisory) {
        if (impl != null) {
            impl.setUsingAddressingAdvisory(advisory);
        }
        usingAddressingAdvisory = advisory;
    }

    /**
     * Whether the use of addressing is completely required for this endpoint
     *
     * @return true if addressing is required
     */
    public boolean isAddressingRequired() {
        if (impl != null) {
            return impl.addressingRequired;
        }
        return addressingRequired;
    }
    /**
     * Sets whether the use of addressing is completely required for this endpoint
     *
     */
    public void setAddressingRequired(boolean required) {
        if (impl != null) {
            impl.setAddressingRequired(required);
        }
        addressingRequired = required;
    }

    /**
     * Sets Addresing Response
     *
     */
    public void setAddressingResponses(WSAddressingFeature.AddressingResponses responses) {
        if (impl != null) {
            impl.setAddressingResponses(responses);
        }
        addressingResponses = responses;
    }

    /**
     * Returns the cache used to enforce duplicate message IDs when
     * {@link #allowDuplicates()} returns {@code false}.
     *
     * @return the cache used to enforce duplicate message IDs
     */
    public MessageIdCache getMessageIdCache() {
        if (impl != null) {
            return impl.getMessageIdCache();
        }
        return messageIdCache;
    }

    /**
     * Sets the cache used to enforce duplicate message IDs when
     * {@link #allowDuplicates()} returns {@code false}.
     *
     * @param messageIdCache the cache to use
     *
     * @throws NullPointerException if {@code messageIdCache} is {@code null}
     */
    public void setMessageIdCache(MessageIdCache messageIdCache) {
        if (messageIdCache == null) {
            throw new NullPointerException("messageIdCache cannot be null.");
        }
        if (impl != null) {
            impl.setMessageIdCache(messageIdCache);
        }
        this.messageIdCache = messageIdCache;
    }

    /**
     * Sets Addressing Response
     *
     */
    public WSAddressingFeature.AddressingResponses getAddressingResponses() {
        if (impl != null) {
            return impl.getAddressingResponses();
        }
        return addressingResponses;
    }

    /**
     * Invoked for normal processing of inbound and outbound messages.
     *
     * @param message the current message
     */
    public void handleMessage(Message message) {
        if (impl == null) {
            //load impl
            MAPAggregatorLoader loader = message.getExchange().getBus()
                .getExtension(MAPAggregatorLoader.class);
            impl = loader.createImplementation(this);
        }
        impl.handleMessage(message);
    }

    @Override
    public void handleFault(Message message) {
        if (impl != null) {
            impl.handleFault(message);
        }
    }


    @Override
    public Collection<PhaseInterceptor<? extends Message>> getAdditionalInterceptors() {
        if (impl != null) {
            return impl.getAdditionalInterceptors();
        }
        return super.getAdditionalInterceptors();
    }


}
