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

package org.apache.cxf.jaxrs.provider;

import java.util.ResourceBundle;
import java.util.logging.Logger;

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;
import org.apache.cxf.common.i18n.BundleUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;

@Provider
public class AbstractCachingMessageProvider<T> extends AbstractConfigurableProvider {

    protected static final Logger LOG = LogUtils.getL7dLogger(AbstractCachingMessageProvider.class);
    protected static final ResourceBundle BUNDLE = BundleUtils.getBundle(AbstractCachingMessageProvider.class);
    protected static final String ACTIVE_JAXRS_PROVIDER_KEY = "active.jaxrs.provider";

    protected MessageContext mc;
    private T object;

    @Context
    public void setMessageContext(MessageContext context) {
        this.mc = context;
    }


    protected boolean isProviderKeyNotSet() {
        return mc.get(ACTIVE_JAXRS_PROVIDER_KEY) == null;
    }

    public T getObject() {
        return object;
    }

    public void setObject(T object) {
        this.object = object;
    }



}
