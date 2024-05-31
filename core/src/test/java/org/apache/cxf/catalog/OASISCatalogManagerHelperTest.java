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

package org.apache.cxf.catalog;

import java.io.IOException;

import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

public class OASISCatalogManagerHelperTest {

    @Test
    public void testResolve() throws IOException {
        OASISCatalogManagerHelper helper = new OASISCatalogManagerHelper();
        String target = "target";
        String base = "base";

        OASISCatalogManager manager = Mockito.mock(OASISCatalogManager.class);
        when(manager.resolveSystem(target)).thenReturn("resolvedLocationNotNull");

        String actual = helper.resolve(manager, target, base);
        String expected = "resolvedLocationNotNull";
        assertEquals(expected, actual);

        when(manager.resolveSystem(target)).thenReturn(null);
        actual = helper.resolve(manager, target, base);
        assertNull(actual);
    }

}
