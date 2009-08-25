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

package org.apache.cxf.bus.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.configuration.ConfiguredBeanLocator;
import org.springframework.context.ApplicationContext;

/**
 * 
 */
public class SpringBeanLocator implements ConfiguredBeanLocator {
    ApplicationContext context;
    public SpringBeanLocator(ApplicationContext ctx) {
        context = ctx;
    }

    /** {@inheritDoc}*/
    public List<String> getBeanNamesOfType(Class<?> type) {
        return Arrays.asList(context.getBeanNamesForType(type));
    }

    /** {@inheritDoc}*/
    public <T> T getBeanOfType(String name, Class<T> type) {
        return type.cast(context.getBean(name, type));
    }

    /** {@inheritDoc}*/
    @SuppressWarnings("unchecked")
    public <T> Collection<? extends T> getBeansOfType(Class<T> type) {
        return context.getBeansOfType(type).values();
    }

    @SuppressWarnings("unchecked")
    public <T> boolean loadBeansOfType(Class<T> type,
                                       BeanLoaderListener<T> listener) {
        List<String> list = new ArrayList<String>(Arrays.asList(context.getBeanNamesForType(type)));
        Collections.reverse(list);
        for (String s : list) {
            Class<? extends T> c = context.getType(s);
            if (listener.loadBean(s, c)) {
                Object o = context.getBean(s);
                if (listener.beanLoaded(s, type.cast(o))) {
                    return true;
                }
            }
        }
        return false;
    }

}
