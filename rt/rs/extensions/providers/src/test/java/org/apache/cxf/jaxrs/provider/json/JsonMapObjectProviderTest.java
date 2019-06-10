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

package org.apache.cxf.jaxrs.provider.json;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.apache.cxf.jaxrs.json.basic.JsonMapObject;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonMapObjectProviderTest {

    @Test
    public void testReadWrite() throws Exception {
        JsonMapObjectProvider p = new JsonMapObjectProvider();

        JsonMapObject object = new JsonMapObject();
        object.setProperty("p", "v");

        assertTrue(p.isWriteable(JsonMapObject.class, null, null, null));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        p.writeTo(object, JsonMapObject.class, JsonMapObject.class, null, null, null, os);

        JsonMapObject result = p.readFrom(JsonMapObject.class, null, null,
                                       null, null, new ByteArrayInputStream(os.toByteArray()));
        assertEquals(object, result);
    }

}
