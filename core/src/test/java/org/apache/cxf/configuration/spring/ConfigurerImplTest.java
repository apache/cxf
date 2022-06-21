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

import javax.xml.namespace.QName;

import jakarta.xml.bind.DatatypeConverter;
import jakarta.xml.bind.DatatypeConverterInterface;
import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import org.apache.cxf.bus.spring.BusApplicationContext;
import org.apache.cxf.configuration.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;




public class ConfigurerImplTest {

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
                     Integer.valueOf(2), sb.getIntAttr());
        assertEquals("Unexpected value for attribute longAttr",
                     Long.valueOf(3L), sb.getLongAttr());
        assertEquals("Unexpected value for attribute shortAttr",
                     Short.valueOf((short)4), sb.getShortAttr());
        assertEquals("Unexpected value for attribute decimalAttr",
                     new BigDecimal("5"), sb.getDecimalAttr());
        assertEquals("Unexpected value for attribute floatAttr",
                     Float.valueOf(6F), sb.getFloatAttr());
        assertEquals("Unexpected value for attribute doubleAttr",
                     Double.valueOf(7.0D), sb.getDoubleAttr());
        assertEquals("Unexpected value for attribute byteAttr",
                     Byte.valueOf((byte)8), sb.getByteAttr());

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
                     Long.valueOf(9L), sb.getUnsignedIntAttr());
        assertEquals("Unexpected value for attribute unsignedShortAttrNoDefault",
                     Integer.valueOf(10), sb.getUnsignedShortAttr());
        assertEquals("Unexpected value for attribute unsignedByteAttrNoDefault",
                     Short.valueOf((short)11), sb.getUnsignedByteAttr());
    }

    @Test
    public void testConfigureSimple() {
        // Try to configure the bean with id
        verifyConfigureSimple("simple");
        // Try to configure the bean with an alias name
        verifyConfigureSimple("simpleValueBean");
    }


    public void verifyConfigureSimple(String beanName) {

        SimpleBean sb = new SimpleBean(beanName);
        BusApplicationContext ac =
            new BusApplicationContext("/org/apache/cxf/configuration/spring/test-beans.xml",
                                      false);

        ConfigurerImpl configurer = new ConfigurerImpl();
        configurer.setApplicationContext(ac);

        configurer.configureBean(sb);
        assertEquals("Unexpected value for attribute stringAttr",
                     "hallo", sb.getStringAttr());
        assertFalse("Unexpected value for attribute booleanAttr",
                   sb.getBooleanAttr());
        assertEquals("Unexpected value for attribute integerAttr",
                     BigInteger.TEN, sb.getIntegerAttr());
        assertEquals("Unexpected value for attribute intAttr",
                     Integer.valueOf(12), sb.getIntAttr());
        assertEquals("Unexpected value for attribute longAttr",
                     Long.valueOf(13L), sb.getLongAttr());
        assertEquals("Unexpected value for attribute shortAttr",
                     Short.valueOf((short)14), sb.getShortAttr());
        assertEquals("Unexpected value for attribute decimalAttr",
                     new BigDecimal("15"), sb.getDecimalAttr());
        assertEquals("Unexpected value for attribute floatAttr",
                     Float.valueOf(16F), sb.getFloatAttr());
        assertEquals("Unexpected value for attribute doubleAttr",
                     Double.valueOf(17D), sb.getDoubleAttr());
        assertEquals("Unexpected value for attribute byteAttr",
                     Byte.valueOf((byte)18), sb.getByteAttr());

        assertEquals("Unexpected value for attribute unsignedIntAttrNoDefault",
                     Long.valueOf(19L), sb.getUnsignedIntAttr());
        assertEquals("Unexpected value for attribute unsignedShortAttrNoDefault",
                     Integer.valueOf(20), sb.getUnsignedShortAttr());
        assertEquals("Unexpected value for attribute unsignedByteAttrNoDefault",
                     Short.valueOf((short)21), sb.getUnsignedByteAttr());
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
        assertFalse("Unexpected value for attribute booleanAttr",
                   sb.getBooleanAttr());
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
        assertFalse("Unexpected value for attribute booleanAttr",
                   sb.getBooleanAttr());
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
        private Integer intAttr = Integer.valueOf(2);
        private Long longAttr = Long.valueOf(3);
        private Short shortAttr = Short.valueOf((short)4);
        private BigDecimal decimalAttr = new BigDecimal("5");
        private Float floatAttr = Float.valueOf(6F);
        private Double doubleAttr = Double.valueOf(7D);
        private Byte byteAttr = Byte.valueOf((byte)8);
        private QName qnameAttr = new QName("http://www.w3.org/2001/XMLSchema", "schema", "xs");
        private byte[] base64BinaryAttr = DatatypeConverter.parseBase64Binary("abcd");
        private byte[] hexBinaryAttr = new HexBinaryAdapter().unmarshal("aaaa");
        private Long unsignedIntAttr = Long.valueOf(9);
        private Integer unsignedShortAttr = Integer.valueOf(10);
        private Short unsignedByteAttr = Short.valueOf((short)11);


        SimpleBean(String bn) {
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

        ChildBean(String bn) {
            super(bn);
        }

    }
}
