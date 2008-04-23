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

package org.apache.cxf.aegis.override;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.aegis.inheritance.Employee;
import org.apache.cxf.databinding.DataReader;
import org.junit.Test;

/**
 * 
 */
public class OverrideTypeTest extends AbstractAegisTest {
    
    @Test
    public void testOverrideBean() throws Exception {
        AegisDatabinding aegisDatabinding = new AegisDatabinding();
        Set<String> types = new HashSet<String>();
        types.add("org.apache.cxf.aegis.inheritance.Employee");
        aegisDatabinding.setOverrideTypes(types);
        DataReader<XMLStreamReader> dataReader = 
            aegisDatabinding.createReader(XMLStreamReader.class);
        InputStream employeeBytes = 
            testUtilities.getResourceAsStream("/org/apache/cxf/aegis/override/employee.xml");
        
        XMLInputFactory readerFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = readerFactory.createXMLStreamReader(employeeBytes);
        Object objectRead = dataReader.read(reader);
        assertNotNull(objectRead);
        assertTrue(objectRead instanceof Employee);
        Employee e = (Employee)objectRead;
        assertEquals("long", e.getDivision());
    }

}
