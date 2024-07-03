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

package org.apache.cxf.systest.jaxrs.security;

import java.lang.reflect.Field;

import org.eclipse.jetty.security.jaas.JAASLoginService;

/**
 * Since BookLoginModule delegates to PropertyFileLoginModule, the access to JAASLoginService.INSTANCE 
 * is required 9.4.35.v20201120+ (see please https://github.com/eclipse/jetty.project/issues/5486).
 */
public class BookLoginService extends JAASLoginService {
    private static JAASLoginService globalInstance;
    private static Field instanceField;
    
    public BookLoginService() {
        globalInstance = this;
        
        try {
            instanceField = JAASLoginService.class.getField("INSTANCE");
        } catch (final Exception ex) {
            /* do nothing, older Jetty version where field is not available */
        }
    }
    
    public static void withInstance(Runnable r) {
        final ThreadLocal<JAASLoginService> instance = getCurrentInstance();
        boolean managed = false;
        
        try {
            if (instance.get() == null) {
                instance.set(globalInstance);
                managed = true;
            }
            
            r.run();
        } finally {
            if (managed) {
                instance.remove();
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static ThreadLocal<JAASLoginService> getCurrentInstance() {
        if (instanceField == null) {
            return new ThreadLocal<JAASLoginService>();
        } else {
            try {
                return (ThreadLocal<JAASLoginService>)instanceField.get(null);
            } catch (final Exception ex) {
                return new ThreadLocal<JAASLoginService>();
            }
        }
    }
}
