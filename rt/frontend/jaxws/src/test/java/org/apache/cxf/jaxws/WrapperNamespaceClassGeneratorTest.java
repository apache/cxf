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

package org.apache.cxf.jaxws;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.ASMHelper;
import org.apache.cxf.common.util.ASMHelperImpl;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxb.WrapperHelperClassLoader;
import org.apache.cxf.jaxb.WrapperHelperCreator;
import org.apache.cxf.jaxws.service.AddNumbersImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;

import org.junit.After;

import static org.junit.Assert.assertTrue;

public class WrapperNamespaceClassGeneratorTest {

    @After
    public void tearDown() {
        BusFactory.setDefaultBus(null);
    }

    @org.junit.Test
    public void testForXmlList() throws Exception {
        JaxWsImplementorInfo implInfo =
            new JaxWsImplementorInfo(AddNumbersImpl.class);
        JaxWsServiceFactoryBean jaxwsFac = new JaxWsServiceFactoryBean(implInfo);
        jaxwsFac.setBus(BusFactory.getDefaultBus());
        Service service = jaxwsFac.create();


        ServiceInfo serviceInfo = service.getServiceInfos().get(0);

        InterfaceInfo interfaceInfo = serviceInfo.getInterface();
        OperationInfo inf = interfaceInfo.getOperations().iterator().next();
        Class<?> requestClass = inf.getInput().getMessagePart(0).getTypeClass();
        Class<?> responseClass = inf.getOutput().getMessagePart(0).getTypeClass();

        // Create request wrapper Object
        List<String> partNames = Arrays.asList(new String[] {"arg0"});
        List<String> elTypeNames = Arrays.asList(new String[] {"list"});
        List<Class<?>> partClasses = Arrays.asList(new Class<?>[] {List.class});

        String className = requestClass.getName();
        className = className.substring(0, className.lastIndexOf('.') + 1);

        WrapperHelper wh = new JAXBDataBinding().createWrapperHelper(requestClass, null,
                                                             partNames, elTypeNames, partClasses);

        List<Object> paraList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        valueList.add("str1");
        valueList.add("str2");
        valueList.add("str3");
        paraList.add(valueList);
        Object requestObj = wh.createWrapperObject(paraList);
        // Create response wrapper Object

        partNames = Arrays.asList(new String[] {"return"});
        elTypeNames = Arrays.asList(new String[] {"list"});
        partClasses = Arrays.asList(new Class<?>[] {List.class});

        className = responseClass.getName();
        className = className.substring(0, className.lastIndexOf('.') + 1);

        wh = new JAXBDataBinding().createWrapperHelper(responseClass, null,
                                                             partNames, elTypeNames, partClasses);
        List<Object> resPara = new ArrayList<>();
        List<Integer> intValueList = new ArrayList<>();
        intValueList.add(1);
        intValueList.add(2);
        intValueList.add(3);
        resPara.add(intValueList);
        Object responseObj = wh.createWrapperObject(resPara);

        JAXBContext jaxbContext = JAXBContext.newInstance(requestClass, responseClass);
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        Marshaller marshaller = jaxbContext.createMarshaller();

        //check marshall wrapper
        marshaller.marshal(requestObj, bout);
        String expected = "<arg0>str1 str2 str3</arg0>";

        assertTrue("The generated request wrapper class does not contain the correct annotations",
                   bout.toString().contains(expected));


        bout.reset();
        marshaller.marshal(responseObj, bout);
        expected = "<return>1</return><return>2</return><return>3</return>";
        assertTrue("The generated response wrapper class is not correct", bout.toString().contains(expected));

    }
    public class CustomClassLoader extends ClassLoader {
        ConcurrentHashMap<String, Class<?>> defined = new ConcurrentHashMap<>();

        CustomClassLoader(ClassLoader parent) {
            super(parent);
        }
        public Class<?> lookupDefinedClass(String name) {
            return defined.get(StringUtils.slashesToPeriod(name));
        }

        public Class<?> defineClass(String name, byte[] bytes) {
            Class<?> ret = defined.get(StringUtils.slashesToPeriod(name));
            if (ret != null) {
                return ret;
            }

            ret = defined.computeIfAbsent(StringUtils.slashesToPeriod(name),
                key -> CustomClassLoader.super.defineClass(key, bytes, 0, bytes.length));

            return ret;
        }
    }
    public class ProxyASMHelper extends ASMHelperImpl {
        private CustomClassLoader customClassLoader;

        public ProxyASMHelper(CustomClassLoader customClassLoader) {
            this.customClassLoader = customClassLoader;
        }
        public ClassWriter createClassWriter() {
            ClassWriter parent = super.createClassWriter();
            return new ProxyClassWriter(parent, this);
        }

        public void notif(String className, byte[] bytes) {
            customClassLoader.defineClass(className, bytes);
        }
    }
    public class ProxyClassWriter implements ASMHelper.ClassWriter {
        private final ASMHelper.ClassWriter proxy;
        private final ProxyASMHelper handler;
        private String className;

