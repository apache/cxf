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

import javax.xml.namespace.QName;


import org.apache.cxf.helpers.CastUtils;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SpringBeanQNameMapTest extends Assert {

    @Test
    public void testPersons() {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("org/apache/cxf/configuration/spring/beanQNameMap.xml");

        Map<QName, Person> beans = CastUtils.cast(((MapProvider)context.getBean("committers"))
                                                   .createMap());
        assertNotNull(beans);

        assertEquals(2, PersonQNameImpl.getLoadCount());
        assertEquals(4, beans.keySet().size());

        QName qn1 = new QName("http://cxf.apache.org", "anonymous");
        Person p1 = beans.get(qn1);
        assertNotNull(p1);
        assertEquals(2, PersonQNameImpl.getLoadCount());

        QName qn2 = new QName("http://cxf.apache.org", "myself");
        Person p2 = beans.get(qn2);
        assertNotNull(p2);
        assertEquals(3, PersonQNameImpl.getLoadCount());

        QName qn3 = new QName("http://cxf.apache.org", "other");
        Person p3 = beans.get(qn3);
        assertNotNull(p3);
        assertEquals(3, PersonQNameImpl.getLoadCount());
         
        QName qn4 = new QName("http://x.y.z", "other");
        Person p = beans.get(qn4);
        assertNotNull(p);
        assertSame(p3, p);
        assertEquals(3, PersonQNameImpl.getLoadCount());
        
        Collection<Person> values = beans.values();
        assertEquals(4, values.size());

        Set<Entry<QName, Person>> entries = beans.entrySet();
        assertEquals(4, entries.size());
        
        QName qn5 = new QName("http://a.b.c", "foo");
        Person p4 = new PersonQNameImpl();
        beans.put(qn5, p4);
       
        p = beans.get(qn5);
        assertEquals(p1, beans.get(qn1));
    }

}
