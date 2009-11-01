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

package org.apache.cxf.javascript.fortest;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;
import javax.jws.WebService;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;

import org.apache.cxf.headers.Header;
import org.apache.cxf.jaxb.JAXBDataBinding;

/**
 * 
 */
@WebService(endpointInterface = "org.apache.cxf.javascript.fortest.SimpleDocLitWrapped",
            targetNamespace = "uri:org.apache.cxf.javascript.fortest")
public class SimpleDocLitWrappedImpl implements SimpleDocLitWrapped {
    
    @Resource
    private WebServiceContext context;
    private String lastString;
    private int lastInt;
    private long lastLong;
    private float lastFloat;
    private double lastDouble;
    private TestBean1 lastBean1;
    private TestBean1[] lastBean1Array;
    private SpecificGenericClass lastSpecificGeneric;
    private GenericGenericClass<Double> lastGenericGeneric;
    private InheritanceTestDerived lastInheritanceTestDerived;
    
    public String echoWithHeader(String what) {
        List<Header> headers = new ArrayList<Header>();
        Header dummyHeader;
        try {
            dummyHeader = new Header(new QName("uri:org.apache.cxf", "dummy"), "decapitated",
                                            new JAXBDataBinding(String.class));
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
        headers.add(dummyHeader);
        context.getMessageContext().put(Header.HEADER_LIST, headers);
        return what;
    }

    /** {@inheritDoc}*/
    public int basicTypeFunctionReturnInt(String s, int i, long l, float f, double d) {
        lastString = s;
        lastInt = i;
        lastLong = l;
        lastFloat = f;
        lastDouble = d;
        return 42;
    }

    /** {@inheritDoc}*/
    public String basicTypeFunctionReturnString(String s, int i, long l, float f, double d) {
        lastString = s;
        lastInt = i;
        lastLong = l;
        lastFloat = f;
        lastDouble = d;
        return "eels"; 
    }

    /** {@inheritDoc}*/
    public String basicTypeFunctionReturnStringNoWrappers(String s, int i, long l, float f, double d) {
        lastString = s;
        lastInt = i;
        lastLong = l;
        lastFloat = f;
        lastDouble = d;
        return "cetaceans"; 
    }

    /** {@inheritDoc}*/
    public void beanFunction(TestBean1 bean, TestBean1[] beans) {
        lastBean1 = bean;
        lastBean1Array = beans;
    }

    /** {@inheritDoc}*/
    public void beanFunctionWithWrapper(TestBean1 bean, TestBean1[] beans) {
        lastBean1 = bean;
        lastBean1Array = beans;
    }

    /** {@inheritDoc}*/
    public TestBean1 functionReturnTestBean1() {
        TestBean1 bean1 = new TestBean1();
        bean1.intItem = 42;
        return bean1;
    }

    public String getLastString() {
        return lastString;
    }

    public int getLastInt() {
        return lastInt;
    }

    public long getLastLong() {
        return lastLong;
    }

    public float getLastFloat() {
        return lastFloat;
    }

    public double getLastDouble() {
        return lastDouble;
    }

    public TestBean1 getLastBean1() {
        return lastBean1;
    }

    public TestBean1[] getLastBean1Array() {
        return lastBean1Array;
    }

    public void genericTestFunction(SpecificGenericClass sgc, GenericGenericClass<Double> ggc) {
        lastSpecificGeneric = sgc;
        lastGenericGeneric = ggc;
    }
    
    public void inheritanceTestFunction(InheritanceTestDerived d) {
        lastInheritanceTestDerived = d;
    }

    public SpecificGenericClass getLastSpecificGeneric() {
        return lastSpecificGeneric;
    }

    public GenericGenericClass<Double> getLastGenericGeneric() {
        return lastGenericGeneric;
    }

    public InheritanceTestDerived getLastInheritanceTestDerived() {
        return lastInheritanceTestDerived;
    }

}
