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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.cxf.Bus;
import org.apache.cxf.common.util.StringUtils;

public class Extension {
   
    protected String className;
    protected ClassLoader classloader;
    protected Class<?> clazz;
    protected Class<?> intf;
    protected String interfaceName;
    protected boolean deferred;
    protected Collection<String> namespaces = new ArrayList<String>();
    protected Object args[];
    protected Object obj;
    
    public Extension() {
    }
    
    public Extension(Class<?> cls, Class<?> inf) {
        clazz = cls;
        intf = inf;
        interfaceName = inf.getName();
        className = cls.getName();
        classloader = cls.getClassLoader();
    }
    public Extension(Class<?> cls) {
        clazz = cls;
        className = cls.getName();
        classloader = cls.getClassLoader();
    }
    
    public Extension(Extension ext) {
        className = ext.className;
        interfaceName = ext.interfaceName;
        deferred = ext.deferred;
        namespaces = ext.namespaces;
        obj = ext.obj;
        clazz = ext.clazz;
        intf = ext.intf;
        classloader = ext.classloader;
        args = ext.args;
    }
    
    public String getName() {
        return StringUtils.isEmpty(interfaceName) ? className : interfaceName;
    }
    public Object getLoadedObject() {
        return obj;
    }
    
    public Extension cloneNoObject() {
        Extension ext = new Extension(this);
        ext.obj = null;
        ext.clazz = null;
        ext.intf = null;
        return ext;
    }
    
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
    
    public String getClassname() {
        return className;
    }
    
    public void setClassname(String i) {
        clazz = null;
        className = i;
    }
       
    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String i) {
        interfaceName = i;
    }

    public boolean isDeferred() {
        return deferred;
    }
    
    public void setDeferred(boolean d) {
        deferred = d;
    }
    
    public Collection<String> getNamespaces() {
        return namespaces;
    }
    
    public void setArgs(Object a[]) {
        args = a;
    }
    
    public Class<?> getClassObject(ClassLoader cl) {
        if (clazz == null) {
            if (classloader != null) {
                try {
                    clazz = classloader.loadClass(className);
                    return clazz;
                } catch (ClassNotFoundException nex) {
                    //ignore, fall into the stuff below
                }
            }                
            try {
                clazz = cl.loadClass(className);
            } catch (ClassNotFoundException ex) {
                try {
                    // using the extension classloader as a fallback
                    clazz = this.getClass().getClassLoader().loadClass(className);
                } catch (ClassNotFoundException nex) {
                    throw new ExtensionException(nex);
                }
            }
        }
        return clazz;
    }
    public Object load(ClassLoader cl, Bus b) {
        try {
            Class<?> cls = getClassObject(cl);
            try {
                //if there is a Bus constructor, use it.
                if (b != null && args == null) {
                    Constructor con = cls.getConstructor(Bus.class);
                    obj = con.newInstance(b);
                    return obj;
                } else if (b != null && args != null) {
                    Constructor con;
                    boolean noBus = false;
                    try {
                        con = cls.getConstructor(Bus.class, Object[].class);
                    } catch (Exception ex) {
                        con = cls.getConstructor(Object[].class);
                        noBus = true;
                    }
                    if (noBus) {
                        obj = con.newInstance(args);
                    } else {
                        obj = con.newInstance(b, args);
                    }
                    return obj;                    
                } else if (args != null) {
                    Constructor con = cls.getConstructor(Object[].class);
                    obj = con.newInstance(args);
                    return obj;                    
                }
            } catch (Exception ex) {
                //ignore
            }
            obj = cls.newInstance();
        } catch (IllegalAccessException ex) {
            throw new ExtensionException(ex);
        } catch (InstantiationException ex) {
            throw new ExtensionException(ex);
        }
        return obj;
    }
    
    public Class loadInterface(ClassLoader cl) {
        if (intf != null) {
            return intf;
        }
        if (classloader != null) {
            try {
                intf = classloader.loadClass(interfaceName);
                if (intf != null) {
                    return intf;
                }
            } catch (ClassNotFoundException nex) {
                //ignore, fall into the stuff below
            }
        }                

        try {
            intf = cl.loadClass(interfaceName);
        } catch (ClassNotFoundException ex) {
            try {
                // using the extension classloader as a fallback
                intf = this.getClass().getClassLoader().loadClass(interfaceName);
            } catch (ClassNotFoundException nex) {
                throw new ExtensionException(nex);
            }
        }
        return intf;
    }
    
    
}
