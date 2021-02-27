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

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.ClassUtils;

/**
 *
 */
class SpringClassUnwrapper implements ClassUnwrapper {
    SpringClassUnwrapper() throws ClassNotFoundException {
        Class.forName("org.springframework.aop.support.AopUtils");
        Class.forName("org.springframework.aop.framework.Advised");
    }

    @Override
    public Class<?> getRealClassFromClass(Class<?> cls) {
        if (ClassUtils.isCglibProxyClass(cls)) {
            final Class<?> superclass = cls.getSuperclass();
            // Lambda's generated class names also contain $$ which makes them trigger CGLIB
            // proxy path. Adding more checks to handle this particular case.
            if (superclass != null && (superclass != Object.class || wasCglibEnhanced(cls))) {
                return getRealClassFromClass(superclass);
            }
        }
        return cls;
    }

    @Override
    public Object getRealObject(Object o) {
        if (o instanceof Advised) {
            try {

                Advised advised = (Advised)o;
                Object target = advised.getTargetSource().getTarget();
                //could be a proxy of a proxy.....
                return getRealObject(target);
            } catch (Exception ex) {
                // ignore
            }
        }
        return o;
    }

    @Override
    public Class<?> getRealClass(Object o) {
        if (AopUtils.isAopProxy(o) && (o instanceof Advised)) {
            Advised advised = (Advised)o;
            try {
                TargetSource targetSource = advised.getTargetSource();

                final Object target;
                try {
                    target = targetSource.getTarget();
                } catch (BeanCreationException ex) {
                    // some scopes such as 'request' may not
                    // be active on the current thread yet
                    return getRealClassFromClass(targetSource.getTargetClass());
                }

                if (target == null) {
                    Class<?> targetClass = AopUtils.getTargetClass(o);
                    if (targetClass != null) {
                        return getRealClassFromClass(targetClass);
                    }
                } else {
                    return getRealClass(target);
                }
            } catch (Exception ex) {
                // ignore
            }

        } else if (ClassUtils.isCglibProxyClass(o.getClass())) {
            return getRealClassFromClass(AopUtils.getTargetClass(o));
        }
        
        return o.getClass();
    }

    /**
     * This additional check is not very reliable since CGLIB allows to
     * supply own NamingPolicy implementations. However, it works with native
     * CGLIB proxies ("byCGLIB$$") as well as Spring CGLIB proxies (by "BySpringCGLIB$$").
     * More expensive approach is to use reflection and inspect the class declared methods, 
     * looking for CGLIB-specific ones like CGLIB$BIND_CALLBACKS. 
     */
    private static boolean wasCglibEnhanced(Class<?> cls) {
        return cls.getName().contains("CGLIB");
    }
}
