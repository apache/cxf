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

package org.apache.cxf.bus.extension;

import java.util.ArrayList;
import java.util.Collection;


public class Extension {

    private String className;
    private Class<?> clazz;
    private String interfaceName;
    private boolean deferred;
    private Collection<String> namespaces = new ArrayList<String>();
    
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("class: ");
        buf.append(className);
        buf.append(", interface: ");
        buf.append(interfaceName);
        buf.append(", deferred: ");
        buf.append(deferred ? "true" : "false");
        buf.append(", namespaces: (");
        int n = 0;
        for (String ns : namespaces) {
            if (n > 0) {
                buf.append(", ");
            }
            buf.append(ns);
            n++;
        }
        buf.append(")");
        return buf.toString();        
    }
    
    Class<?> getClassObject(ClassLoader cl) {
        if (clazz == null) {
            try {
                clazz = cl.loadClass(className);
            } catch (ClassNotFoundException ex) {
                throw new ExtensionException(ex);
            }
        }
        return clazz;
    }
    String getClassname() {
        return className;
    }
    
    void setClassname(String i) {
        clazz = null;
        className = i;
    }
       
    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String i) {
        interfaceName = i;
    }

    boolean isDeferred() {
        return deferred;
    }
    
    void setDeferred(boolean d) {
        deferred = d;
    }
    
    Collection<String> getNamespaces() {
        return namespaces;
    }
    
    Object load(ClassLoader cl) {
        Object obj = null;
        try {
            Class<?> cls = getClassObject(cl);
            obj = cls.newInstance();
        } catch (IllegalAccessException ex) {
            throw new ExtensionException(ex);
        } catch (InstantiationException ex) {
            throw new ExtensionException(ex);
        }
        
        return obj;
    }
    
    Class loadInterface(ClassLoader cl) {
        Class<?> cls = null;
        try {
            cls = cl.loadClass(interfaceName);
        } catch (ClassNotFoundException ex) {
            throw new ExtensionException(ex);
        }
        return cls;
    }
    
    
}
