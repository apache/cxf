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

package org.apache.cxf.xmlbeans;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.xmlschema.SchemaCollection;
import org.apache.cxf.databinding.AbstractDataBinding;
import org.apache.cxf.databinding.AbstractWrapperHelper;
import org.apache.cxf.databinding.DataReader;
import org.apache.cxf.databinding.DataWriter;
import org.apache.cxf.databinding.WrapperCapableDatabinding;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.ServiceInfo;




/**
 * 
 */
public class XmlBeansDataBinding extends AbstractDataBinding implements WrapperCapableDatabinding {
    private static final Logger LOG = LogUtils.getLogger(XmlBeansDataBinding.class);

    private static final Class<?> SUPPORTED_READER_FORMATS[] = new Class<?>[] {XMLStreamReader.class};
    private static final Class<?> SUPPORTED_WRITER_FORMATS[]
        = new Class<?>[] {XMLStreamWriter.class, Node.class};
    
    
    @SuppressWarnings("unchecked")
    public <T> DataWriter<T> createWriter(Class<T> c) {
        if (c == XMLStreamWriter.class) {
            return (DataWriter<T>)new DataWriterImpl();
        } else if (c == Node.class) {
            return (DataWriter<T>)new NodeDataWriterImpl();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public <T> DataReader<T> createReader(Class<T> c) {
        DataReader<T> dr = null;
        if (c == XMLStreamReader.class) {
            dr = (DataReader<T>)new DataReaderImpl();
        }
        return dr;
    }
    

    /**
     * XmlBeans has no declared namespace prefixes.
     * {@inheritDoc}
     */
    public Map<String, String> getDeclaredNamespaceMappings() {
        return null;
    }

    public Class<?>[] getSupportedReaderFormats() {
        return SUPPORTED_READER_FORMATS;
    }

    public Class<?>[] getSupportedWriterFormats() {
        return SUPPORTED_WRITER_FORMATS;
    }

    public void initialize(Service service) {
        if (LOG.isLoggable(Level.FINER)) {
            LOG.log(Level.FINER, "Creating XmlBeansDatabinding for " + service.getName());
        }
        for (ServiceInfo serviceInfo : service.getServiceInfos()) {
            SchemaCollection col = serviceInfo.getXmlSchemaCollection();

            if (col.getXmlSchemas().length > 1) {
                // someone has already filled in the types
                continue;
            } 
            
            XmlBeansSchemaInitializer schemaInit 
                = new XmlBeansSchemaInitializer(serviceInfo, col, this);
            schemaInit.walk();
        }
    }

    public WrapperHelper createWrapperHelper(Class<?> wrapperType, QName wrapperName, List<String> partNames,
                                             List<String> elTypeNames, List<Class<?>> partClasses) {
        
        List<Method> getMethods = new ArrayList<Method>(partNames.size());
        List<Method> setMethods = new ArrayList<Method>(partNames.size());        
        List<Field> fields = new ArrayList<Field>(partNames.size());
        
        Method allMethods[] = wrapperType.getMethods();
                        
        for (int x = 0; x < partNames.size(); x++) {
            String partName = partNames.get(x);            
            if (partName == null) {
                getMethods.add(null);
                setMethods.add(null);
                fields.add(null);
                continue;
            }
                                   
            String getAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.GETTER);
            String setAccessor = JAXBUtils.nameToIdentifier(partName, JAXBUtils.IdentifierType.SETTER);
            Method getMethod = null;
            Method setMethod = null;
            Class<?> valueClass = XmlBeansWrapperHelper.getXMLBeansValueType(wrapperType);
            allMethods = valueClass.getMethods();
            
            try {
                getMethod = valueClass.getMethod(getAccessor, AbstractWrapperHelper.NO_CLASSES);
            } catch (NoSuchMethodException ex) {
                //ignore for now
            }
                        
            for (Method method : allMethods) {
                if (method.getParameterTypes() != null && method.getParameterTypes().length == 1
                    && (setAccessor.equals(method.getName()))) {                        
                    setMethod = method;
                    break;
                }
            }
            
            getMethods.add(getMethod);
            setMethods.add(setMethod);
            // There is no filed in the XMLBeans type class
            fields.add(null);
            
        }
        
        return new XmlBeansWrapperHelper(wrapperType,
                                 setMethods.toArray(new Method[setMethods.size()]),
                                 getMethods.toArray(new Method[getMethods.size()]),
                                 fields.toArray(new Field[fields.size()]));
    }

}
