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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.cxf.Bus;
import org.apache.cxf.common.i18n.Message;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.StringUtils;

public class Extension {
    protected static final Logger LOG = LogUtils.getL7dLogger(Extension.class);
    
    private static final String PROBLEM_CREATING_EXTENSION_CLASS = "PROBLEM_CREATING_EXTENSION_CLASS";

    protected String className;
    protected ClassLoader classloader;
    protected volatile Class<?> clazz;
    protected volatile Class<?> intf;
    protected String interfaceName;
    protected boolean deferred;
    protected Collection<String> namespaces = new ArrayList<>();
    protected Object[] args;
    protected volatile Object obj;
    protected boolean optional;
    protected boolean notFound;
    

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
    public Extension(ClassLoader loader) {
        classloader = loader;
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
        optional = ext.optional;
    }

    public void setOptional(boolean b) {
        optional = b;
    }
    public boolean isOptional() {
        return optional;
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
        StringBuilder buf = new StringBuilder(128);
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
        buf.append(')');
        return buf.toString();
    }

    public String getClassname() {
        return className;
    }

    public void setClassname(String i) {
        clazz = null;
        notFound = false;
        className = i;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String i) {
        interfaceName = i;
        notFound = false;
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

    public void setArgs(Object[] a) {
        args = a;
    }

    protected Class<?> tryClass(String name, ClassLoader cl) {
        Throwable origEx = null;
        if (classloader != null) {
            try {
                return classloader.loadClass(name);
            } catch (Throwable nex) {
                //ignore, fall into the stuff below
                //save the exception though as this is likely the important one
                origEx = nex;
            }
        }
        try {
            return cl.loadClass(name);
        } catch (Throwable ex) {
            try {
                // using the extension classloader as a fallback
                return this.getClass().getClassLoader().loadClass(name);
            } catch (Throwable nex) {
                notFound = true;
                if (!optional) {
                    throw new ExtensionException(new Message("PROBLEM_LOADING_EXTENSION_CLASS", LOG, name),
                        origEx != null ? origEx : ex);
                }
            }
        }
        return null;
    }

    public Class<?> getClassObject(ClassLoader cl) {
        if (notFound) {
            return null;
        }
        if (clazz != null) {
            return clazz;
        }
        synchronized (this) {
            if (clazz == null) {
                clazz = tryClass(className, cl);
            }
        }
        return clazz;
    }
    public Object load(ClassLoader cl, Bus b) {
        if (obj != null) {
            return obj;
        }
        Class<?> cls = getClassObject(cl);
        try {
            if (notFound) {
                return null;
            }
            try {
                //if there is a Bus constructor, use it.
                if (b != null && args == null) {
                    Constructor<?> con = cls.getConstructor(Bus.class);
                    obj = con.newInstance(b);
                    return obj;
                } else if (b != null && args != null) {
                    try {
                        obj = cls.getConstructor(Bus.class, Object[].class).newInstance(b, args);
                    } catch (NoSuchMethodException ex) { // no bus
                        obj = cls.getConstructor(Object[].class).newInstance(args);
                    }
                    return obj;
                } else if (args != null) {
                    Constructor<?> con = cls.getConstructor(Object[].class);
                    obj = con.newInstance(args);
                    return obj;
                }
            } catch (InvocationTargetException ex) {
                throw new ExtensionException(new Message(PROBLEM_CREATING_EXTENSION_CLASS, LOG, cls.getName()),
                                             ex.getCause());
            } catch (InstantiationException | SecurityException ex) {
                throw new ExtensionException(new Message(PROBLEM_CREATING_EXTENSION_CLASS, LOG, cls.getName()), ex);
            } catch (NoSuchMethodException e) {
                //ignore
            }
            obj = cls.getConstructor().newInstance();
        } catch (ExtensionException ex) {
            notFound = true;
            if (!optional) {
                throw ex;
            }
            LOG.log(Level.FINE, "Could not load optional extension " + getName(), ex);
        } catch (InvocationTargetException ex) {
            notFound = true;
            if (!optional) {
                throw new ExtensionException(new Message(PROBLEM_CREATING_EXTENSION_CLASS, LOG, cls.getName()),
                                             ex.getCause());
            }
            LOG.log(Level.FINE, "Could not load optional extension " + getName(), ex);
        } catch (NoSuchMethodException ex) {
            notFound = true;
            List<Object> a = new ArrayList<>();
            if (b != null) {
                a.add(b);
            }
            if (args != null) {
                a.add(args);
            }
            if (!optional) {
                throw new ExtensionException(new Message("PROBLEM_FINDING_CONSTRUCTOR", LOG,
                                                         cls.getName(), a), ex);
            }
            LOG.log(Level.FINE, "Could not load optional extension " + getName(), ex);
        } catch (Throwable e) {
            notFound = true;
            if (!optional) {
                throw new ExtensionException(new Message(PROBLEM_CREATING_EXTENSION_CLASS, LOG, cls.getName()), e);
            }
            LOG.log(Level.FINE, "Could not load optional extension " + getName(), e);
        }
        return obj;
    }

    public Class<?> loadInterface(ClassLoader cl) {
        if (intf != null || notFound) {
            return intf;
        }
        synchronized (this) {
            if (intf == null) {
                intf = tryClass(interfaceName, cl);
            }
        }
        return intf;
    }


}
