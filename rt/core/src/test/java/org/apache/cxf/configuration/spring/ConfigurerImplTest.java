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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.DatatypeConverterInterface;
import javax.xml.bind.annotation.adapters.HexBinaryAdapter;
import javax.xml.namespace.QName;

import org.apache.cxf.bus.spring.BusApplicationContext;
import org.apache.cxf.configuration.Configurable;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;





public class ConfigurerImplTest extends Assert {
    
    static {
        Class<?> cls;
        try {
            try {
                cls = Class.forName("com.sun.xml.bind.DatatypeConverterImpl");
            } catch (ClassNotFoundException e) {
                cls = Class.forName("com.sun.xml.internal.bind.DatatypeConverterImpl");
            }
            DatatypeConverterInterface convert = (DatatypeConverterInterface)cls.getField("theInstance")
                                                                                .get(null);
            DatatypeConverter.setDatatypeConverter(convert);
        } catch (Exception ex) {
            //ignore;
        }
    }
    
    @Test
    public void testConfigureSimpleNoMatchingBean() {
        SimpleBean sb = new SimpleBean("unknown");
        
        BusApplicationContext ac = 
            new BusApplicationContext("/org/apache/cxf/configuration/spring/test-beans.xml",
                                      false);

        ConfigurerImpl configurer = new ConfigurerImpl(ac);
        configurer.configureBean(sb);
        assertEquals("Unexpected value for attribute stringAttr", 
                     "hello", sb.getStringAttr());
        assertTrue("Unexpected value for attribute booleanAttr", 
                   sb.getBooleanAttr());
        assertEquals("Unexpected value for attribute integerAttr", 
                     BigInteger.ONE, sb.getIntegerAttr());
        assertEquals("Unexpected value for attribute intAttr", 
                     new Integer(2), sb.getIntAttr());
        assertEquals("Unexpected value for attribute longAttr", 
                     new Long(3L), sb.getLongAttr());
        assertEquals("Unexpected value for attribute shortAttr", 
                     new Short((short)4), sb.getShortAttr());
        assertEquals("Unexpected value for attribute decimalAttr", 
                     new BigDecimal("5"), sb.getDecimalAttr());
        assertEquals("Unexpected value for attribute floatAttr", 
                     new Float(6F), sb.getFloatAttr());
        assertEquals("Unexpected value for attribute doubleAttr", 
                     new Double(7D), sb.getDoubleAttr());
        assertEquals("Unexpected value for attribute byteAttr", 
                     new Byte((byte)8), sb.getByteAttr());
        
        QName qn = sb.getQnameAttr();
        assertEquals("Unexpected value for attribute qnameAttrNoDefault", 
                     "schema", qn.getLocalPart());
        assertEquals("Unexpected value for attribute qnameAttrNoDefault",
                     "http://www.w3.org/2001/XMLSchema", qn.getNamespaceURI());
        byte[] expected = DatatypeConverter.parseBase64Binary("abcd");
        byte[] val = sb.getBase64BinaryAttr();
        assertEquals("Unexpected value for attribute base64BinaryAttrNoDefault", expected.length, val.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for attribute base64BinaryAttrNoDefault", expected[i], val[i]);
        }
        expected = new HexBinaryAdapter().unmarshal("aaaa");
        val = sb.getHexBinaryAttr();
        assertEquals("Unexpected value for attribute hexBinaryAttrNoDefault", expected.length, val.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for attribute hexBinaryAttrNoDefault", expected[i], val[i]);
        }
        
        assertEquals("Unexpected value for attribute unsignedIntAttrNoDefault", 
                     new Long(9L), sb.getUnsignedIntAttr());
        assertEquals("Unexpected value for attribute unsignedShortAttrNoDefault", 
                     new Integer(10), sb.getUnsignedShortAttr());
        assertEquals("Unexpected value for attribute unsignedByteAttrNoDefault", 
                     new Short((short)11), sb.getUnsignedByteAttr());
    }
    
