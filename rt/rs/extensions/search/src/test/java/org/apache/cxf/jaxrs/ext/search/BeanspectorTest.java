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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BeanspectorTest {

    @Test
    public void testSimpleBean() throws SearchParseException {
        Beanspector<SimpleBean> bean = new Beanspector<>(new SimpleBean());
        Set<String> getters = bean.getGettersNames();
        assertEquals(3, getters.size());
        assertTrue(getters.contains("class"));
        assertTrue(getters.contains("a"));
        assertTrue(getters.contains("promised"));

        Set<String> setters = bean.getSettersNames();
        assertEquals(2, setters.size());
        assertTrue(setters.contains("a"));
        assertTrue(setters.contains("fluent"));
    }
    
    @Test
    public void testOverriddenBeans1() throws SearchParseException {
        Beanspector<OverriddenBean> bean = new Beanspector<OverriddenBean>(new OverriddenBean());
        Set<String> getters = bean.getGettersNames();
        assertEquals(2, getters.size());
        assertTrue(getters.contains("class"));
        assertTrue(getters.contains("simplebean"));
        
        Set<String> setters = bean.getSettersNames();
        assertEquals(1, setters.size());
        assertTrue(setters.contains("simplebean"));
    }
    
    @Test
    public void testOverriddenBeans2() throws SearchParseException {
        Beanspector<AntoherOverriddenBean> bean = new Beanspector<AntoherOverriddenBean>(new AntoherOverriddenBean());
        Set<String> getters = bean.getGettersNames();
        assertEquals(2, getters.size());
        assertTrue(getters.contains("class"));
        assertTrue(getters.contains("simplebean"));
        
        Set<String> setters = bean.getSettersNames();
        assertEquals(1, setters.size());
        assertTrue(setters.contains("simplebean"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testMismatchedOverriddenBeans() throws SearchParseException {
        new Beanspector<MismatchedOverriddenBean>(new MismatchedOverriddenBean());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMismatchedAccessorTypes() throws SearchParseException {
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
        
        public SimpleBean setFluent(String val) {
            return this;
        }
    }
   
    @Ignore 
    static class OverriddenSimpleBean extends SimpleBean {
        
        OverriddenSimpleBean() { }
        
        OverriddenSimpleBean(SimpleBean arg) { }
    }
    
    @Ignore 
    static class AnotherBean {
        
        protected SimpleBean simpleBean;

        public SimpleBean getSimpleBean() {
            return simpleBean;
        }

        public void setSimpleBean(SimpleBean simpleBean) {
            this.simpleBean = simpleBean;
        }        
    }
    
    @Ignore 
    static class OverriddenBean extends AnotherBean {
                
        @Override
        public OverriddenSimpleBean getSimpleBean() {
            return new OverriddenSimpleBean(simpleBean);
        }

        public void setSimpleBean(OverriddenSimpleBean simpleBean) {
            this.simpleBean = simpleBean;
        }        
    }
    
    @Ignore 
    static class AntoherOverriddenBean extends AnotherBean {
                
        @Override
        public OverriddenSimpleBean getSimpleBean() {
            return new OverriddenSimpleBean(simpleBean);
        }
    }
    
    @Ignore 
    static class MismatchedOverriddenBean extends AnotherBean {
                
        @Override
        public OverriddenSimpleBean getSimpleBean() {
            return new OverriddenSimpleBean(simpleBean);
        }
        
        public void setSimpleBean(String simpleBean) { }
    }
}
