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

package org.apache.cxf.ws.policy.builder.jaxb;

import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import org.apache.cxf.common.classloader.ClassLoaderUtils;
import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.common.util.PackageUtils;
import org.apache.cxf.jaxb.JAXBUtils;
import org.apache.cxf.ws.policy.AssertionBuilder;
import org.apache.cxf.ws.policy.PolicyAssertion;
import org.apache.neethi.Constants;

public class JaxbAssertionBuilder<T> implements AssertionBuilder {

    private static final Logger LOG = LogUtils.getL7dLogger(JaxbAssertionBuilder.class);
    private Unmarshaller unmarshaller;
    private Collection<QName> supportedTypes;
    private Class<T> type;
    
    /**
     * Constructs a JAXBAssertionBuilder from the QName of the schema type
     * @param qn the schema type
     * @throws JAXBException
     * @throws ClassNotFoundException
     */
    public JaxbAssertionBuilder(QName qn) throws JAXBException, ClassNotFoundException {
        this(JAXBUtils.namespaceURIToPackage(qn.getNamespaceURI())
            + "." + JAXBUtils.nameToIdentifier(qn.getLocalPart(), JAXBUtils.IdentifierType.CLASS), qn);
    }
    
    /**
     * Constructs a JAXBAssertionBuilder from the specified class name and schema type. 
     * @param className the name of the class to which the schema type is mapped
     * @param qn the schema type
     * @throws JAXBException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    public JaxbAssertionBuilder(String className, QName qn) throws JAXBException, ClassNotFoundException {
        this((Class<T>)ClassLoaderUtils.loadClass(className, JaxbAssertionBuilder.class), qn);
    }
    
    /**
    * Constructs a JAXBAssertionBuilder from the specified class and schema type. 
    * @param type the class to which the schema type is mapped
    * @param qn the schema type
    * @throws JAXBException
    * @throws ClassNotFoundException
    */
    public JaxbAssertionBuilder(Class<T> type, QName qn) throws JAXBException {
        this.type = type;
        supportedTypes = Collections.singletonList(qn);
    }
       
    protected Unmarshaller getUnmarshaller() {
        if (unmarshaller == null) {
            try {
                createUnmarhsaller();
            } catch (JAXBException e) {
                throw new RuntimeException(e);
            }
        }
        
        return unmarshaller;
    }
    
    protected synchronized void createUnmarhsaller()  throws JAXBException {
        if (unmarshaller != null) {
            return;
        }
        
        JAXBContext context = JAXBContext.newInstance(PackageUtils.getPackageName(type), 
                                                      type.getClassLoader());
        unmarshaller = context.createUnmarshaller();
    }
    
    public PolicyAssertion build(Element element) {
        QName name = new QName(element.getNamespaceURI(), element.getLocalName());
        JaxbAssertion<T> assertion = buildAssertion();
        assertion.setName(name);
        assertion.setOptional(getOptionality(element));
        assertion.setData(getData(element));
        return assertion;
    }
    
    public Collection<QName> getKnownElements() {
        return supportedTypes;
    }

    @SuppressWarnings("unchecked")
    public PolicyAssertion buildCompatible(PolicyAssertion a, PolicyAssertion b) {
        if (a.equal(b)) {
            JaxbAssertion<T> ja = (JaxbAssertion<T>)a;
            JaxbAssertion<T> compatible = buildAssertion();
            compatible.setName(a.getName());
            compatible.setOptional(a.isOptional() && b.isOptional());
            compatible.setData(ja.getData());
            return compatible;
        }
        return null;
    }
    
    protected JaxbAssertion<T> buildAssertion() {
        return new JaxbAssertion<T>();
    }
    
    protected boolean getOptionality(Element element) {
        boolean optional = false;
        String value = element.getAttributeNS(
                           Constants.Q_ELEM_OPTIONAL_ATTR.getNamespaceURI(), 
                           Constants.Q_ELEM_OPTIONAL_ATTR.getLocalPart());
        if (Boolean.valueOf(value)) {
            optional = true;
        }
        return optional;
    }
    
    @SuppressWarnings("unchecked")
    protected T getData(Element element) {
        Object obj = null;
        try {
            obj = getUnmarshaller().unmarshal(element);
        } catch (JAXBException ex) {
            LogUtils.log(LOG, Level.SEVERE, "UNMARSHAL_ELEMENT_EXC", ex);
        }
        if (obj instanceof JAXBElement<?>) {
            JAXBElement<?> el = (JAXBElement<?>)obj;
            obj = el.getValue();
        } 
        if (null != obj && LOG.isLoggable(Level.FINE)) {
            LOG.fine("Unmarshaled element into object of type: " + obj.getClass().getName()
                 + "    value: " + obj);
        }
        return (T)obj;
    }
    
    

}