        public ProxyClassWriter(ASMHelper.ClassWriter parent, ProxyASMHelper handler) {
            this.proxy = parent;
            this.handler = handler;
        }
        @Override
        public ASMHelper.AnnotationVisitor visitAnnotation(String cls, boolean t) {
            return proxy.visitAnnotation(cls, t);
        }

        @Override
        public ASMHelper.FieldVisitor visitField(int accPrivate, String fieldName, String classCode,
                                                 String fieldDescriptor, Object object) {
            return proxy.visitField(accPrivate, fieldName, classCode, fieldDescriptor, object);
        }

        @Override
        public void visitEnd() {
            proxy.visitEnd();
        }

        @Override
        public byte[] toByteArray() {
            byte[] bytes = proxy.toByteArray();
            handler.notif(className, bytes);
            return bytes;
        }

        @Override
        public ASMHelper.MethodVisitor visitMethod(int accPublic, String string, String string2, String s3,
                                                   String[] s4) {
            return proxy.visitMethod(accPublic, string, string2, s3, s4);
        }

        @Override
        public void visit(int v15, int i, String newClassName, String object, String string, String[] object2) {
            className = newClassName;
            proxy.visit(v15, i, newClassName, object, string, object2);
        }

        @Override
        public void visitSource(String arg0, String arg1) {
            proxy.visitSource(arg0, arg1);
        }
    }
    @org.junit.Test
    public void testGeneratedFirst() throws Exception {
        JaxWsImplementorInfo implInfo =
                new JaxWsImplementorInfo(AddNumbersImpl.class);
        JaxWsServiceFactoryBean jaxwsFac = new JaxWsServiceFactoryBean(implInfo);
        jaxwsFac.setBus(BusFactory.getDefaultBus());
        Service service = jaxwsFac.create();


        ServiceInfo serviceInfo = service.getServiceInfos().get(0);

        InterfaceInfo interfaceInfo = serviceInfo.getInterface();
        OperationInfo inf = interfaceInfo.getOperations().iterator().next();
        Class<?> requestClass = inf.getInput().getMessagePart(0).getTypeClass();
        Class<?> responseClass = inf.getOutput().getMessagePart(0).getTypeClass();

        // Create request wrapper Object
        List<String> partNames = Arrays.asList(new String[] {"arg0"});
        List<String> elTypeNames = Arrays.asList(new String[] {"list"});
        List<Class<?>> partClasses = Arrays.asList(new Class<?>[] {List.class});

        String className = requestClass.getName();
        className = className.substring(0, className.lastIndexOf('.') + 1);
        Bus bus = jaxwsFac.getBus();
        CustomClassLoader cl = new CustomClassLoader(WrapperNamespaceClassGeneratorTest.class.getClassLoader());

        bus.setExtension(new ProxyASMHelper(cl), ASMHelper.class);
        // generate class and store it to class loader
        WrapperHelper wh = new JAXBDataBinding().createWrapperHelper(requestClass, null,
                partNames, elTypeNames, partClasses);
        // now no more generation is allowed
        WrapperHelperClassLoader wrapperHelperClassLoader = new WrapperHelperClassLoader(bus);
        bus.setExtension(wrapperHelperClassLoader, WrapperHelperCreator.class);
        bus.setExtension(cl, ClassLoader.class);
        wh = new JAXBDataBinding().createWrapperHelper(requestClass, null,
                partNames, elTypeNames, partClasses);
        List<Object> paraList = new ArrayList<>();
        List<String> valueList = new ArrayList<>();
        valueList.add("str1");
        valueList.add("str2");
        valueList.add("str3");
        paraList.add(valueList);
        Object requestObj = wh.createWrapperObject(paraList);
        // Create response wrapper Object

        partNames = Arrays.asList(new String[] {"return"});
        elTypeNames = Arrays.asList(new String[] {"list"});
        partClasses = Arrays.asList(new Class<?>[] {List.class});

        className = responseClass.getName();
        className = className.substring(0, className.lastIndexOf('.') + 1);

        wh = new JAXBDataBinding().createWrapperHelper(responseClass, null,
                partNames, elTypeNames, partClasses);
        List<Object> resPara = new ArrayList<>();
        List<Integer> intValueList = new ArrayList<>();
        intValueList.add(1);
        intValueList.add(2);
        intValueList.add(3);
        resPara.add(intValueList);
        Object responseObj = wh.createWrapperObject(resPara);

        JAXBContext jaxbContext = JAXBContext.newInstance(requestClass, responseClass);
        java.io.ByteArrayOutputStream bout = new java.io.ByteArrayOutputStream();
        Marshaller marshaller = jaxbContext.createMarshaller();

        //check marshall wrapper
        marshaller.marshal(requestObj, bout);
        String expected = "<arg0>str1 str2 str3</arg0>";

        assertTrue("The generated request wrapper class does not contain the correct annotations",
                bout.toString().contains(expected));


        bout.reset();
        marshaller.marshal(responseObj, bout);
        expected = "<return>1</return><return>2</return><return>3</return>";
        assertTrue("The generated response wrapper class is not correct", bout.toString().contains(expected));

    }
}