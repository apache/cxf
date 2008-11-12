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

import java.net.URL;

import javax.xml.namespace.QName;

import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.test.AbstractCXFTest;

import org.junit.After;
import org.junit.Before;
//import org.junit.Ignore;
import org.junit.Test;

public class XmlBeansTest extends AbstractCXFTest {

    private static final String CONFIG1 = "org/apache/cxf/xmlbeans/cxf.xml";
    private static final String CONFIG2 = "org/apache/cxf/xmlbeans/cxf2.xml";
    private static final String ERROR_MSG = "Service class org.apache.cxf.xmlbeans.GreeterMine method " 
        +    "sayHi2 part {http://cxf.apache.org/xmlbeans}in cannot be mapped to schema";   
    private static final String ERROR_MSG2 = "Could not send Message";
    private SpringBusFactory bf;

    @Before
    public void setUp() throws Exception {
        bf = new SpringBusFactory();

    }

    @After
    public void tearDown() throws Exception {
        if (bus != null) {
            bus.shutdown(false);
            bus = null;
        } 
        BusFactory.setDefaultBus(null);
    }
    
    
    @Test
    public void testBusCreationFails() throws Exception {
        try {
            bf = new SpringBusFactory();
            bus = bf.createBus(CONFIG1);
            BusFactory.setDefaultBus(bus);
        } catch (Exception ex) {       
            assertTrue(ex.getMessage().contains(ERROR_MSG));
        }
    }

    @Test
    public void testBasicFails() throws Exception {
        
        try {
            bf = new SpringBusFactory();
            bus = bf.createBus(CONFIG2);
            BusFactory.setDefaultBus(bus);
            URL wsdlURL = XmlBeansTest.class.getResource("xmlbeanstest.wsdl");
            SOAPMineService ss =
                new SOAPMineService(wsdlURL,
                                    new QName("http://cxf.apache.org/xmlbeans", "SOAPMineService"));
            GreeterMine port = ss.getSoapPort();

            StringListType stringListType = new StringListType();
            stringListType.setMyname("sean");
            stringListType.setMyaddress("home");
            port.sayHi2(stringListType);
        } catch (Exception ex) {          
            assertTrue(ex.getMessage().contains(ERROR_MSG2));
            //ex.printStackTrace();
        }
    }
    


}
