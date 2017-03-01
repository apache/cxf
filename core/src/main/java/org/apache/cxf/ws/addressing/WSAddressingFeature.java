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

import org.apache.cxf.Bus;
import org.apache.cxf.common.injection.NoJSR250Annotations;
import org.apache.cxf.feature.AbstractFeature;
import org.apache.cxf.interceptor.InterceptorProvider;

@NoJSR250Annotations
public class WSAddressingFeature extends AbstractFeature {
    public enum AddressingResponses {
        ALL,
        NON_ANONYMOUS,
        ANONYMOUS,
    }

    public interface WSAddressingFeatureApplier {
        void initializeProvider(WSAddressingFeature feature, InterceptorProvider provider, Bus bus);
    }

    boolean allowDuplicates = true;
    boolean usingAddressingAdvisory = true;
    boolean required;
    MessageIdCache cache;
    AddressingResponses responses = AddressingResponses.ALL;

    @Override
    protected void initializeProvider(InterceptorProvider provider, Bus bus) {
        bus.getExtension(WSAddressingFeatureApplier.class).initializeProvider(this, provider, bus);
    }

    public void setAllowDuplicates(boolean allow) {
        allowDuplicates = allow;
    }

    public boolean isAllowDuplicates() {
        return allowDuplicates;
    }

    public void setUsingAddressingAdvisory(boolean advisory) {
        usingAddressingAdvisory = advisory;
    }

    public boolean isUsingAddressingAdvisory() {
        return usingAddressingAdvisory;
    }


    public boolean isAddressingRequired() {
        return required;
    }
    public void setAddressingRequired(boolean r) {
        required = r;
    }

    /**
     * Returns the cache used to enforce duplicate message IDs when
     * {@link #isAllowDuplicates()} returns {@code false}.
     *
     * @return the cache used to enforce duplicate message IDs
     */
    public MessageIdCache getMessageIdCache() {
        return cache;
    }

    /**
     * Sets the cache used to enforce duplicate message IDs when
     * {@link #isAllowDuplicates()} returns {@code false}.
     *
     * @param messageIdCache the cache to use
     *
     * @throws NullPointerException if {@code messageIdCache} is {@code null}
     */
    public void setMessageIdCache(MessageIdCache messageIdCache) {
        cache = messageIdCache;
    }

    public void setResponses(AddressingResponses r) {
        responses = r;
    }
    public void setResponses(String r) {
        responses = AddressingResponses.valueOf(r);
    }
    public AddressingResponses getResponses() {
        return responses;
    }

}
