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

package org.apache.cxf.tools.java2wsdl.generator.wsdl11.annotator;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.tools.common.model.Annotator;
import org.apache.cxf.tools.common.model.JAnnotation;
import org.apache.cxf.tools.common.model.JAnnotationElement;
import org.apache.cxf.tools.common.model.JavaAnnotatable;
import org.apache.cxf.tools.common.model.JavaField;
import org.apache.cxf.tools.java2wsdl.generator.wsdl11.model.WrapperBeanClass;
public class WrapperBeanAnnotator implements Annotator {
    Class<?> sourceClass;
    
    public WrapperBeanAnnotator() {
        
    }
    public WrapperBeanAnnotator(Class<?> cls) {
        this.sourceClass = cls;
    }
    
    public void annotate(final JavaAnnotatable clz) {
        WrapperBeanClass beanClass = null;
        if (clz instanceof WrapperBeanClass) {
            beanClass = (WrapperBeanClass) clz;
        } else {
            throw new RuntimeException("WrapperBeanAnnotator expect JavaClass as input");
        }

        JAnnotation xmlRootElement = new JAnnotation(XmlRootElement.class);
        xmlRootElement.addElement(new JAnnotationElement("name", 
                                                         beanClass.getElementName().getLocalPart()));
        xmlRootElement.addElement(new JAnnotationElement("namespace", 
                                                         beanClass.getElementName().getNamespaceURI()));
        
        JAnnotation xmlAccessorType = new JAnnotation(XmlAccessorType.class);
        xmlAccessorType.addElement(new JAnnotationElement(null, XmlAccessType.FIELD));

        XmlType tp = null;
        if (sourceClass != null) {
            tp = sourceClass.getAnnotation(XmlType.class);
        }
        JAnnotation xmlType = new JAnnotation(XmlType.class);
        if (tp == null) {
            xmlType.addElement(new JAnnotationElement("name", 
                                                  beanClass.getElementName().getLocalPart()));
            xmlType.addElement(new JAnnotationElement("namespace", 
                                                  beanClass.getElementName().getNamespaceURI()));
        } else {
            if (!"##default".equals(tp.name())) {
                xmlType.addElement(new JAnnotationElement("name", 
                                                          tp.name()));
            }
            if (!"##default".equals(tp.namespace())) {
                xmlType.addElement(new JAnnotationElement("namespace", 
                                                          tp.namespace()));
            }
            if (!StringUtils.isEmpty(tp.factoryMethod())) {
                xmlType.addElement(new JAnnotationElement("factoryMethod",
                                                          tp.factoryMethod()));
            }
            if (tp.propOrder().length != 1 
                || !StringUtils.isEmpty(tp.propOrder()[0])) {
                xmlType.addElement(new JAnnotationElement("propOrder", 
                                                      tp.propOrder()));
            }
            
        }
        List<String> props = new ArrayList<String>();
        for (JavaField f : beanClass.getFields()) {
            props.add(f.getParaName());
        }
        if (props.size() > 1) {
            xmlType.addElement(new JAnnotationElement("propOrder",
                                                      props));
        }
        
        // Revisit: why annotation is string?
        beanClass.addAnnotation(xmlRootElement);
        beanClass.addAnnotation(xmlAccessorType);
        beanClass.addAnnotation(xmlType);
    }
}
