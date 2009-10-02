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

/**
 * 
 */
public class ClassHelper {
    static final ClassHelper HELPER;
    static {
        ClassHelper theHelper = null;
        try {
            theHelper = new SpringAopClassHelper();
        } catch (Exception ex) {
            theHelper = new ClassHelper();
        }
        HELPER = theHelper;
    }
    
    
    protected ClassHelper() {
    }
    
    protected Class getRealClassInternal(Object o) {
        return o.getClass();
    }
    
    protected Class getRealClassFromClassInternal(Class cls) {
        return cls;
    }
    
    
    public static Class getRealClass(Object o) {
        return HELPER.getRealClassInternal(o);
    }
    
    public static Class getRealClassFromClass(Class cls) {
        return HELPER.getRealClassFromClassInternal(cls);
    }
}
