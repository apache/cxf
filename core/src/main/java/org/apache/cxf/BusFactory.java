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
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.SystemPropertyAction;

/**
 * Factory to create CXF Bus objects.
 * <p>CXF includes a large number of components that provide services, such
 * as WSDL parsing, and message processing. To avoid creating these objects over and over, and to
 * allow them to be shared easily, they are associated with a data structure called a bus.
 * </p>
 * <p>
 * You don't ever have to explicitly create or manipulate bus objects. If you simply use the CXF
 * or JAX-WS APIs to create clients or servers, CXF will create a default bus for you. You can create a bus
 * explicitly if you need to customize components on the bus or maintain several independent buses
 * with independent configurations.
 * </p>
 * <p>
 * This class maintains the default bus for the entire process and a set of thread-default buses. All CXF
 * components that reference the bus, which is to say all CXF components, will obtain a default bus from this
 * class if you do not set a specific bus.
 * </p>
 * <p>
 * If you create a bus when there is no default bus in effect, that bus will become the default bus.
 * </p>
 * <p>
 * This class holds a reference to the global default bus and a reference to each thread default
 * bus. The thread references are weak with respect to the threads, but otherwise ordinary.
 * Thus, so long as the thread remains alive
 * there will be a strong reference to the bus, and it will not get garbage-collected.
 * If you want to recover memory used CXF, you can set
 * the default and per-thread default bus to null, explicitly.
 * </p>
 */
public abstract class BusFactory {

    public static final String BUS_FACTORY_PROPERTY_NAME = "org.apache.cxf.bus.factory";
    public static final String DEFAULT_BUS_FACTORY = "org.apache.cxf.bus.CXFBusFactory";

    protected static Bus defaultBus;

    static class BusHolder {
        Bus bus;
        volatile boolean stale;
    }

    protected static final Map<Thread, BusHolder> THREAD_BUSSES = new WeakHashMap<>();
    protected static final ThreadLocal<BusHolder> THREAD_BUS = new ThreadLocal<>();

    private static final Logger LOG = LogUtils.getL7dLogger(BusFactory.class);

    /**
     * Creates a new bus. While concrete <code>BusFactory</code> may offer differently parameterized methods
     * for creating a bus, all factories support this no-arg factory method.
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
     *
     * @param createIfNeeded Set to true to create a default bus if one doesn't exist
     * @return the default bus.
     */
    public static synchronized Bus getDefaultBus(boolean createIfNeeded) {
        if (defaultBus == null && createIfNeeded) {
            defaultBus = newInstance().createBus();
        }
        if (defaultBus == null) {
            // never set up.
            return null;
        }
        return defaultBus;
    }

    private static BusHolder getThreadBusHolder(boolean set) {
        BusHolder h = THREAD_BUS.get();
        if (h == null || h.stale) {
            Thread cur = Thread.currentThread();
            synchronized (THREAD_BUSSES) {
                h = THREAD_BUSSES.get(cur);
            }
            if (h == null || h.stale) {
                h = new BusHolder();

                synchronized (THREAD_BUSSES) {
                    THREAD_BUSSES.put(cur, h);
                }
            }
            if (set) {
                THREAD_BUS.set(h);
            }
        }
        return h;
    }


    /**
     * Sets the default bus.
     *
     * @param bus the default bus.
     */
    public static synchronized void setDefaultBus(Bus bus) {
        defaultBus = bus;
        BusHolder b = getThreadBusHolder(false);
        b.bus = bus;
        if (bus == null) {
            b.stale = true;
            THREAD_BUS.remove();
        }
    }

    /**
     * Sets the default bus for the thread.
     *
     * @param bus the default bus.
     */
    public static void setThreadDefaultBus(Bus bus) {
        if (bus == null) {
            BusHolder h = THREAD_BUS.get();
            if (h == null) {
                Thread cur = Thread.currentThread();
                synchronized (THREAD_BUSSES) {
                    h = THREAD_BUSSES.get(cur);
                }
            }
            if (h != null) {
                h.bus = null;
                h.stale = true;
                THREAD_BUS.remove();
            }
        } else {
            BusHolder b = getThreadBusHolder(true);
            b.bus = bus;
        }
    }

    /**
     * Sets the default bus for the thread.
     *
     * @param bus the new thread default bus.
     * @return the old thread default bus or null
     */
    public static Bus getAndSetThreadDefaultBus(Bus bus) {
        if (bus == null) {
            BusHolder b = THREAD_BUS.get();
            if (b == null) {
                Thread cur = Thread.currentThread();
                synchronized (THREAD_BUSSES) {
                    b = THREAD_BUSSES.get(cur);
                }
            }
            if (b != null) {
                Bus orig = b.bus;
                b.bus = null;
                b.stale = true;
                THREAD_BUS.remove();
                return orig;
            }
            return null;
        }
        BusHolder b = getThreadBusHolder(true);
        Bus old = b.bus;
        b.bus = bus;
        return old;
    }

    /**
     * Gets the default bus for the thread.
     *
     * @return the default bus.
     */
    public static Bus getThreadDefaultBus() {
        return getThreadDefaultBus(true);
    }

