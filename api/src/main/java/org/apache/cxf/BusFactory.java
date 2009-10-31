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

package org.apache.cxf;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;

public abstract class BusFactory {
    
    public static final String BUS_FACTORY_PROPERTY_NAME = "org.apache.cxf.bus.factory";
    public static final String DEFAULT_BUS_FACTORY = "org.apache.cxf.bus.CXFBusFactory";

    protected static Bus defaultBus;
    protected static ThreadLocal<Bus> localBus = new ThreadLocal<Bus>();

    private static final Logger LOG = LogUtils.getL7dLogger(BusFactory.class, "APIMessages");
    

    /** 
     * Creates a new bus. 
     * While concrete <code>BusFactory</code> may offer differently
     * parameterized methods for creating a bus, all factories support
     * this no-arg factory method.
     *
     * @return the newly created bus.
     */
    public abstract Bus createBus();
    
    /**
     * Returns the default bus, creating it if necessary.
     * 
     * @return the default bus.
     */
    public static synchronized Bus getDefaultBus() {
        return getDefaultBus(true);
    }
    
    /**
     * Returns the default bus
     * @param createIfNeeded Set to true to create a default bus if one doesn't exist
     * @return the default bus.
     */
    public static synchronized Bus getDefaultBus(boolean createIfNeeded) {
        if (defaultBus == null
            && createIfNeeded) {
            defaultBus = newInstance().createBus();
        }
        return defaultBus;
    }
    
    /**
     * Sets the default bus.
     * @param bus the default bus.
     */
    public static synchronized void setDefaultBus(Bus bus) {
        defaultBus = bus;
        setThreadDefaultBus(bus);
    }
    
    
    /**
     * Sets the default bus for the thread.
     * @param bus the default bus.
     */
    public static void setThreadDefaultBus(Bus bus) {
        localBus.set(bus);
    }
    
    /**
     * Gets the default bus for the thread.
     * @return the default bus.
     */
    public static Bus getThreadDefaultBus() {
        return getThreadDefaultBus(true);
    }
    /**
     * Gets the default bus for the thread, creating if needed
     * @param createIfNeeded Set to true to create a default bus if one doesn't exist
     * @return the default bus.
     */
    public static Bus getThreadDefaultBus(boolean createIfNeeded) {
        if (createIfNeeded && localBus.get() == null) {
            Bus b = getDefaultBus(createIfNeeded);
            localBus.set(b);
        }
        return localBus.get();
    }

    /**
     * Sets the default bus if a default bus is not already set.
     * @param bus the default bus.
     * @return true if the bus was not set and is now set
     */
    public static synchronized boolean possiblySetDefaultBus(Bus bus) {
        if (localBus.get() == null) {
            localBus.set(bus);
        }
        
        if (defaultBus == null) {
            defaultBus = bus;            
            return true;
        }
        return false;
    }

    /**
     * Create a new BusFactory
     * 
     * The class of the BusFactory is determined by looking for the system propery: 
     * org.apache.cxf.bus.factory
     * or by searching the classpath for:
     * META-INF/services/org.apache.cxf.bus.factory
     * 
     * @return a new BusFactory to be used to create Bus objects
     */
    public static BusFactory newInstance() {
        return newInstance(null);
    }
    
    /**
     * Create a new BusFactory
     * @param className The class of the BusFactory to create.  If null, uses the
     * default search algorithm.
     * @return a new BusFactory to be used to create Bus objects
     */
    public static BusFactory newInstance(String className) {
        BusFactory instance = null;
        if (className == null) {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            className = getBusFactoryClass(loader);
            if (className == null && loader != BusFactory.class.getClassLoader()) {
                className = getBusFactoryClass(BusFactory.class.getClassLoader()); 
            }
        }
        if (className == null) {
            className = BusFactory.DEFAULT_BUS_FACTORY;
        }
        
        Class<? extends BusFactory> busFactoryClass;
        try {
            busFactoryClass = 
                ClassLoaderUtils.loadClass(className, BusFactory.class).asSubclass(BusFactory.class);
            instance = busFactoryClass.newInstance();
        } catch (Exception ex) {
            LogUtils.log(LOG, Level.SEVERE, "BUS_FACTORY_INSTANTIATION_EXC", ex);
            throw new RuntimeException(ex);
        }
        return instance;
    }

    protected void initializeBus(Bus bus) {
    }
    
    private static String getBusFactoryClass(ClassLoader classLoader) {
        
        String busFactoryClass = null;
        String busFactoryCondition = null;
        
        // next check system properties
        busFactoryClass = System.getProperty(BusFactory.BUS_FACTORY_PROPERTY_NAME);
        if (isValidBusFactoryClass(busFactoryClass)) {
            return busFactoryClass;
        }
    
        try {
            // next, check for the services stuff in the jar file
            String serviceId = "META-INF/services/" + BusFactory.BUS_FACTORY_PROPERTY_NAME;
            InputStream is = null;
        
            if (classLoader == null) {
                classLoader = Thread.currentThread().getContextClassLoader();
            }
        
            if (classLoader == null) {
                is = ClassLoader.getSystemResourceAsStream(serviceId);
            } else {
                is = classLoader.getResourceAsStream(serviceId);        
            }
            if (is == null) {
                serviceId = "META-INF/cxf/" + BusFactory.BUS_FACTORY_PROPERTY_NAME;
            
                if (classLoader == null) {
                    classLoader = Thread.currentThread().getContextClassLoader();
                }
            
                if (classLoader == null) {
                    is = ClassLoader.getSystemResourceAsStream(serviceId);
                } else {
                    is = classLoader.getResourceAsStream(serviceId);        
                }
            }
            
            
            if (is != null) {
                BufferedReader rd = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                busFactoryClass = rd.readLine();
                busFactoryCondition = rd.readLine();
                rd.close();
            }
            if (isValidBusFactoryClass(busFactoryClass)) {
                if (busFactoryCondition != null) {
                    try { 
                        classLoader.loadClass(busFactoryCondition);
                        return busFactoryClass;
                    } catch (ClassNotFoundException e) {
                        return DEFAULT_BUS_FACTORY;
                    }
                } else {
                    return busFactoryClass;
                }
            }
            return busFactoryClass;

        } catch (Exception ex) {
            LogUtils.log(LOG, Level.SEVERE, "FAILED_TO_DETERMINE_BUS_FACTORY_EXC", ex);
        } 
        return busFactoryClass;
    }
    
    private static boolean isValidBusFactoryClass(String busFactoryClassName) { 
        return busFactoryClassName != null && !"".equals(busFactoryClassName);
    }
    
}
 