    @Test
    public void testConfigureSimple() {
        SimpleBean sb = new SimpleBean("simple");
        BusApplicationContext ac = 
            new BusApplicationContext("/org/apache/cxf/configuration/spring/test-beans.xml",
                                      false);

        ConfigurerImpl configurer = new ConfigurerImpl();
        configurer.setApplicationContext(ac);
        
        configurer.configureBean(sb);
        assertEquals("Unexpected value for attribute stringAttr", 
                     "hallo", sb.getStringAttr());
        assertTrue("Unexpected value for attribute booleanAttr", 
                   !sb.getBooleanAttr());
        assertEquals("Unexpected value for attribute integerAttr", 
                     BigInteger.TEN, sb.getIntegerAttr());
        assertEquals("Unexpected value for attribute intAttr", 
                     new Integer(12), sb.getIntAttr());
        assertEquals("Unexpected value for attribute longAttr", 
                     new Long(13L), sb.getLongAttr());
        assertEquals("Unexpected value for attribute shortAttr", 
                     new Short((short)14), sb.getShortAttr());
        assertEquals("Unexpected value for attribute decimalAttr", 
                     new BigDecimal("15"), sb.getDecimalAttr());
        assertEquals("Unexpected value for attribute floatAttr", 
                     new Float(16F), sb.getFloatAttr());
        assertEquals("Unexpected value for attribute doubleAttr", 
                     new Double(17D), sb.getDoubleAttr());
        assertEquals("Unexpected value for attribute byteAttr", 
                     new Byte((byte)18), sb.getByteAttr());
           
        /*
        QName qn = sb.getQnameAttr();
        assertEquals("Unexpected value for attribute qnameAttrNoDefault", 
                     "string", qn.getLocalPart());
        assertEquals("Unexpected value for attribute qnameAttrNoDefault",
                     "http://www.w3.org/2001/XMLSchema", qn.getNamespaceURI());
        */
        
        /*
        byte[] expected = DatatypeConverter.parseBase64Binary("wxyz");
        byte[] val = sb.getBase64BinaryAttr(); 
        assertEquals("Unexpected value for attribute base64BinaryAttrNoDefault", expected.length, val.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for attribute base64BinaryAttrNoDefault", expected[i], val[i]);
        }

        expected = new HexBinaryAdapter().unmarshal("bbbb");
        val = sb.getHexBinaryAttr();
        assertEquals("Unexpected value for attribute hexBinaryAttrNoDefault", expected.length, val.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals("Unexpected value for attribute hexBinaryAttrNoDefault", expected[i], val[i]);
        }
        */
        
        assertEquals("Unexpected value for attribute unsignedIntAttrNoDefault", 
                     new Long(19L), sb.getUnsignedIntAttr());
        assertEquals("Unexpected value for attribute unsignedShortAttrNoDefault", 
                     new Integer(20), sb.getUnsignedShortAttr());
        assertEquals("Unexpected value for attribute unsignedByteAttrNoDefault", 
                     new Short((short)21), sb.getUnsignedByteAttr());
    }
    
    @Test
    public void testConfigureSimpleMatchingStarBeanId() {
        SimpleBean sb = new SimpleBean("simple2");
        BusApplicationContext ac = 
            new BusApplicationContext("/org/apache/cxf/configuration/spring/test-beans.xml",
                                      false);

        ConfigurerImpl configurer = new ConfigurerImpl();
        configurer.setApplicationContext(ac);
        configurer.configureBean(sb);
        assertTrue("Unexpected value for attribute booleanAttr", 
                   !sb.getBooleanAttr());
        assertEquals("Unexpected value for attribute integerAttr", 
                     BigInteger.TEN, sb.getIntegerAttr());
        assertEquals("Unexpected value for attribute stringAttr", 
                     "StarHallo", sb.getStringAttr());
    }
    
    @Test
    public void testConfigureSimpleMatchingStarBeanIdWithChildInstance() {
        SimpleBean sb = new ChildBean("simple2");
        BusApplicationContext ac = 
            new BusApplicationContext("/org/apache/cxf/configuration/spring/test-beans.xml",
                                      false);

        ConfigurerImpl configurer = new ConfigurerImpl();
        configurer.setApplicationContext(ac);
        configurer.configureBean(sb);
        assertTrue("Unexpected value for attribute booleanAttr", 
                   !sb.getBooleanAttr());
        assertEquals("Unexpected value for attribute integerAttr", 
                     BigInteger.TEN, sb.getIntegerAttr());
        assertEquals("Unexpected value for attribute stringAttr", 
                     "StarHallo", sb.getStringAttr());
    }
    
    @Test
    public void testGetBeanName() {
        ConfigurerImpl configurer = new ConfigurerImpl();
        Object beanInstance = new Configurable() {

            public String getBeanName() {
                return "a";
            }
        };
        assertEquals("a", configurer.getBeanName(beanInstance));
        final class NamedBean {
            @SuppressWarnings("unused")
            public String getBeanName() {
                return "b";
            }
        }
        beanInstance = new NamedBean(); 
        assertEquals("b", configurer.getBeanName(beanInstance));
        beanInstance = this;
        assertNull(configurer.getBeanName(beanInstance));
    }
    
    @Test
    public void testAddApplicationContext() {
        ConfigurableApplicationContext context1 =
            new ClassPathXmlApplicationContext("/org/apache/cxf/configuration/spring/test-beans.xml");
        ConfigurerImpl configurer = new ConfigurerImpl();
        configurer.setApplicationContext(context1);
        // Just to simulate the OSGi's uninstall command
        context1.close();
        
        ConfigurableApplicationContext context2 =
            new ClassPathXmlApplicationContext("/org/apache/cxf/configuration/spring/test-beans.xml");
        configurer.addApplicationContext(context2);
        Set<ApplicationContext> contexts = configurer.getAppContexts();
        assertEquals("The Context's size is wrong", 1, contexts.size());
        assertTrue("The conetxts' contains a wrong application context", contexts.contains(context2));
    }
    
