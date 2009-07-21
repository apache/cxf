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

package org.apache.cxf.sdo;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.WrapperCapableDatabinding;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.resource.ExtendedURIResolver;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.tuscany.sdo.api.SDOUtil;

import commonj.sdo.DataObject;
import commonj.sdo.Type;
import commonj.sdo.helper.HelperContext;
import commonj.sdo.impl.HelperProvider;


/**
 * 
 */
public class SDODataBinding extends AbstractDataBinding 
    implements WrapperCapableDatabinding { 

    private final class SDOWrapperHelper implements WrapperHelper {
        private final List<String> partNames;
        private Method fact;
        private Object factory;
        private QName wrapperName;

        private SDOWrapperHelper(List<String> partNames, Class<?> wrapperType, QName wrapperName) {
            this.partNames = partNames;
            if (DataObject.class != wrapperType) {
                try {
                    String s = wrapperType.getPackage().getName() + ".SdoFactory";
                    Class<?> cls = Class.forName(s, false, wrapperType.getClassLoader());
                    for (Method m : cls.getMethods()) {
                        if (m.getReturnType() == wrapperType) {
                            fact = m;
                            break;
                        }
                    }
                    factory = cls.getField("INSTANCE").get(null);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            this.wrapperName = wrapperName;
        }

        public Object createWrapperObject(List<?> lst) throws Fault {
            DataObject o;
            if (fact != null) {
                try {
                    o = (DataObject)fact.invoke(factory);
                } catch (Exception e) {
                    throw new Fault(e); 
                }
            } else {
                o = context.getDataFactory().create(wrapperName.getNamespaceURI(), 
                                                    wrapperName.getLocalPart());
            }
            for (int x = 0; x < lst.size(); x++) {
                o.set(partNames.get(x), lst.get(x));
            }
            return o;
        }

        public String getSignature() {
            return "" + System.identityHashCode(this);
        }

        public List<Object> getWrapperParts(Object o) throws Fault {
            List<Object> lst = new ArrayList<Object>();
            DataObject obj = (DataObject)o;
            for (String s : partNames) {
                lst.add(obj.get(s));
            }
            return lst;
        }
    }



    private static final Class<?> SUPPORTED_READER_FORMATS[] = new Class<?>[] {XMLStreamReader.class};
    private static final Class<?> SUPPORTED_WRITER_FORMATS[]
        = new Class<?>[] {XMLStreamWriter.class, Node.class};
    HelperProvider provider;
    HelperContext context;
    
    public void initialize(Service service) {
        context = SDOUtil.createHelperContext();
        
        Set<String> pkgs = new HashSet<String>();
        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            SDOClassCollector cc = new SDOClassCollector(serviceInfo);
            cc.walk();
            
            for (Class<?> cls : cc.getClasses()) {
                if (DataObject.class == cls) {
                    continue;
                }
                String pkg = cls.getPackage().getName();
                if (!pkgs.contains(pkg)) {
                    try {
                        Class fact = Class.forName(pkg + ".SdoFactory", false, cls.getClassLoader());
                        registerFactory(fact);
                        pkgs.add(pkg);
                    } catch (Throwable t) {
                        //ignore, register the class itself
                        register(cls);
                    }
                }
            }
        }
        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            String uri = serviceInfo.getDescription().getBaseURI();
            ExtendedURIResolver resolver = new ExtendedURIResolver();
            InputStream ins = resolver.resolve(uri, "").getByteStream();
            context.getXSDHelper().define(ins, uri);
            resolver.close();
        }        
    }
    void registerFactory(Class<?> factoryClass) throws Exception {
        Field field = factoryClass.getField("INSTANCE");
        Object factory = field.get(null);
        Method method = factory.getClass().getMethod("register", new Class[] {HelperContext.class});
        method.invoke(factory, new Object[] {context});
    }
    boolean register(Class javaType) {
        if (javaType == null || DataObject.class == javaType) {
            return false;
        }
        try {
            Type type = context.getTypeHelper().getType(javaType);
            if (type != null && (!type.isDataType())) {
                Method method = type.getClass().getMethod("getEPackage");
                Object factory = method.invoke(type, new Object[] {});
                method = factory.getClass().getMethod("register", HelperContext.class);
                method.invoke(factory, new Object[] {context});
                return true;
            }
            return false;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> c) {
        DataReader<T> dr = null;
        if (c == XMLStreamReader.class) {
            dr = (DataReader<T>)new DataReaderImpl(context);
        }
        return dr;
    }
    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> c) {
        if (c == XMLStreamWriter.class) {
            return (DataWriter<T>)new DataWriterImpl(context);
        } else if (c == Node.class) {
            return (DataWriter<T>)new NodeDataWriterImpl(context);
        }
        return null;
    }
    public Class<?>[] getSupportedReaderFormats() {
        return SUPPORTED_READER_FORMATS;
    }

    public Class<?>[] getSupportedWriterFormats() {
        return SUPPORTED_WRITER_FORMATS;
    }

    
    
    public WrapperHelper createWrapperHelper(final Class<?> wrapperType,
                                             final QName wrapperName, 
                                             final List<String> partNames,
                                             final List<String> elTypeNames,
                                             final List<Class<?>> partClasses) {
        
        
        return new SDOWrapperHelper(partNames, wrapperType, wrapperName);
    }
}
