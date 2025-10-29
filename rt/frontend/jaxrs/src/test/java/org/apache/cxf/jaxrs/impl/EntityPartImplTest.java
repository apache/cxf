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

package org.apache.cxf.jaxrs.impl;

import java.io.IOException;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.MediaType;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class EntityPartImplTest {
    @Test
    public void testCreateEntityPart() throws WebApplicationException, IOException {
        final EntityPart entityPart = EntityPart
            .withName("greeting")
            .content("hello")
            .build();
        assertThat(entityPart, is(not(nullValue())));
        assertThat(entityPart.getContent(), is(not(nullValue())));
    }
    
    @Test
    public void testCreateGenericEntityPart() throws WebApplicationException, IOException {
        final String content = "hello";
        @SuppressWarnings("unchecked")
        final EntityPart entityPart = EntityPart
            .withName("greeting")
            .mediaType(MediaType.APPLICATION_SVG_XML_TYPE)
            .content(content, GenericType.forInstance(content))
            .build();
        assertThat(entityPart, is(not(nullValue())));
        assertThat(entityPart.getContent(), is(not(nullValue())));
    }
}