    class SimpleBean implements Configurable {
        
        private String beanName;
        
        private String stringAttr = "hello";
        private Boolean booleanAttr = Boolean.TRUE;
        private BigInteger integerAttr = BigInteger.ONE;
        private Integer intAttr = new Integer(2);
        private Long longAttr = new Long(3);
        private Short shortAttr = new Short((short)4);
        private BigDecimal decimalAttr = new BigDecimal("5");
        private Float floatAttr = new Float(6F);
        private Double doubleAttr = new Double(7D);
        private Byte byteAttr = new Byte((byte)8);
        private QName qnameAttr = new QName("http://www.w3.org/2001/XMLSchema", "schema", "xs");
        private byte[] base64BinaryAttr = DatatypeConverter.parseBase64Binary("abcd");
        private byte[] hexBinaryAttr = new HexBinaryAdapter().unmarshal("aaaa");
        private Long unsignedIntAttr = new Long(9);
        private Integer unsignedShortAttr = new Integer(10);
        private Short unsignedByteAttr = new Short((short)11);
 
        
        public SimpleBean(String bn) {
            beanName = bn;
        }
         
        public String getBeanName() {
            return beanName;
        }

        public byte[] getBase64BinaryAttr() {
            return base64BinaryAttr;
        }

        public void setBase64BinaryAttr(byte[] base64BinaryAttr) {
            this.base64BinaryAttr = base64BinaryAttr;
        }

        public Boolean getBooleanAttr() {
            return booleanAttr;
        }

        public void setBooleanAttr(Boolean booleanAttr) {
            this.booleanAttr = booleanAttr;
        }

        public Byte getByteAttr() {
            return byteAttr;
        }

        public void setByteAttr(Byte byteAttr) {
            this.byteAttr = byteAttr;
        }

        public BigDecimal getDecimalAttr() {
            return decimalAttr;
        }

        public void setDecimalAttr(BigDecimal decimalAttr) {
            this.decimalAttr = decimalAttr;
        }

        public Double getDoubleAttr() {
            return doubleAttr;
        }

        public void setDoubleAttr(Double doubleAttr) {
            this.doubleAttr = doubleAttr;
        }

        public Float getFloatAttr() {
            return floatAttr;
        }

        public void setFloatAttr(Float floatAttr) {
            this.floatAttr = floatAttr;
        }

        public byte[] getHexBinaryAttr() {
            return hexBinaryAttr;
        }

        public void setHexBinaryAttr(byte[] hexBinaryAttr) {
            this.hexBinaryAttr = hexBinaryAttr;
        }

        public Integer getIntAttr() {
            return intAttr;
        }

        public void setIntAttr(Integer intAttr) {
            this.intAttr = intAttr;
        }

        public BigInteger getIntegerAttr() {
            return integerAttr;
        }

        public void setIntegerAttr(BigInteger integerAttr) {
            this.integerAttr = integerAttr;
        }

        public Long getLongAttr() {
            return longAttr;
        }

        public void setLongAttr(Long longAttr) {
            this.longAttr = longAttr;
        }

        public QName getQnameAttr() {
            return qnameAttr;
        }

        public void setQnameAttr(QName qnameAttr) {
            this.qnameAttr = qnameAttr;
        }

        public Short getShortAttr() {
            return shortAttr;
        }

        public void setShortAttr(Short shortAttr) {
            this.shortAttr = shortAttr;
        }

        public String getStringAttr() {
            return stringAttr;
        }

        public void setStringAttr(String stringAttr) {
            this.stringAttr = stringAttr;
        }

        public Short getUnsignedByteAttr() {
            return unsignedByteAttr;
        }

        public void setUnsignedByteAttr(Short unsignedByteAttr) {
            this.unsignedByteAttr = unsignedByteAttr;
        }

        public Long getUnsignedIntAttr() {
            return unsignedIntAttr;
        }

        public void setUnsignedIntAttr(Long unsignedIntAttr) {
            this.unsignedIntAttr = unsignedIntAttr;
        }

        public Integer getUnsignedShortAttr() {
            return unsignedShortAttr;
        }

        public void setUnsignedShortAttr(Integer unsignedShortAttr) {
            this.unsignedShortAttr = unsignedShortAttr;
        }

        public void setBeanName(String beanName) {
            this.beanName = beanName;
        }    
    }
    
    class ChildBean extends SimpleBean {

        public ChildBean(String bn) {
            super(bn);
        }
        
    }
}
