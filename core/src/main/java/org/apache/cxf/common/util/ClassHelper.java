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

package org.apache.cxf.common.util;


import java.lang.reflect.Proxy;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;

/**
 *
 */
public class ClassHelper {

    public static final String USE_DEFAULT_CLASS_HELPER = "org.apache.cxf.useDefaultClassHelpers";

    static final ClassHelper HELPER;
    static final ClassUnwrapper DEFAULT_UNWRAPPER;
    static final ClassUnwrapper UNWRAPPER;
    
    /**
     * Default class unwrapper implementation which delegates to the ClassHelper
     * internal methods.
     *
     */
    private static class DefaultClassUnwrapper implements ClassUnwrapper {
        private final ClassHelper helper;
        
        DefaultClassUnwrapper(ClassHelper helper) {
            this.helper = helper;
        }
        
        @Override
        public Class<?> getRealClassFromClass(Class<?> clazz) {
            return helper.getRealClassFromClassInternal(clazz);
        }
        
        @Override
        public Class<?> getRealClass(Object o) {
            return helper.getRealClassInternal(o);
        }
        
        @Override
        public Object getRealObject(Object o) {
            return helper.getRealObjectInternal(o);
        }
    }

    static {
        HELPER = new ClassHelper();
        DEFAULT_UNWRAPPER = new DefaultClassUnwrapper(HELPER);
        UNWRAPPER = getClassUnwrapper(DEFAULT_UNWRAPPER);
    }

    protected ClassHelper() {
    }

    private static ClassUnwrapper getClassUnwrapper(ClassUnwrapper defaultHelper) {
        boolean useSpring = true;
        String s = SystemPropertyAction.getPropertyOrNull("org.apache.cxf.useSpringClassHelpers");
        if (!StringUtils.isEmpty(s)) {
            useSpring = "1".equals(s) || Boolean.parseBoolean(s);
        }
        if (useSpring) {
            try {
                return new SpringClassUnwrapper();
            } catch (Throwable ex) {
                // ignore
            }
        }
        return defaultHelper;
    }

    private Class<?> getRealClassInternal(Object o) {
        return getRealObjectInternal(o).getClass();
    }

    private Class<?> getRealClassFromClassInternal(Class<?> cls) {
        return cls;
    }

    private Object getRealObjectInternal(Object o) {
        return o instanceof Proxy ? Proxy.getInvocationHandler(o) : o;
    }

    public static Class<?> getRealClass(Object o) {
        return getRealClass(null, o);
    }

    public static Class<?> getRealClassFromClass(Class<?> cls) {
        return getRealClassFromClass(null, cls);
    }

    public static Class<?> getRealClassFromClass(Bus bus, Class<?> cls) {
        return getContextClassUnwrapper(getBus(bus)).getRealClassFromClass(cls);
    }

    public static Object getRealObject(Object o) {
        return getContextClassUnwrapper(getBus(null)).getRealObject(o);
    }

    public static Class<?> getRealClass(Bus bus, Object o) {
        return getContextClassUnwrapper(getBus(bus)).getRealClass(o);
    }

    private static ClassUnwrapper getContextClassUnwrapper(Bus bus) {
        if (bus != null && bus.getProperty(ClassUnwrapper.class.getName()) != null) {
            return  (ClassUnwrapper) bus.getProperty(ClassUnwrapper.class.getName());
        }

        return (DEFAULT_UNWRAPPER == UNWRAPPER || checkUseDefaultClassHelper(bus)) ? DEFAULT_UNWRAPPER : UNWRAPPER;
    }

    private static Bus getBus(Bus bus) {
        return bus == null ? BusFactory.getThreadDefaultBus() : bus;
    }

    private static boolean checkUseDefaultClassHelper(Bus bus) {
        return bus != null && Boolean.TRUE.equals(bus.getProperty(USE_DEFAULT_CLASS_HELPER));
    }

}
