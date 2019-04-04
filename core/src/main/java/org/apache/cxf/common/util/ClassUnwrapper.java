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

public interface ClassUnwrapper {
    /**
     * Return a real class for the instance, possibly a proxy.
     * @param o instance to get real class for
     * @return real class for the instance
     */
    Class<?> getRealClass(Object o);

    /**
     * Return a real class for the class, possibly a proxy class.
     * @param clazz class to get real class for
     * @return real class for the class
     */
    Class<?> getRealClassFromClass(Class<?> clazz);
    
    /**
     * Return a real class for the instance, possibly a proxy.
     * @param o instance to get real class for
     * @return real class for the instance
     */
    default Object getRealObject(Object o) {
        return (o instanceof Proxy) ? Proxy.getInvocationHandler(o) : o;
    }
}