    /**
     * Gets the default bus for the thread, creating if needed
     *
     * @param createIfNeeded Set to true to create a default bus if one doesn't exist
     * @return the default bus.
     */
    public static Bus getThreadDefaultBus(boolean createIfNeeded) {
        if (createIfNeeded) {
            BusHolder b = getThreadBusHolder(false);
            if (b.bus == null) {
                b.bus = createThreadBus();
            }
            return b.bus;
        }
        BusHolder h = THREAD_BUS.get();
        if (h == null || h.stale) {
            Thread cur = Thread.currentThread();
            synchronized (THREAD_BUSSES) {
                h = THREAD_BUSSES.get(cur);
            }
        }
        return h == null || h.stale ? null : h.bus;
    }
    private static synchronized Bus createThreadBus() {
        BusHolder b = getThreadBusHolder(false);
        if (b.bus == null) {
            b.bus = getDefaultBus(true);
        }
        return b.bus;
    }
    /**
     * Removes a bus from being a thread default bus for any thread.
     * <p>
     * This is typically done when a bus has ended its lifecycle (i.e.: a call to
     * {@link Bus#shutdown(boolean)} was invoked) and it wants to remove any reference to itself for any
     * thread.
     *
     * @param bus the bus to remove
     */
    public static void clearDefaultBusForAnyThread(final Bus bus) {
        synchronized (THREAD_BUSSES) {
            for (final Iterator<BusHolder> iterator = THREAD_BUSSES.values().iterator();
                iterator.hasNext();) {
                BusHolder itBus = iterator.next();
                if (bus == null || itBus == null || itBus.bus == null
                    || itBus.stale
                    || bus.equals(itBus.bus)) {
                    if (itBus != null) {
                        itBus.bus = null;
                        //mark as stale so if a thread asks again, it will create a new one
                        itBus.stale = true;
                    }
                    //This will remove the BusHolder from the only place that should
                    //strongly reference it
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Sets the default bus if a default bus is not already set.
     *
     * @param bus the default bus.
     * @return true if the bus was not set and is now set
     */
    public static synchronized boolean possiblySetDefaultBus(Bus bus) {
        BusHolder b = getThreadBusHolder(false);
        if (b.bus == null) {
            b.bus = bus;
        }
        if (defaultBus == null) {
            defaultBus = bus;
            return true;
        }
        return false;
    }

    /**
     * Create a new BusFactory The class of the BusFactory is determined by looking for the system propery:
     * org.apache.cxf.bus.factory or by searching the classpath for:
     * META-INF/services/org.apache.cxf.bus.factory
     *
     * @return a new BusFactory to be used to create Bus objects
     */
    public static BusFactory newInstance() {
        return newInstance(null);
    }

    /**
     * Create a new BusFactory
     *
     * @param className The class of the BusFactory to create. If null, uses the default search algorithm.
     * @return a new BusFactory to be used to create Bus objects
     */
    public static BusFactory newInstance(String className) {
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

        try {
            Class<? extends BusFactory> busFactoryClass = ClassLoaderUtils.loadClass(className, BusFactory.class)
                .asSubclass(BusFactory.class);

            return busFactoryClass.getConstructor().newInstance();
        } catch (Exception ex) {
            LogUtils.log(LOG, Level.SEVERE, "BUS_FACTORY_INSTANTIATION_EXC", ex);
            throw new RuntimeException(ex);
        }
    }

    protected void initializeBus(Bus bus) {
    }

    private static String getBusFactoryClass(ClassLoader classLoader) {

        // next check system properties
        String busFactoryClass = SystemPropertyAction.getPropertyOrNull(BusFactory.BUS_FACTORY_PROPERTY_NAME);
        if (isValidBusFactoryClass(busFactoryClass)) {
            return busFactoryClass;
        }

        try {
            // next, check for the services stuff in the jar file
            String serviceId = "META-INF/services/" + BusFactory.BUS_FACTORY_PROPERTY_NAME;
            InputStream is;

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

            String busFactoryCondition = null;

            if (is != null) {
                try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    busFactoryClass = rd.readLine();
                    busFactoryCondition = rd.readLine();
                }
            }
            if (isValidBusFactoryClass(busFactoryClass)
                && busFactoryCondition != null) {
                try {
                    Class<?> cls = ClassLoaderUtils.loadClass(busFactoryClass, BusFactory.class)
                        .asSubclass(BusFactory.class);
                    if (busFactoryCondition.startsWith("#")) {
                        busFactoryCondition = busFactoryCondition.substring(1);
                    }
                    int idx = busFactoryCondition.indexOf(',');
                    while (idx != -1) {
                        cls.getClassLoader().loadClass(busFactoryCondition.substring(0, idx));
                        busFactoryCondition = busFactoryCondition.substring(idx + 1);
                        idx = busFactoryCondition.indexOf(',');
                    }
                    cls.getClassLoader().loadClass(busFactoryCondition);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    busFactoryClass = DEFAULT_BUS_FACTORY;
                }

            }

        } catch (Exception ex) {
            LogUtils.log(LOG, Level.SEVERE, "FAILED_TO_DETERMINE_BUS_FACTORY_EXC", ex);
        }
        return busFactoryClass;
    }

    private static boolean isValidBusFactoryClass(String busFactoryClassName) {
        return busFactoryClassName != null && !busFactoryClassName.isEmpty();
    }

}
