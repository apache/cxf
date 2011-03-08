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

package org.apache.cxf.bus.spring;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.logging.LogUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;

/**
 * 
 */
public final class OldSpringSupport implements FactoryBean, BeanNameAware {
    public static final Logger LOG = LogUtils.getL7dLogger(OldSpringSupport.class);
    Bus bus;
    Class<?> cls;
    String id;
    
    public OldSpringSupport(String imp) {
        logWarning(imp);
    }
    
    public OldSpringSupport(Bus b, Class<?> c) {
        cls = c;
        bus = b;
    }
    public OldSpringSupport(Bus b, Class<?> c, String imp) {
        cls = c;
        bus = b;
        logWarning(imp);
    }
    
    public static String logWarning(String imp) {
        LOG.log(Level.WARNING, "DEPRECATED_IMPORT", imp);
        return imp;
    }

    public Object getObject() throws Exception {
        LOG.log(Level.WARNING, "DEPRECATED_OBJECT_RETRIEVAL", id);
        return bus.getExtension(cls);
    }

    public Class getObjectType() {
        return cls;
    }

    public boolean isSingleton() {
        return true;
    }

    public void setBeanName(String name) {
        id = name;
    }

}
