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
package org.apache.cxf.jaxb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.cxf.databinding.WrapperHelper;
import org.junit.Assert;
import org.junit.Test;

public class JAXBWrapperHelperTest extends Assert {


    @Test
    public void getBooleanTypeWrappedPart() throws Exception {
        SetIsOK ok = new SetIsOK();
        ok.setParameter3(new boolean[] {true, false});
        ok.setParameter4("hello");
        
        List<String> partNames = Arrays.asList(new String[] {
            "Parameter1",
            "Parameter2",
            "Parameter3",
            "Parameter4",
            "Parameter5",
        });
        List<String> elTypeNames = Arrays.asList(new String[] {
            "boolean",
            "int",
            "boolean",
            "string",
            "string",
        });
        List<Class<?>> partClasses = Arrays.asList(new Class<?>[] {
            Boolean.TYPE,
            Integer.TYPE,
            boolean[].class,
            String.class,
            List.class,
        });
        
        WrapperHelper wh = new JAXBDataBinding().createWrapperHelper(SetIsOK.class,
                                                                     null,
                                          partNames,
                                          elTypeNames,
                                          partClasses);
        
        List<Object> lst = wh.getWrapperParts(ok);
        assertEquals(5, lst.size());
        assertTrue(lst.get(0) instanceof Boolean);
        assertTrue(lst.get(1) instanceof Integer);
        assertTrue(lst.get(2) instanceof boolean[]);
        assertTrue(((boolean[])lst.get(2))[0]);
        assertFalse(((boolean[])lst.get(2))[1]);
        assertEquals("hello", (String)lst.get(3));

        lst.set(0, Boolean.TRUE);
        Object o = wh.createWrapperObject(lst);
        assertNotNull(0);
        ok = (SetIsOK)o;
        assertTrue(ok.isParameter1());
        assertTrue(ok.getParameter3()[0]);
        assertFalse(ok.getParameter3()[1]);
        assertEquals("hello", ok.getParameter4());
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = { "parameter1", "parameter2", "parameter3", "parameter4" })
    @XmlRootElement(name = "setIsOK")
    public static class SetIsOK {

        @XmlElement(name = "Parameter1")
        protected boolean parameter1;
        @XmlElement(name = "Parameter2")
        protected int parameter2;
        @XmlElement(name = "Parameter3")
        protected boolean parameter3[];
        @XmlElement(name = "Parameter4")
        protected String parameter4;
        @XmlElement(name = "Parameter5")
        protected List<String> parameter5 = new ArrayList<String>();
        
        
        /**
         * Gets the value of the parameter1 property.
         * 
         */
        public boolean isParameter1() {
            return parameter1;
        }

        /**
         * Sets the value of the parameter1 property.
         * 
         */
        public void setParameter1(boolean value) {
            this.parameter1 = value;
        }

        /**
         * Gets the value of the parameter2 property.
         * 
         */
        public int getParameter2() {
            return parameter2;
        }

        /**
         * Sets the value of the parameter2 property.
         * 
         */
        public void setParameter2(int value) {
            this.parameter2 = value;
        }
        
        
        /**
         * Gets the value of the parameter2 property.
         * 
         */
        public boolean[] getParameter3() {
            return parameter3;
        }

        /**
         * Sets the value of the parameter2 property.
         * 
         */
        public void setParameter3(boolean value[]) {
            this.parameter3 = value;
        }
        
        public String getParameter4() {
            return parameter4;
        }
        public void setParameter4(String value) {
            this.parameter4 = value;
        }

        public List<String> getParameter5() {
            return parameter5;
        }
    }
}

