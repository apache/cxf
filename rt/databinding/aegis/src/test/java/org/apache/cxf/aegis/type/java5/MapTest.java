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

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.type.DefaultTypeMapping;
import org.apache.cxf.aegis.type.Type;
import org.apache.cxf.aegis.type.TypeCreationOptions;
import org.apache.cxf.aegis.type.collection.CollectionType;
import org.apache.cxf.aegis.type.collection.MapType;
import org.apache.cxf.aegis.type.java5.dto.MapDTO;
import org.apache.cxf.aegis.type.java5.dto.MapDTOService;
import org.junit.Before;
import org.junit.Test;

public class MapTest extends AbstractAegisTest {
    private DefaultTypeMapping tm;
    private Java5TypeCreator creator;

    @Before
    public void setUp() throws Exception {
        super.setUp();

        tm = new DefaultTypeMapping();
        creator = new Java5TypeCreator();
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testType() throws Exception {
        Method m = MapService.class.getMethod("getMap", new Class[0]);

        Type type = creator.createType(m, -1);
        tm.register(type);
        assertTrue(type instanceof MapType);

        MapType mapType = (MapType)type;
        QName keyName = mapType.getKeyName();
        assertNotNull(keyName);

        type = mapType.getKeyType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(String.class));

        type = mapType.getValueType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(Integer.class));
    }

    @Test
    public void testRecursiveType() throws Exception {
        Method m = MapService.class.getMethod("getMapOfCollections", new Class[0]);

        Type type = creator.createType(m, -1);
        tm.register(type);
        assertTrue(type instanceof MapType);

        MapType mapType = (MapType)type;
        QName keyName = mapType.getKeyName();
        assertNotNull(keyName);

        type = mapType.getKeyType();
        assertNotNull(type);
        assertTrue(type instanceof CollectionType);
        assertEquals(String.class, ((CollectionType)type).getComponentType().getTypeClass());

        type = mapType.getValueType();
        assertNotNull(type);
        assertTrue(type instanceof CollectionType);
        assertEquals(Double.class, ((CollectionType)type).getComponentType().getTypeClass());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testPDType() throws Exception {
        PropertyDescriptor pd = Introspector.getBeanInfo(MapDTO.class,
                                                         Object.class).getPropertyDescriptors()[0];
        Type type = creator.createType(pd);
        tm.register(type);
        assertTrue(type instanceof MapType);

        MapType mapType = (MapType)type;
        QName keyName = mapType.getKeyName();
        assertNotNull(keyName);

        type = mapType.getKeyType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(String.class));

        type = mapType.getValueType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(Integer.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testMapDTO() {
        tm = new DefaultTypeMapping();
        creator = new Java5TypeCreator();
        creator.setConfiguration(new TypeCreationOptions());
        tm.setTypeCreator(creator);

        Type dto = creator.createType(MapDTO.class);
        Set deps = dto.getDependencies();

        Type type = (Type)deps.iterator().next();
        assertTrue(type instanceof MapType);

        MapType mapType = (MapType)type;

        deps = dto.getDependencies();
        assertEquals(1, deps.size());

        type = mapType.getKeyType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(String.class));

        type = mapType.getValueType();
        assertNotNull(type);
        assertTrue(type.getTypeClass().isAssignableFrom(Integer.class));
    }

    @Test
    public void testMapDTOService() throws Exception {
        createService(MapDTOService.class);

        invoke("MapDTOService", "/org/apache/cxf/aegis/type/java5/dto/GetDTO.xml");
    }

    @Test
    public void testMapServiceWSDL() throws Exception {
        createService(MapDTOService.class);

        getWSDLDocument("MapDTOService");
    }

    public class MapService {
        public Map<String, Integer> getMap() {
            return null;
        }

        public void setMap(Map<String, Integer> strings) {

        }

        public Map<Collection<String>, Collection<Double>> getMapOfCollections() {
            return null;
        }
    }
}
