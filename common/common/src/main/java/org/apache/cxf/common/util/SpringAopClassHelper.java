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

/**
 * 
 */
class SpringAopClassHelper extends ClassHelper {
    SpringAopClassHelper() throws Exception {
        Class.forName("org.springframework.aop.support.AopUtils");
        Class.forName("org.springframework.aop.framework.Advised");
    }
    
    protected Class getRealClassFromClassInternal(Class cls) {
        if (AopUtils.isCglibProxyClass(cls)) {
            return getRealClassFromClassInternal(cls.getSuperclass());
        }
        return cls;
    }
    protected Object getRealObjectInternal(Object o) {
        if (o instanceof Advised) {
            try {

                Advised advised = (Advised)o;
                Object target = advised.getTargetSource().getTarget();
                //could be a proxy of a proxy.....   
                return getRealObjectInternal(target);
            } catch (Exception ex) {
                // ignore
            }
        }
        return o;
    }

    protected Class getRealClassInternal(Object o) {
        if (AopUtils.isAopProxy(o) && (o instanceof Advised)) {
            Advised advised = (Advised)o;
            try {
                TargetSource targetSource = advised.getTargetSource();
                
                Object target = null;
                
                try {
                    target = targetSource.getTarget();
                } catch (BeanCreationException ex) {
                    // some scopes such as 'request' may not 
                    // be active on the current thread yet
                    return getRealClassFromClassInternal(targetSource.getTargetClass());
                }
                
                if (target == null) {
                    Class targetClass = AopUtils.getTargetClass(o);
                    if (targetClass != null) {
                        return getRealClassFromClassInternal(targetClass);
                    }
                } else {
                    return getRealClassInternal(target); 
                }
            } catch (Exception ex) {
                // ignore
            }
            
        } else if (AopUtils.isCglibProxyClass(o.getClass())) {
            return getRealClassFromClassInternal(AopUtils.getTargetClass(o));
        }
        return o.getClass();
    }
    
}
