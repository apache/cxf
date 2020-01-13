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

package org.apache.cxf.microprofile.client.cdi;

import java.util.NoSuchElementException;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;

import org.apache.cxf.Bus;


public final class CDIUtils {

    private CDIUtils() {
    }

    static BeanManager getCurrentBeanManager(Bus bus) {
        BeanManager bm = bus.getExtension(BeanManager.class);
        if (bm == null) {
            bm = getCurrentBeanManager();
            bus.setExtension(bm, BeanManager.class);
        }
        return bm;
    }

    static BeanManager getCurrentBeanManager() {
        return CDI.current().getBeanManager();
    }


    static <T> T getInstanceFromCDI(Class<T> clazz) {
        return getInstanceFromCDI(clazz, null);
    }
    
    static <T> T getInstanceFromCDI(Class<T> clazz, Bus bus) {
        T t;
        try {
            t = findBean(clazz, bus);
        } catch (ExceptionInInitializerError | NoClassDefFoundError | IllegalStateException ex) {
            // expected if no CDI implementation is available
            t = null;
        } catch (NoSuchElementException ex) {
            // expected if ClientHeadersFactory is not managed by CDI
            t = null;
        }
        return t;
    }

    @SuppressWarnings("unchecked")
    private static <T> T findBean(Class<T> clazz, Bus bus) {
        BeanManager beanManager = bus == null ? getCurrentBeanManager() : getCurrentBeanManager(bus);
        Bean<?> bean = beanManager.getBeans(clazz).iterator().next();
        CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
        T instance = (T) beanManager.getReference(bean, clazz, ctx);
        return instance;
    }
}