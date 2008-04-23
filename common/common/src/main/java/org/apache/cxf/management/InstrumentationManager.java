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

package org.apache.cxf.management;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/** 
 *  InstrumentationManager interface for the instrumentations query, register 
 *  and unregister
 */
public interface InstrumentationManager {
    /**
     * Register a component with management infrastructure. Component will supply registration name.
     * @param component
     * @return name used to register the component
     * @throws JMException
     */
    ObjectName register(ManagedComponent component) throws JMException;
    
    /**
     * Register a component with management infrastructure. Component will supply registration name.
     * @param component
     * @param forceRegistration if set to true, then component will be registered despite existing component.
     * @return name used to register the component
     * @throws JMException
     */
    ObjectName register(ManagedComponent component, boolean forceRegistration) throws JMException;

    /**
     * Registers object with management infrastructure with a specific name. Object must be annotated or 
     * implement standard MBean interface.
     * @param obj
     * @param name
     * @throws JMException
     */
    void register(Object obj, ObjectName name) throws JMException;
    
    /**
     * Registers object with management infrastructure with a specific name. Object must be annotated or 
     * implement standard MBean interface.
     * @param obj
     * @param name
     * @param forceRegistration if set to true, then component will be registered despite existing component.
     * @throws JMException
     */
    void register(Object obj, ObjectName name, boolean forceRegistration) throws JMException;
    
    /**
     * Unregisters component with management infrastructure
     * @param component
     * @throws JMException
     */
    void unregister(ManagedComponent component) throws JMException;
    
    /**
     * Unregisters component based upon registered name
     * @param name
     * @throws JMException
     */
    void unregister(ObjectName name) throws JMException;

    /**
     * Cleans up and shutsdown management infrastructure.
     */
    void shutdown();
    
    /**
     * Get the MBeanServer which hosts managed components
     * NOTE: if the configuration is not set the JMXEnabled to be true, this method
     * will return null
     * @return the MBeanServer 
     */
    MBeanServer getMBeanServer();

}
