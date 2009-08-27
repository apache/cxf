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

package org.apache.cxf.configuration;

import java.util.Collection;
import java.util.List;

/**
 * 
 */
public interface ConfiguredBeanLocator {
    
    /**
     * Gets the names of all the configured beans of the specific type.  Does
     * not cause them to be loaded.
     * @param type
     * @return
     */
    List<String> getBeanNamesOfType(Class<?> type);
    
    /**
     * Gets all the configured beans of the specific types.  Causes them
     * all to be loaded. 
     * @param <T>
     * @param type
     * @return
     */
    <T> Collection<? extends T> getBeansOfType(Class<T> type);

    /**
     * Returns the bean of the given type and name.  Causes it to be loaded.
     * @param <T>
     * @param name
     * @param type
     * @return
     */
    <T> T getBeanOfType(String name, Class<T> type);
    
    /**
     * Iterates through the beans of the given type, calling the listener
     * to determine if it should be loaded or not. 
     * @param <T>
     * @param type
     * @param listener
     * @return
     */
    <T> boolean loadBeansOfType(Class<T> type, BeanLoaderListener<T> listener);

    /**
     * For supporting "legacy" config, checks the configured bean to see if
     * it has a property configured with the given name/value.  Mostly used 
     * for supporting things configured with "activationNamespaces" set. 
     * @param <T>
     * @param type
     * @param property
     * @param value
     * @return
     */
    boolean hasConfiguredPropertyValue(String beanName, String propertyName, String value);
    
    public interface BeanLoaderListener<T> {
        /**
         * Return true to have the loader go ahead and load the bean.  If false, 
         * the loader will just skip to the next bean
         * @param name
         * @param type
         * @return
         */
        boolean loadBean(String name, Class<? extends T> type);

        /**
         * Return true if the bean that was loaded meets the requirements at
         * which point, the loader will stop loading additional beans of the
         * given type
         * @param bean
         * @return
         */
        boolean beanLoaded(String name, T bean);
    }
}
