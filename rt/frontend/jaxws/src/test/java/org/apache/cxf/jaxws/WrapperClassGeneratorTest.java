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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;

import org.apache.cxf.BusFactory;
import org.apache.cxf.databinding.WrapperHelper;
import org.apache.cxf.jaxb.JAXBDataBinding;
import org.apache.cxf.jaxws.service.AddNumbersImpl;
import org.apache.cxf.jaxws.support.JaxWsImplementorInfo;
import org.apache.cxf.jaxws.support.JaxWsServiceFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.service.model.InterfaceInfo;
import org.apache.cxf.service.model.OperationInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.junit.After;
import org.junit.Assert;

public class WrapperClassGeneratorTest extends Assert {
    
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
        
        
        ServiceInfo serviceInfo =  service.getServiceInfos().get(0);
        
        InterfaceInfo interfaceInfo = serviceInfo.getInterface();
        OperationInfo inf = interfaceInfo.getOperations().iterator().next();
        Class requestClass = inf.getInput().getMessagePart(0).getTypeClass();
        Class responseClass = inf.getOutput().getMessagePart(0).getTypeClass();
                        
        // Create request wrapper Object
        List<String> partNames = Arrays.asList(new String[] {"arg0"});
        List<String> elTypeNames = Arrays.asList(new String[] {"list"});
        List<Class<?>> partClasses = Arrays.asList(new Class<?>[] {List.class});
           
        String className = requestClass.getName();
        className = className.substring(0, className.lastIndexOf(".") + 1);
        
        WrapperHelper wh = new JAXBDataBinding().createWrapperHelper(requestClass,
                                                             partNames, elTypeNames, partClasses);        
        
        List<Object> paraList = new ArrayList<Object>();
        List<String> valueList = new ArrayList<String>();
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
        className = className.substring(0, className.lastIndexOf(".") + 1);
        
        wh = new JAXBDataBinding().createWrapperHelper(responseClass,
                                                             partNames, elTypeNames, partClasses);        
        List<Object> resPara = new ArrayList<Object>();
        List<Integer> intValueList = new ArrayList<Integer>();
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
        String expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" 
            + "<ns2:addNumbers xmlns:ns2=\"http://service.jaxws.cxf.apache.org/\">" 
            + "<arg0>str1 str2 str3</arg0></ns2:addNumbers>";

        assertEquals("The generated request wrapper class does not contain the correct annotations", 
                     expected, bout.toString());
       
        
        bout.reset();
        marshaller.marshal(responseObj, bout);       
        expected = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" 
            + "<ns2:addNumbersResponse xmlns:ns2=\"http://service.jaxws.cxf.apache.org/\">" 
            + "<return>1</return><return>2</return><return>3</return></ns2:addNumbersResponse>";
        assertEquals("The generated response wrapper class is not correct", expected,  bout.toString());
     
    }
  
}
