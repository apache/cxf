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

package org.apache.cxf.service.invoker;

import org.apache.cxf.message.Exchange;

/**
 * Always returns a single instance of the bean.
 * 
 * This is generally the default.
 */
public class SingletonFactory implements Factory {
    Object bean;
    Factory factory;
    public SingletonFactory(final Object bean) {
        this.bean = bean;
    }
    public SingletonFactory(final Class<?> beanClass) {
        this.factory = new PerRequestFactory(beanClass);
    }
    public SingletonFactory(final Factory f) {
        this.factory = f;
    }

    /** {@inheritDoc}*/
    public Object create(Exchange ex) throws Throwable {
        if (bean == null && factory != null) {
            createBean(ex);
        }
        return bean;
    }

    private synchronized void createBean(Exchange e) throws Throwable {
        if (bean == null) {
            bean = factory.create(e);
        }
    }
    
    /** {@inheritDoc}*/
    public void release(Exchange ex, Object o) {
        //nothing to do
    }

}
