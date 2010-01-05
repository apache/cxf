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

package org.apache.cxf.aegis.type.java5;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.databinding.XFireCompatibilityServiceConfiguration;
import org.apache.cxf.frontend.ClientProxyFactoryBean;

import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class CollectionTestsWithService extends AbstractAegisTest {
    
    private CollectionServiceInterface csi;
    private CollectionService impl;

    @Before
    public void before() {
        impl = new CollectionService();
        createService(CollectionServiceInterface.class, impl, null);
        
        ClientProxyFactoryBean proxyFac = new ClientProxyFactoryBean();
        proxyFac.getServiceFactory().getServiceConfigurations().add(0, 
                                                              new XFireCompatibilityServiceConfiguration());
        proxyFac.setServiceClass(CollectionServiceInterface.class);
        proxyFac.setDataBinding(new AegisDatabinding());
        proxyFac.setAddress("local://CollectionServiceInterface");
        proxyFac.setBus(getBus());

        csi = (CollectionServiceInterface)proxyFac.create();
    }
    
    /**
     * CXF-2017
     * @throws Exception
     */
    @Test
    public void testNestedMap() throws Exception {
        Map<String, Map<String, BeanWithGregorianDate>> complexMap;
        complexMap = new HashMap<String, Map<String, BeanWithGregorianDate>>();
        Map<String, BeanWithGregorianDate> innerMap = new HashMap<String, BeanWithGregorianDate>();
        BeanWithGregorianDate bean = new BeanWithGregorianDate();
        bean.setName("shem bean");
        bean.setId(42);
        innerMap.put("firstBean", bean);
        complexMap.put("firstKey", innerMap);
        csi.mapOfMapWithStringAndPojo(complexMap);
        
        Map<String, Map<String, BeanWithGregorianDate>> gotMap = impl.getLastComplexMap();
        assertTrue(gotMap.containsKey("firstKey"));
        Map<String, BeanWithGregorianDate> v = gotMap.get("firstKey");
        BeanWithGregorianDate b = v.get("firstBean");
        assertNotNull(b);
        
    }
    
    @Test
    public void testListTypes() throws Exception {
        SortedSet<String> strings = new TreeSet<String>();
        strings.add("Able");
        strings.add("Baker");
        String first = csi.takeSortedStrings(strings);
        assertEquals("Able", first);
        
        //CHECKSTYLE:OFF
        HashSet<String> hashedSet = new HashSet<String>();
        hashedSet.addAll(strings);
        String countString = csi.takeUnsortedSet(hashedSet);
        assertEquals("2", countString);
        //CHECKSTYLE:ON
        
    }
    
    @Test
    public void returnValueIsCollectionOfArrays() {
        Collection<double[]> doubleDouble = csi.returnCollectionOfPrimitiveArrays();
        assertEquals(2, doubleDouble.size());
        double[][] data = new double[2][];

        for (double[] array : doubleDouble) {
            if (array.length == 3) {
                data[0] = array;
            } else {
                data[1] = array;
            }
        }
        assertNotNull(data[0]);
        assertNotNull(data[1]);
        assertEquals(3.14, data[0][0], .0001);
        assertEquals(2, data[0][1], .0001);
        assertEquals(-666.6, data[0][2], .0001);
        assertEquals(-666.6, data[1][0], .0001);
        assertEquals(3.14, data[1][1], .0001);
        assertEquals(2.0, data[1][2], .0001);
    }

}
