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
package org.apache.cxf.configuration.spring;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cxf.helpers.CastUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringBeanMapTest extends Assert {
    @Test
    public void testPersons() {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("org/apache/cxf/configuration/spring/beanMap.xml");

        
        Map<String, Person> beans = CastUtils.cast(((MapProvider)context.getBean("mapOfPersons"))
                                                       .createMap());
        assertNotNull(beans);

        assertEquals(2, beans.size());

        Person p = (Person)beans.get("dan");
        assertNotNull(p);
 
        assertEquals(1, PersonImpl.getLoadCount());
        
        Person jane = (Person)beans.get("jane");
        assertNotNull(jane);
        assertEquals(2, PersonImpl.getLoadCount());
        
        Collection<Person> values = beans.values();
        assertEquals(2, values.size());

        Set<Entry<String, Person>> entries = beans.entrySet();
        assertEquals(2, entries.size());
        
        Person p3 = new PersonImpl();
        beans.put("foo", p3);
        
        assertNotNull(beans.get("foo"));
        
        beans.put("dan", p3);
        assertEquals(p3, beans.get("dan"));
    }
}
