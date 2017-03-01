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

package org.apache.cxf.jaxrs.lifecycle;

import java.lang.reflect.Constructor;
import java.util.Collections;

import org.apache.cxf.common.util.ClassHelper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.factory.ServiceConstructionException;

/**
 * The default singleton resource provider which returns
 * the same resource instance per every request
 */
public class SingletonResourceProvider implements ResourceProvider {
    private Object resourceInstance;
    private boolean callPostConstruct;
    
    public SingletonResourceProvider(Object o, boolean callPostConstruct) {
        resourceInstance = o;
        this.callPostConstruct = callPostConstruct;
    }

    public SingletonResourceProvider(Object o) {
        this(o, false);
    }

    public void init(Endpoint ep) {
        if (resourceInstance instanceof Constructor) {
            Constructor<?> c = (Constructor<?>)resourceInstance;
            Message m = new MessageImpl();
            ExchangeImpl exchange = new ExchangeImpl();
            exchange.put(Endpoint.class, ep);
            m.setExchange(exchange);
            Object[] values = 
                ResourceUtils.createConstructorArguments(c, m, false, Collections.emptyMap());
            try {
                resourceInstance = values.length > 0 ? c.newInstance(values) : c.newInstance(new Object[]{});
            } catch (Exception ex) {
                throw new ServiceConstructionException(ex);
            }
        }
        if (callPostConstruct) {
            InjectionUtils.invokeLifeCycleMethod(resourceInstance,
                ResourceUtils.findPostConstructMethod(ClassHelper.getRealClass(resourceInstance)));
        }    
    }
    
    /**
     * {@inheritDoc}
     */
    public boolean isSingleton() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public Object getInstance(Message m) {
        return resourceInstance;
    }

    /**
     * {@inheritDoc}
     */
    public void releaseInstance(Message m, Object o) {
        // complete
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getResourceClass() {
        return ClassHelper.getRealClass(resourceInstance);
    }

}
