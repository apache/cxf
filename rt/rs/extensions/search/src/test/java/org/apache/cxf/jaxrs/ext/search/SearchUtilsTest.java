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
package org.apache.cxf.jaxrs.ext.search;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SearchUtilsTest {

    @Test
    public void testSqlWildcardString() {
        assertEquals("abc", SearchUtils.toSqlWildcardString("abc", false));
    }

    @Test
    public void testSqlWildcardStringAlways() {
        assertEquals("%abc%", SearchUtils.toSqlWildcardString("abc", true));
    }

    @Test
    public void testSqlWildcardString2() {
        assertEquals("%abc", SearchUtils.toSqlWildcardString("*abc", false));
    }

    @Test
    public void testSqlWildcardString3() {
        assertEquals("abc%", SearchUtils.toSqlWildcardString("abc*", false));
    }

    @Test
    public void testSqlWildcardString4() {
        assertEquals("%abc%", SearchUtils.toSqlWildcardString("*abc*", false));
    }

    @Test
    public void testSqlWildcardString5() {
        assertEquals("%", SearchUtils.toSqlWildcardString("*", false));
    }
}