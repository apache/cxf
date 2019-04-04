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
package org.apache.cxf.cdi;

import java.util.regex.Pattern;

import org.apache.cxf.common.util.ClassUnwrapper;

/**
 * Unwraps the CDI proxy classes into real classes.
 */
class CdiClassUnwrapper implements ClassUnwrapper {
    /**
     * Known proxy patterns for OWB and Weld:
     * 
     *  Xxx$$OwbNormalScopeProxy0
     *  Xxx$Proxy$_$$_WeldClientProxy
     *  
     */
    private static final Pattern PROXY_PATTERN = Pattern.compile(".+\\$\\$.+Proxy\\d*");

    CdiClassUnwrapper() {

    }

    @Override
    public Class<?> getRealClass(Object o) {
        Class<?> clazz = o.getClass();
        return getRealClassFromClass(clazz);
    }
    
    @Override
    public Class<?> getRealClassFromClass(Class<?> clazz) {
        if (PROXY_PATTERN.matcher(clazz.getSimpleName()).matches()) {
            return clazz.getSuperclass();
        }
        return clazz;
    }
}
