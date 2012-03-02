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

import java.util.Date;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class BeanspectorTest extends Assert {
    
    @Test
    public void testSimpleBean() throws FiqlParseException {
        Beanspector<SimpleBean> bean = new Beanspector<SimpleBean>(new SimpleBean());
        Set<String> getters = bean.getGettersNames();
        assertEquals(3, getters.size());
        assertTrue(getters.contains("class"));
        assertTrue(getters.contains("a"));
        assertTrue(getters.contains("promised"));
        
        Set<String> setters = bean.getSettersNames();
        assertEquals(1, setters.size());
        assertTrue(getters.contains("a"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatchedAccessorTypes() throws FiqlParseException {
        new Beanspector<MismatchedTypes>(MismatchedTypes.class);
    }
    
    @Ignore
    static class MismatchedTypes {
        public Date getFoo() {
            return null;
        }

        public void setFoo(String val) {
        }
    }
    @Ignore
    static class SimpleBean {
        
        public boolean isPromised() {
            return true;
        }
        
        
        public String getA() {
            return "a";
        }

        public void setA(String val) {
        }
    }
}
