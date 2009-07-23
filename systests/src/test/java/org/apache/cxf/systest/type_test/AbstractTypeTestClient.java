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
package org.apache.cxf.systest.type_test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.datatype.DatatypeConstants;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.ws.Holder;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.testutil.common.AbstractBusClientServerTestBase;
import org.apache.type_test.doc.TypeTestPortType;
import org.apache.type_test.rpc.SOAPService;
import org.apache.type_test.types1.AnyURIEnum;
import org.apache.type_test.types1.ColourEnum;
import org.apache.type_test.types1.DecimalEnum;
import org.apache.type_test.types1.NMTokenEnum;
import org.apache.type_test.types1.NumberEnum;
import org.apache.type_test.types1.StringEnum;
import org.junit.Test;

public abstract class AbstractTypeTestClient
    extends AbstractBusClientServerTestBase implements TypeTestTester {
    protected static TypeTestPortType docClient;
    protected static org.apache.type_test.xml.TypeTestPortType xmlClient;
    protected static org.apache.type_test.rpc.TypeTestPortType rpcClient;
    protected static boolean testDocLiteral;
    protected static boolean testXMLBinding;

    protected boolean perfTestOnly;

    public void setPerformanceTestOnly() {
        perfTestOnly = true;
    }

    public boolean shouldRunTest(String name) {
        if (System.getProperty("java.vendor").contains("IBM")
            && "GMonth".equals(name)) {
            //the validator in ibm doesn't like this type.
            return false;
        }
        return true;
    }
    
    public static void initClient(Class clz, QName serviceName, QName portName, String wsdlPath)
        throws Exception {
        URL wsdlLocation = clz.getResource(wsdlPath);
        assertNotNull("Could not load wsdl " + wsdlPath, wsdlLocation);
        testDocLiteral = wsdlPath.contains("doclit") || wsdlPath.contains("-corba");
        testXMLBinding = wsdlPath.contains("_xml");
        if (testXMLBinding) {
            org.apache.type_test.xml.XMLService xmlService
                = new org.apache.type_test.xml.XMLService(wsdlLocation,
                                                           serviceName);
            xmlClient = xmlService.getPort(portName, org.apache.type_test.xml.TypeTestPortType.class);
            assertNotNull("Could not create xmlClient", xmlClient);
        } else {
            if (testDocLiteral) {
                org.apache.type_test.doc.SOAPService docService
                    = new org.apache.type_test.doc.SOAPService(wsdlLocation,
                                                                serviceName);
                docClient = docService.getPort(portName, org.apache.type_test.doc.TypeTestPortType.class);
                assertNotNull("Could not create docClient", docClient);
            } else {
                SOAPService rpcService = new SOAPService(wsdlLocation, serviceName);
                rpcClient = rpcService.getPort(portName, org.apache.type_test.rpc.TypeTestPortType.class);
                assertNotNull("Could not create rpcClient", rpcClient);
            }
        }
    }

    protected boolean equalsDate(XMLGregorianCalendar orig, XMLGregorianCalendar actual) {
        boolean result = false;

        if ((orig.getYear() == actual.getYear()) && (orig.getMonth() == actual.getMonth())
            && (orig.getDay() == actual.getDay()) && (actual.getHour() == DatatypeConstants.FIELD_UNDEFINED)
            && (actual.getMinute() == DatatypeConstants.FIELD_UNDEFINED)
            && (actual.getSecond() == DatatypeConstants.FIELD_UNDEFINED)
            && (actual.getMillisecond() == DatatypeConstants.FIELD_UNDEFINED)) {

            result = orig.getTimezone() == actual.getTimezone();
        }
        return result;
    }

    protected boolean equalsTime(XMLGregorianCalendar orig, XMLGregorianCalendar actual) {
        boolean result = false;
        if ((orig.getHour() == actual.getHour()) && (orig.getMinute() == actual.getMinute())
            && (orig.getSecond() == actual.getSecond()) && (orig.getMillisecond() == actual.getMillisecond())
            && (orig.getTimezone() == actual.getTimezone())) {
            result = true;
        }
        return result;
    }

    protected boolean equalsDateTime(XMLGregorianCalendar orig, XMLGregorianCalendar actual) {
        boolean result = false;
        if ((orig.getYear() == actual.getYear()) && (orig.getMonth() == actual.getMonth())
            && (orig.getDay() == actual.getDay()) && (orig.getHour() == actual.getHour())
            && (orig.getMinute() == actual.getMinute()) && (orig.getSecond() == actual.getSecond())
            && (orig.getMillisecond() == actual.getMillisecond())) {

            result = orig.getTimezone() == actual.getTimezone();
        }
        return result;
    }

    @Test
    public void testVoid() throws Exception {
        if (!shouldRunTest("Void")) {
            return;
        }
        if (testDocLiteral) {
            docClient.testVoid();
        } else if (testXMLBinding) {
            xmlClient.testVoid();
        } else {
            rpcClient.testVoid();
        }
    }

    @Test
    public void testOneway() throws Exception {
        if (!shouldRunTest("Oneway")) {
            return;
        }
        String x = "hello";
        String y = "oneway";
        if (testDocLiteral) {
            docClient.testOneway(x, y);
        } else if (testXMLBinding) {
            xmlClient.testOneway(x, y);
        } else {
            rpcClient.testOneway(x, y);
        }
    }

    @Test
    public void testByte() throws Exception {
        if (!shouldRunTest("Byte")) {
            return;
        }
        byte valueSets[][] = {{0, 1}, {-1, 0}, {Byte.MIN_VALUE, Byte.MAX_VALUE}};

        for (int i = 0; i < valueSets.length; i++) {
            byte x = valueSets[i][0];
            Holder<Byte> yOrig = new Holder<Byte>(valueSets[i][1]);
            Holder<Byte> y = new Holder<Byte>(valueSets[i][1]);
            Holder<Byte> z = new Holder<Byte>();

            byte ret;
            if (testDocLiteral) {
                ret = docClient.testByte(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testByte(x, y, z);
            } else {
                ret = rpcClient.testByte(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testByte(): Incorrect value for inout param", Byte.valueOf(x), y.value);
                assertEquals("testByte(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testByte(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testShort() throws Exception {
        if (!shouldRunTest("Short")) {
            return;
        }
        short valueSets[][] = {{0, 1}, {-1, 0}, {Short.MIN_VALUE, Short.MAX_VALUE}};

        for (int i = 0; i < valueSets.length; i++) {
            short x = valueSets[i][0];
            Holder<Short> yOrig = new Holder<Short>(valueSets[i][1]);
            Holder<Short> y = new Holder<Short>(valueSets[i][1]);
            Holder<Short> z = new Holder<Short>();

            short ret;
            if (testDocLiteral) {
                ret = docClient.testShort(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testShort(x, y, z);
            } else {
                ret = rpcClient.testShort(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testShort(): Incorrect value for inout param", Short.valueOf(x), y.value);
                assertEquals("testShort(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testShort(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testUnsignedShort() throws Exception {
        if (!shouldRunTest("UnsignedShort")) {
            return;
        }
        int valueSets[][] = {{0, 1}, {1, 0}, {0, Short.MAX_VALUE * 2 + 1}};

        for (int i = 0; i < valueSets.length; i++) {
            int x = valueSets[i][0];
            Holder<Integer> yOrig = new Holder<Integer>(valueSets[i][1]);
            Holder<Integer> y = new Holder<Integer>(valueSets[i][1]);
            Holder<Integer> z = new Holder<Integer>();

            int ret;
            if (testDocLiteral) {
                ret = docClient.testUnsignedShort(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testUnsignedShort(x, y, z);
            } else {
                ret = rpcClient.testUnsignedShort(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testUnsignedShort(): Incorrect value for inout param", Integer.valueOf(x),
                             y.value);
                assertEquals("testUnsignedShort(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testUnsignedShort(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testInt() throws Exception {
        if (!shouldRunTest("Int")) {
            return;
        }
        int valueSets[][] = {{5, 10}, {-10, 50}, {Integer.MIN_VALUE, Integer.MAX_VALUE}};

        for (int i = 0; i < valueSets.length; i++) {
            int x = valueSets[i][0];
            Holder<Integer> yOrig = new Holder<Integer>(valueSets[i][1]);
            Holder<Integer> y = new Holder<Integer>(valueSets[i][1]);
            Holder<Integer> z = new Holder<Integer>();

            int ret;
            if (testDocLiteral) {
                ret = docClient.testInt(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testInt(x, y, z);
            } else {
                ret = rpcClient.testInt(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testInt(): Incorrect value for inout param", Integer.valueOf(x), y.value);
                assertEquals("testInt(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testInt(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testUnsignedInt() throws Exception {
        if (!shouldRunTest("UnsignedInt")) {
            return;
        }
        long valueSets[][] = {{0, ((long)Integer.MAX_VALUE) * 2 + 1}, {11, 20}, {1, 0}};

        for (int i = 0; i < valueSets.length; i++) {
            long x = valueSets[i][0];
            long yOrig = valueSets[i][1];
            Holder<Long> y = new Holder<Long>(valueSets[i][1]);
            Holder<Long> z = new Holder<Long>();

            long ret;
            if (testDocLiteral) {
                ret = docClient.testUnsignedInt(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testUnsignedInt(x, y, z);
            } else {
                ret = rpcClient.testUnsignedInt(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testUnsignedInt(): Incorrect value for inout param",
                             Long.valueOf(x), y.value);
                assertEquals("testUnsignedInt(): Incorrect value for out param",
                             Long.valueOf(yOrig), z.value);
                assertEquals("testUnsignedInt(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testLong() throws Exception {
        if (!shouldRunTest("Long")) {
            return;
        }
        long valueSets[][] = {{0, 1}, {-1, 0}, {Long.MIN_VALUE, Long.MAX_VALUE}};

        for (int i = 0; i < valueSets.length; i++) {
            long x = valueSets[i][0];
            Holder<Long> yOrig = new Holder<Long>(valueSets[i][1]);
            Holder<Long> y = new Holder<Long>(valueSets[i][1]);
            Holder<Long> z = new Holder<Long>();

            long ret;
            if (testDocLiteral) {
                ret = docClient.testLong(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testLong(x, y, z);
            } else {
                ret = rpcClient.testLong(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testLong(): Incorrect value for inout param", Long.valueOf(x), y.value);
                assertEquals("testLong(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testLong(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testUnsignedLong() throws Exception {
        if (!shouldRunTest("UnsignedLong")) {
            return;
        }
        BigInteger valueSets[][] = {{new BigInteger("0"), new BigInteger("1")},
                                    {new BigInteger("1"), new BigInteger("0")},
                                    {new BigInteger("0"),
                                     new BigInteger(String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE))}};

        for (int i = 0; i < valueSets.length; i++) {
            BigInteger x = valueSets[i][0];
            Holder<BigInteger> yOrig = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> y = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> z = new Holder<BigInteger>();

            BigInteger ret;
            if (testDocLiteral) {
                ret = docClient.testUnsignedLong(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testUnsignedLong(x, y, z);
            } else {
                ret = rpcClient.testUnsignedLong(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testUnsignedLong(): Incorrect value for inout param", x, y.value);
                assertEquals("testUnsignedLong(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testUnsignedLong(): Incorrect return value", x, ret);
            }
        }
    }

    protected float[][] getTestFloatData() {
        return new float[][] {{0.0f, 1.0f}, {-1.0f, (float)java.lang.Math.PI}, {-100.0f, 100.0f},
                              {Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}, };
    }
    @Test
    public void testFloat() throws Exception {
        if (!shouldRunTest("Float")) {
            return;
        }
        float delta = 0.0f;
        float valueSets[][] = getTestFloatData();

        for (int i = 0; i < valueSets.length; i++) {
            float x = valueSets[i][0];
            Holder<Float> yOrig = new Holder<Float>(valueSets[i][1]);
            Holder<Float> y = new Holder<Float>(valueSets[i][1]);
            Holder<Float> z = new Holder<Float>();

            float ret;
            if (testDocLiteral) {
                ret = docClient.testFloat(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testFloat(x, y, z);
            } else {
                ret = rpcClient.testFloat(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals(i + ": testFloat(): Wrong value for inout param", x, y.value, delta);
                assertEquals(i + ": testFloat(): Wrong value for out param", yOrig.value, z.value, delta);
                assertEquals(i + ": testFloat(): Wrong return value", x, ret, delta);
            }
        }

        float x = Float.NaN;
        Holder<Float> yOrig = new Holder<Float>(0.0f);
        Holder<Float> y = new Holder<Float>(0.0f);
        Holder<Float> z = new Holder<Float>();
        float ret;
        if (testDocLiteral) {
            ret = docClient.testFloat(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testFloat(x, y, z);
        } else {
            ret = rpcClient.testFloat(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testFloat(): Incorrect value for inout param", Float.isNaN(y.value));
            assertEquals("testFloat(): Incorrect value for out param", yOrig.value, z.value, delta);
            assertTrue("testFloat(): Incorrect return value", Float.isNaN(ret));
        }
    }
    protected double[][] getTestDoubleData() {
        return new double[][] {{0.0f, 1.0f}, {-1, java.lang.Math.PI}, {-100.0, 100.0},
                               {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}};
    }

    @Test
    public void testDouble() throws Exception {
        if (!shouldRunTest("Double")) {
            return;
        }
        double delta = 0.0d;
        double valueSets[][] = getTestDoubleData();
        for (int i = 0; i < valueSets.length; i++) {
            double x = valueSets[i][0];
            Holder<Double> yOrig = new Holder<Double>(valueSets[i][1]);
            Holder<Double> y = new Holder<Double>(valueSets[i][1]);
            Holder<Double> z = new Holder<Double>();

            double ret;
            if (testDocLiteral) {
                ret = docClient.testDouble(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testDouble(x, y, z);
            } else {
                ret = rpcClient.testDouble(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testDouble(): Incorrect value for inout param", x, y.value, delta);
                assertEquals("testDouble(): Incorrect value for out param", yOrig.value, z.value, delta);
                assertEquals("testDouble(): Incorrect return value", x, ret, delta);
            }
        }

        double x = Double.NaN;
        Holder<Double> yOrig = new Holder<Double>(0.0);
        Holder<Double> y = new Holder<Double>(0.0);
        Holder<Double> z = new Holder<Double>();
        double ret;
        if (testDocLiteral) {
            ret = docClient.testDouble(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDouble(x, y, z);
        } else {
            ret = rpcClient.testDouble(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDouble(): Incorrect value for inout param", Double.isNaN(y.value));
            assertEquals("testDouble(): Incorrect value for out param", yOrig.value, z.value, delta);
            assertTrue("testDouble(): Incorrect return value", Double.isNaN(ret));
        }
    }

    @Test
    public void testUnsignedByte() throws Exception {
        if (!shouldRunTest("UnsignedByte")) {
            return;
        }
        short valueSets[][] = {{0, 1}, {1, 0}, {0, Byte.MAX_VALUE * 2 + 1}};

        for (int i = 0; i < valueSets.length; i++) {
            short x = valueSets[i][0];
            Holder<Short> yOrig = new Holder<Short>(valueSets[i][1]);
            Holder<Short> y = new Holder<Short>(valueSets[i][1]);
            Holder<Short> z = new Holder<Short>();

            short ret;
            if (testDocLiteral) {
                ret = docClient.testUnsignedByte(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testUnsignedByte(x, y, z);
            } else {
                ret = rpcClient.testUnsignedByte(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testUnsignedByte(): Incorrect value for inout param",
                             Short.valueOf(x), y.value);
                assertEquals("testUnsignedByte(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testUnsignedByte(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testBoolean() throws Exception {
        if (!shouldRunTest("Boolean")) {
            return;
        }
        boolean valueSets[][] = {{true, false}, {true, true}, {false, true}, {false, false}};

        for (int i = 0; i < valueSets.length; i++) {
            boolean x = valueSets[i][0];
            Holder<Boolean> yOrig = new Holder<Boolean>(valueSets[i][1]);
            Holder<Boolean> y = new Holder<Boolean>(valueSets[i][1]);
            Holder<Boolean> z = new Holder<Boolean>();

            boolean ret;
            if (testDocLiteral) {
                ret = docClient.testBoolean(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testBoolean(x, y, z);
            } else {
                ret = rpcClient.testBoolean(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testBoolean(): Incorrect value for inout param", Boolean.valueOf(x), y.value);
                assertEquals("testBoolean(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testBoolean(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testString() throws Exception {
        if (!shouldRunTest("String")) {
            return;
        }
        int bufferSize = 1000;
        StringBuffer buffer = new StringBuffer(bufferSize);
        StringBuffer buffer2 = new StringBuffer(bufferSize);
        for (int x = 0; x < bufferSize; x++) {
            buffer.append((char)('a' + (x % 26)));
            buffer2.append((char)('A' + (x % 26)));
        }

        String valueSets[][] = {{"hello", "world"},
                                {"is pi > 3 ?", " is pi < 4\\\""},
                                {"<illegal_tag/>", ""},
                                {buffer.toString(), buffer2.toString()},
                                {"jon&marry", "marry&john"}};

        for (int i = 0; i < valueSets.length; i++) {
            String x = valueSets[i][0];
            Holder<String> yOrig = new Holder<String>(valueSets[i][1]);
            Holder<String> y = new Holder<String>(valueSets[i][1]);
            Holder<String> z = new Holder<String>();

            String ret;
            if (testDocLiteral) {
                ret = docClient.testString(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testString(x, y, z);
            } else {
                ret = rpcClient.testString(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testString(): Incorrect value for inout param", x, y.value);
                assertEquals("testString(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testString(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testStringI18N() throws Exception {
        if (!shouldRunTest("StringI18N")) {
            return;
        }
        String valueSets[][] = {{"hello", I18NStrings.CHINESE_COMPLEX_STRING},
                                {"hello", I18NStrings.JAP_SIMPLE_STRING}, };

        for (int i = 0; i < valueSets.length; i++) {
            String x = valueSets[i][0];
            Holder<String> yOrig = new Holder<String>(valueSets[i][1]);
            Holder<String> y = new Holder<String>(valueSets[i][1]);
            Holder<String> z = new Holder<String>();

            String ret;
            if (testDocLiteral) {
                ret = docClient.testString(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testString(x, y, z);
            } else {
                ret = rpcClient.testString(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testStringI18N(): Incorrect value for inout param", x, y.value);
                assertEquals("testStringI18N(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testStringI18N(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testQName() throws Exception {
        if (!shouldRunTest("QName")) {
            return;
        }
        String valueSets[][] = {{"NoNamespaceService", ""},
                                {"HelloWorldService", "http://www.iona.com/services"},
                                {I18NStrings.JAP_SIMPLE_STRING, "http://www.iona.com/iona"},
                                {"MyService", "http://www.iona.com/iona"}};
        for (int i = 0; i < valueSets.length; i++) {
            QName x = new QName(valueSets[i][1], valueSets[i][0]);
            QName yOrig = new QName("http://www.iona.com/inoutqname", "InOutQName");
            Holder<QName> y = new Holder<QName>(yOrig);
            Holder<QName> z = new Holder<QName>();

            QName ret;
            if (testDocLiteral) {
                ret = docClient.testQName(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testQName(x, y, z);
            } else {
                ret = rpcClient.testQName(x, y, z);
            }

            if (!perfTestOnly) {
                assertEquals("testQName(): Incorrect value for inout param", x, y.value);
                assertEquals("testQName(): Incorrect value for out param", yOrig, z.value);
                assertEquals("testQName(): Incorrect return value", x, ret);
            }
        }
    }

    // Revisit When client Fault is ready. Comment should be removed
    @Test
    public void testDate() throws Exception {
        if (!shouldRunTest("Date")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x = datatypeFactory.newXMLGregorianCalendar();
        x.setYear(1975);
        x.setMonth(5);
        x.setDay(5);
        XMLGregorianCalendar yOrig = datatypeFactory.newXMLGregorianCalendar();
        yOrig.setYear(2004);
        yOrig.setMonth(4);
        yOrig.setDay(1);

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testDate(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDate(x, y, z);
        } else {
            ret = rpcClient.testDate(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDate(): Incorrect value for inout param " + x + " != " + y.value,
                       equalsDate(x, y.value));
            assertTrue("testDate(): Incorrect value for out param", equalsDate(yOrig, z.value));
            assertTrue("testDate(): Incorrect return value", equalsDate(x, ret));
        }

        x = datatypeFactory.newXMLGregorianCalendar();
        yOrig = datatypeFactory.newXMLGregorianCalendar();

        y = new Holder<XMLGregorianCalendar>(yOrig);
        z = new Holder<XMLGregorianCalendar>();

        try {
            if (testDocLiteral) {
                ret = docClient.testDate(x, y, z);
            } else {
                ret = rpcClient.testDate(x, y, z);
            }
            fail("Expected to catch WebServiceException when calling"
                 + " testDate() with uninitialized parameters.");
        } catch (RuntimeException re) {
            assertNotNull(re);
        }
    }

    @Test
    public void testDateTime() throws Exception {
        if (!shouldRunTest("DateTime")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x = datatypeFactory.newXMLGregorianCalendar();
        x.setYear(1975);
        x.setMonth(5);
        x.setDay(5);
        x.setHour(12);
        x.setMinute(30);
        x.setSecond(15);
        XMLGregorianCalendar yOrig = datatypeFactory.newXMLGregorianCalendar();
        yOrig.setYear(2005);
        yOrig.setMonth(4);
        yOrig.setDay(1);
        yOrig.setHour(17);
        yOrig.setMinute(59);
        yOrig.setSecond(30);

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testDateTime(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDateTime(x, y, z);
        } else {
            ret = rpcClient.testDateTime(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDateTime(): Incorrect value for inout param", equalsDateTime(x, y.value));
            assertTrue("testDateTime(): Incorrect value for out param", equalsDateTime(yOrig, z.value));
            assertTrue("testDateTime(): Incorrect return value", equalsDateTime(x, ret));
        }
    }

    @Test
    public void testTime() throws Exception {
        if (!shouldRunTest("Time")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x = datatypeFactory.newXMLGregorianCalendar();
        x.setHour(12);
        x.setMinute(14);
        x.setSecond(5);
        XMLGregorianCalendar yOrig = datatypeFactory.newXMLGregorianCalendar();
        yOrig.setHour(22);
        yOrig.setMinute(4);
        yOrig.setSecond(15);
        yOrig.setMillisecond(250);

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testTime(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testTime(x, y, z);
        } else {
            ret = rpcClient.testTime(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testTime(): Incorrect value for inout param", equalsTime(x, y.value));
            assertTrue("testTime(): Incorrect value for out param", equalsTime(yOrig, z.value));
            assertTrue("testTime(): Incorrect return value", equalsTime(x, ret));
        }
    }

    @Test
    public void testGYear() throws Exception {
        if (!shouldRunTest("GYear")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x = datatypeFactory.newXMLGregorianCalendar("2004");
        XMLGregorianCalendar yOrig = datatypeFactory.newXMLGregorianCalendar("2003+05:00");

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testGYear(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testGYear(x, y, z);
        } else {
            ret = rpcClient.testGYear(x, y, z);
        }
        assertTrue("testGYear(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testGYear(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testGYear(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testGYearMonth() throws Exception {
        if (!shouldRunTest("GYearMonth")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x = datatypeFactory.newXMLGregorianCalendar("2004-08");
        XMLGregorianCalendar yOrig = datatypeFactory.newXMLGregorianCalendar("2003-12+05:00");

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testGYearMonth(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testGYearMonth(x, y, z);
        } else {
            ret = rpcClient.testGYearMonth(x, y, z);
        }
        assertTrue("testGYearMonth(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testGYearMonth(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testGYearMonth(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testGMonth() throws Exception {
        if (!shouldRunTest("GMonth")) { 
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x;
        XMLGregorianCalendar yOrig;

        try {
            x = datatypeFactory.newXMLGregorianCalendar("--08");
            yOrig = datatypeFactory.newXMLGregorianCalendar("--12+05:00");
        } catch (java.lang.IllegalArgumentException iae) {
            // broken XMLGregorianCalendar impl
            x = datatypeFactory.newXMLGregorianCalendar("--08--");
            yOrig = datatypeFactory.newXMLGregorianCalendar("--12--+05:00");
        }

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testGMonth(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testGMonth(x, y, z);
        } else {
            ret = rpcClient.testGMonth(x, y, z);
        }
        assertTrue("testGMonth(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testGMonth(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testGMonth(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testGMonthDay() throws Exception {
        if (!shouldRunTest("GMonthDay")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x = datatypeFactory.newXMLGregorianCalendar("--08-21");
        XMLGregorianCalendar yOrig = datatypeFactory.newXMLGregorianCalendar("--12-05+05:00");

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testGMonthDay(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testGMonthDay(x, y, z);
        } else {
            ret = rpcClient.testGMonthDay(x, y, z);
        }
        assertTrue("testGMonthDay(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testGMonthDay(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testGMonthDay(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testGDay() throws Exception {
        if (!shouldRunTest("GDay")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        XMLGregorianCalendar x = datatypeFactory.newXMLGregorianCalendar("---21");
        XMLGregorianCalendar yOrig = datatypeFactory.newXMLGregorianCalendar("---05+05:00");

        Holder<XMLGregorianCalendar> y = new Holder<XMLGregorianCalendar>(yOrig);
        Holder<XMLGregorianCalendar> z = new Holder<XMLGregorianCalendar>();

        XMLGregorianCalendar ret;
        if (testDocLiteral) {
            ret = docClient.testGDay(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testGDay(x, y, z);
        } else {
            ret = rpcClient.testGDay(x, y, z);
        }
        assertTrue("testGDay(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testGDay(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testGDay(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testDuration() throws Exception {
        if (!shouldRunTest("Duration")) {
            return;
        }
        javax.xml.datatype.DatatypeFactory datatypeFactory = javax.xml.datatype.DatatypeFactory.newInstance();

        Duration x = datatypeFactory.newDuration("P1Y35DT60M60.500S");
        Duration yOrig = datatypeFactory.newDuration("-P2MT24H60S");

        Holder<Duration> y = new Holder<Duration>(yOrig);
        Holder<Duration> z = new Holder<Duration>();

        Duration ret;
        if (testDocLiteral) {
            ret = docClient.testDuration(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDuration(x, y, z);
        } else {
            ret = rpcClient.testDuration(x, y, z);
        }
        assertTrue("testDuration(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testDuration(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testDuration(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testNormalizedString() throws Exception {
        if (!shouldRunTest("NormalizedString")) {
            return;
        }
        String x = "  normalized string ";
        String yOrig = "  another normalized  string ";

        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testNormalizedString(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testNormalizedString(x, y, z);
        } else {
            ret = rpcClient.testNormalizedString(x, y, z);
        }
        assertTrue("testNormalizedString(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testNormalizedString(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testNormalizedString(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testToken() throws Exception {
        if (!shouldRunTest("Token")) {
            return;
        }
        String x = "token";
        String yOrig = "another token";

        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testToken(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testToken(x, y, z);
        } else {
            ret = rpcClient.testToken(x, y, z);
        }
        assertTrue("testToken(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testToken(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testToken(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testLanguage() throws Exception {
        if (!shouldRunTest("Language")) {
            return;
        }
        String x = "abc";
        String yOrig = "abc-def";

        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testLanguage(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testLanguage(x, y, z);
        } else {
            ret = rpcClient.testLanguage(x, y, z);
        }
        assertTrue("testLanguage(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testLanguage(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testLanguage(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testNMTOKEN() throws Exception {
        if (!shouldRunTest("NMTOKEN")) {
            return;
        }
        String x = "123:abc";
        String yOrig = "abc.-_:";

        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testNMTOKEN(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testNMTOKEN(x, y, z);
        } else {
            ret = rpcClient.testNMTOKEN(x, y, z);
        }
        assertTrue("testNMTOKEN(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testNMTOKEN(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testNMTOKEN(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testNMTOKENS() throws Exception {
        if (!shouldRunTest("NMTOKENS")) {
            return;
        }
        //
        // XXX - The jaxb ri code generation produces different method
        // signatures for the NMTOKENS type between using rpc literal
        // and doc literal styles.
        //
        if (testDocLiteral) {
            List<String> x = Arrays.asList("123:abc");
            List<String> yOrig = Arrays.asList("abc.-_:", "a");
            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();
            List<String> ret = docClient.testNMTOKENS(x, y, z);
            assertTrue("testNMTOKENS(): Incorrect value for inout param", x.equals(y.value));
            assertTrue("testNMTOKENS(): Incorrect value for out param", yOrig.equals(z.value));
            assertTrue("testNMTOKENS(): Incorrect return value", x.equals(ret));
        } else if (testXMLBinding) {
            List<String> x = Arrays.asList("123:abc");
            List<String> yOrig = Arrays.asList("abc.-_:", "a");
            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();
            List<String> ret = xmlClient.testNMTOKENS(x, y, z);
            assertTrue("testNMTOKENS(): Incorrect value for inout param", x.equals(y.value));
            assertTrue("testNMTOKENS(): Incorrect value for out param", yOrig.equals(z.value));
            assertTrue("testNMTOKENS(): Incorrect return value", x.equals(ret));
        } else {
            String[] x = new String[1];
            x[0] = "123:abc";
            String[] yOrig = new String[2];
            yOrig[0] = "abc.-_:";
            yOrig[1] = "a";

            Holder<String[]> y = new Holder<String[]>(yOrig);
            Holder<String[]> z = new Holder<String[]>();

            String[] ret = rpcClient.testNMTOKENS(x, y, z);
            assertTrue("testNMTOKENS(): Incorrect value for inout param", Arrays.equals(x, y.value));
            assertTrue("testNMTOKENS(): Incorrect value for out param", Arrays.equals(yOrig, z.value));
            assertTrue("testNMTOKENS(): Incorrect return value", Arrays.equals(x, ret));
        }
    }

    @Test
    public void testName() throws Exception {
        if (!shouldRunTest("Name")) {
            return;
        }
        String x = "abc:123";
        String yOrig = "abc.-_";

        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testName(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testName(x, y, z);
        } else {
            ret = rpcClient.testName(x, y, z);
        }
        assertTrue("testName(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testName(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testName(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testNCName() throws Exception {
        if (!shouldRunTest("NCName")) {
            return;
        }
        String x = "abc-123";
        String yOrig = "abc.-";

        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testNCName(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testNCName(x, y, z);
        } else {
            ret = rpcClient.testNCName(x, y, z);
        }
        assertTrue("testNCName(): Incorrect value for inout param", x.equals(y.value));
        assertTrue("testNCName(): Incorrect value for out param", yOrig.equals(z.value));
        assertTrue("testNCName(): Incorrect return value", x.equals(ret));
    }

    @Test
    public void testID() throws Exception {
        if (!shouldRunTest("ID")) {
            return;
        }
        // n.b. to be valid, elements with an ID in the response message
        // must have a unique ID, so this test does not return x as the
        // return value (like the other tests).
        String valueSets[][] = {{"root.id-testartix.2", "L.-type_test"}, {"_iona.com", "zoo-5_wolf"},
                                {"x-_liberty", "_-.-_"}};

        for (int i = 0; i < valueSets.length; i++) {
            String x = valueSets[i][0];
            String yOrig = valueSets[i][1];
            Holder<String> y = new Holder<String>(yOrig);
            Holder<String> z = new Holder<String>();

            if (testDocLiteral) {
                /* String ret = */docClient.testID(x, y, z);
            } else if (testXMLBinding) {
                /* String ret = */xmlClient.testID(x, y, z);
            } else {
                /* String ret = */rpcClient.testID(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testID(): Incorrect value for inout param", x, y.value);
                assertEquals("testID(): Incorrect value for out param", yOrig, z.value);
            }
        }
    }

    @Test
    public void testDecimal() throws Exception {
        if (!shouldRunTest("Decimal")) {
            return;
        }
        BigDecimal valueSets[][] = {{new BigDecimal("-1234567890.000000"),
                                     new BigDecimal("1234567890.000000")},
                                    {new BigDecimal("-" + String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE)
                                                    + ".000000"),
                                     new BigDecimal(String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE)
                                                    + ".000000")}};

        for (int i = 0; i < valueSets.length; i++) {
            BigDecimal x = valueSets[i][0];
            Holder<BigDecimal> yOrig = new Holder<BigDecimal>(valueSets[i][1]);
            Holder<BigDecimal> y = new Holder<BigDecimal>(valueSets[i][1]);
            Holder<BigDecimal> z = new Holder<BigDecimal>();

            BigDecimal ret;
            if (testDocLiteral) {
                ret = docClient.testDecimal(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testDecimal(x, y, z);
            } else {
                ret = rpcClient.testDecimal(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testDecimal(): Incorrect value for inout param", x, y.value);
                assertEquals("testDecimal(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testDecimal(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testInteger() throws Exception {
        if (!shouldRunTest("Integer")) {
            return;
        }
        BigInteger valueSets[][] = {{new BigInteger("-1234567890"), new BigInteger("1234567890")},
                                    {new BigInteger("-" + String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE)),
                                     new BigInteger(String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE))}};

        for (int i = 0; i < valueSets.length; i++) {
            BigInteger x = valueSets[i][0];
            Holder<BigInteger> yOrig = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> y = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> z = new Holder<BigInteger>();

            BigInteger ret;
            if (testDocLiteral) {
                ret = docClient.testInteger(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testInteger(x, y, z);
            } else {
                ret = rpcClient.testInteger(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testInteger(): Incorrect value for inout param", x, y.value);
                assertEquals("testInteger(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testInteger(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testPositiveInteger() throws Exception {
        if (!shouldRunTest("PositiveInteger")) {
            return;
        }
        BigInteger valueSets[][] = {{new BigInteger("1"), new BigInteger("1234567890")},
                                    {new BigInteger(String.valueOf(Integer.MAX_VALUE * Integer.MAX_VALUE)),
                                     new BigInteger(String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE))}};

        for (int i = 0; i < valueSets.length; i++) {
            BigInteger x = valueSets[i][0];
            Holder<BigInteger> yOrig = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> y = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> z = new Holder<BigInteger>();

            BigInteger ret;
            if (testDocLiteral) {
                ret = docClient.testPositiveInteger(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testPositiveInteger(x, y, z);
            } else {
                ret = rpcClient.testPositiveInteger(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testPositiveInteger(): Incorrect value for inout param", x, y.value);
                assertEquals("testPositiveInteger(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testPositiveInteger(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testNonPositiveInteger() throws Exception {
        if (!shouldRunTest("NonPositiveInteger")) {
            return;
        }
        BigInteger valueSets[][] = {{new BigInteger("0"), new BigInteger("-1234567890")},
                                    {new BigInteger("-"
                                                    + String.valueOf(Integer.MAX_VALUE * Integer.MAX_VALUE)),
                                     new BigInteger("-" + String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE))}};

        for (int i = 0; i < valueSets.length; i++) {
            BigInteger x = valueSets[i][0];
            Holder<BigInteger> yOrig = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> y = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> z = new Holder<BigInteger>();

            BigInteger ret;
            if (testDocLiteral) {
                ret = docClient.testNonPositiveInteger(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testNonPositiveInteger(x, y, z);
            } else {
                ret = rpcClient.testNonPositiveInteger(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testNonPositiveInteger(): Incorrect value for inout param", x, y.value);
                assertEquals("testNonPositiveInteger(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testNonPositiveInteger(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testNegativeInteger() throws Exception {
        if (!shouldRunTest("NegativeInteger")) {
            return;
        }
        BigInteger valueSets[][] = {{new BigInteger("-1"), new BigInteger("-1234567890")},
                                    {new BigInteger("-"
                                                    + String.valueOf(Integer.MAX_VALUE * Integer.MAX_VALUE)),
                                     new BigInteger("-" + String.valueOf(Long.MAX_VALUE * Long.MAX_VALUE))}};

        for (int i = 0; i < valueSets.length; i++) {
            BigInteger x = valueSets[i][0];
            Holder<BigInteger> yOrig = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> y = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> z = new Holder<BigInteger>();

            BigInteger ret;
            if (testDocLiteral) {
                ret = docClient.testNegativeInteger(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testNegativeInteger(x, y, z);
            } else {
                ret = rpcClient.testNegativeInteger(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testNegativeInteger(): Incorrect value for inout param", x, y.value);
                assertEquals("testNegativeInteger(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testNegativeInteger(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testNonNegativeInteger() throws Exception {
        if (!shouldRunTest("NonNegativeInteger")) {
            return;
        }
        BigInteger valueSets[][] = {{new BigInteger("0"),
                                        new BigInteger("1234567890")},
                                    {new BigInteger(String.valueOf(Integer.MAX_VALUE
                                                                   * Integer.MAX_VALUE)),
                                     new BigInteger(String.valueOf(Long.MAX_VALUE
                                                                   * Long.MAX_VALUE))}};

        for (int i = 0; i < valueSets.length; i++) {
            BigInteger x = valueSets[i][0];
            Holder<BigInteger> yOrig = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> y = new Holder<BigInteger>(valueSets[i][1]);
            Holder<BigInteger> z = new Holder<BigInteger>();

            BigInteger ret;
            if (testDocLiteral) {
                ret = docClient.testNonNegativeInteger(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testNonNegativeInteger(x, y, z);
            } else {
                ret = rpcClient.testNonNegativeInteger(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testNonNegativeInteger(): Incorrect value for inout param", x, y.value);
                assertEquals("testNonNegativeInteger(): Incorrect value for out param", yOrig.value, z.value);
                assertEquals("testNonNegativeInteger(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testHexBinary() throws Exception {
        if (!shouldRunTest("HexBinary")) {
            return;
        }
        byte[] x = "hello".getBytes();
        Holder<byte[]> y = new Holder<byte[]>("goodbye".getBytes());
        Holder<byte[]> yOriginal = new Holder<byte[]>("goodbye".getBytes());
        Holder<byte[]> z = new Holder<byte[]>();
        byte[] ret;
        if (testDocLiteral) {
            ret = docClient.testHexBinary(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testHexBinary(x, y, z);
        } else {
            ret = rpcClient.testHexBinary(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testHexBinary(): Incorrect value for inout param", Arrays.equals(x, y.value));
            assertTrue("testHexBinary(): Incorrect value for out param", Arrays.equals(yOriginal.value,
                                                                                       z.value));
            assertTrue("testHexBinary(): Incorrect return value", Arrays.equals(x, ret));
        }
    }

    @Test
    public void testBase64Binary() throws Exception {
        if (!shouldRunTest("Base64Binary")) {
            return;
        }
        byte[] x = "hello".getBytes();
        Holder<byte[]> y = new Holder<byte[]>("goodbye".getBytes());
        Holder<byte[]> yOriginal = new Holder<byte[]>("goodbye".getBytes());
        Holder<byte[]> z = new Holder<byte[]>();
        byte[] ret;
        if (testDocLiteral) {
            ret = docClient.testBase64Binary(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testBase64Binary(x, y, z);
        } else {
            ret = rpcClient.testBase64Binary(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testBase64Binary(): Incorrect value for inout param", Arrays.equals(x, y.value));
            assertTrue("testBase64Binary(): Incorrect value for out param", Arrays.equals(yOriginal.value,
                                                                                          z.value));
            assertTrue("testBase64Binary(): Incorrect return value", Arrays.equals(x, ret));
        }

        // Test uninitialized holder value
        try {
            y = new Holder<byte[]>();
            z = new Holder<byte[]>();
            if (testDocLiteral) {
                docClient.testBase64Binary(x, y, z);
            } else if (testXMLBinding) {
                xmlClient.testBase64Binary(x, y, z);
            } else {
                rpcClient.testBase64Binary(x, y, z);
            }
            fail("Uninitialized Holder for inout parameter should have thrown an error.");
        } catch (Exception e) {
            // Ignore expected //failure.
        }
    }

    @Test
    public void testAnyURI() throws Exception {
        if (!shouldRunTest("AnyURI")) {
            return;
        }
        String valueSets[][] = {{"file:///root%20%20/-;?&+",
                                    "file:///w:/test!artix~java*"},
                                {"http://iona.com/",
                                    "file:///z:/mail_iona=com,\'xmlbus\'"},
                                {"mailto:windows@systems", "file:///"}};

        for (int i = 0; i < valueSets.length; i++) {
            String x = new String(valueSets[i][0]);
            String yOrig = new String(valueSets[i][1]);
            Holder<String> y = new Holder<String>(yOrig);
            Holder<String> z = new Holder<String>();

            String ret;
            if (testDocLiteral) {
                ret = docClient.testAnyURI(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testAnyURI(x, y, z);
            } else {
                ret = rpcClient.testAnyURI(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testAnyURI(): Incorrect value for inout param", x, y.value);
                assertEquals("testAnyURI(): Incorrect value for out param", yOrig, z.value);
                assertEquals("testAnyURI(): Incorrect return value", x, ret);
            }
        }
    }

    @Test
    public void testColourEnum() throws Exception {
        if (!shouldRunTest("ColourEnum")) {
            return;
        }
        String[] xx = {"RED", "GREEN", "BLUE"};
        String[] yy = {"GREEN", "BLUE", "RED"};

        Holder<ColourEnum> z = new Holder<ColourEnum>();

        for (int i = 0; i < 3; i++) {
            ColourEnum x = ColourEnum.fromValue(xx[i]);
            ColourEnum yOrig = ColourEnum.fromValue(yy[i]);
            Holder<ColourEnum> y = new Holder<ColourEnum>(yOrig);

            ColourEnum ret;
            if (testDocLiteral) {
                ret = docClient.testColourEnum(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testColourEnum(x, y, z);
            } else {
                ret = rpcClient.testColourEnum(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testColourEnum(): Incorrect value for inout param", x.value(), y.value.value());
                assertEquals("testColourEnum(): Incorrect value for out param", yOrig.value(), z.value
                    .value());
                assertEquals("testColourEnum(): Incorrect return value", x.value(), ret.value());
            }
        }
    }

    @Test
    public void testNumberEnum() throws Exception {
        if (!shouldRunTest("NumberEnum")) {
            return;
        }
        int[] xx = {1, 2, 3};
        int[] yy = {3, 1, 2};

        Holder<NumberEnum> z = new Holder<NumberEnum>();

        for (int i = 0; i < 3; i++) {
            NumberEnum x = NumberEnum.fromValue(xx[i]);
            NumberEnum yOrig = NumberEnum.fromValue(yy[i]);
            Holder<NumberEnum> y = new Holder<NumberEnum>(yOrig);

            NumberEnum ret;
            if (testDocLiteral) {
                ret = docClient.testNumberEnum(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testNumberEnum(x, y, z);
            } else {
                ret = rpcClient.testNumberEnum(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testNumberEnum(): Incorrect value for inout param", x.value(), y.value.value());
                assertEquals("testNumberEnum(): Incorrect value for out param", yOrig.value(), z.value
                    .value());
                assertEquals("testNumberEnum(): Incorrect return value", x.value(), ret.value());
            }
        }
    }

    @Test
    public void testStringEnum() throws Exception {
        if (!shouldRunTest("StringEnum")) {
            return;
        }
        String[] xx = {"a b c", "d e f", "g h i"};
        String[] yy = {"g h i", "a b c", "d e f"};

        Holder<StringEnum> z = new Holder<StringEnum>();
        for (int i = 0; i < 3; i++) {
            StringEnum x = StringEnum.fromValue(xx[i]);
            StringEnum yOrig = StringEnum.fromValue(yy[i]);
            Holder<StringEnum> y = new Holder<StringEnum>(yOrig);

            StringEnum ret;
            if (testDocLiteral) {
                ret = docClient.testStringEnum(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testStringEnum(x, y, z);
            } else {
                ret = rpcClient.testStringEnum(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testStringEnum(): Incorrect value for inout param", x.value(), y.value.value());
                assertEquals("testStringEnum(): Incorrect value for out param", yOrig.value(), z.value
                    .value());
                assertEquals("testStringEnum(): Incorrect return value", x.value(), ret.value());
            }
        }
    }

    @Test
    public void testDecimalEnum() throws Exception {
        if (!shouldRunTest("DecimalEnum")) {
            return;
        }
        BigDecimal[] xx = {new BigDecimal("-10.34"), new BigDecimal("11.22"), new BigDecimal("14.55")};
        BigDecimal[] yy = {new BigDecimal("14.55"), new BigDecimal("-10.34"), new BigDecimal("11.22")};

        Holder<DecimalEnum> z = new Holder<DecimalEnum>();

        for (int i = 0; i < 3; i++) {
            DecimalEnum x = DecimalEnum.fromValue(xx[i]);
            DecimalEnum yOrig = DecimalEnum.fromValue(yy[i]);
            Holder<DecimalEnum> y = new Holder<DecimalEnum>(yOrig);

            DecimalEnum ret;
            if (testDocLiteral) {
                ret = docClient.testDecimalEnum(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testDecimalEnum(x, y, z);
            } else {
                ret = rpcClient.testDecimalEnum(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testDecimalEnum(): Incorrect value for inout param",
                             x.value(), y.value.value());
                assertEquals("testDecimalEnum(): Incorrect value for out param", yOrig.value(), z.value
                    .value());
                assertEquals("testDecimalEnum(): Incorrect return value", x.value(), ret.value());
            }
        }
    }

    @Test
    public void testNMTokenEnum() throws Exception {
        if (!shouldRunTest("NMTokenEnum")) {
            return;
        }
        String[] xx = {"hello", "there"};
        String[] yy = {"there", "hello"};

        Holder<NMTokenEnum> z = new Holder<NMTokenEnum>();

        for (int i = 0; i < 2; i++) {
            NMTokenEnum x = NMTokenEnum.fromValue(xx[i]);
            NMTokenEnum yOrig = NMTokenEnum.fromValue(yy[i]);
            Holder<NMTokenEnum> y = new Holder<NMTokenEnum>(yOrig);

            NMTokenEnum ret;
            if (testDocLiteral) {
                ret = docClient.testNMTokenEnum(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testNMTokenEnum(x, y, z);
            } else {
                ret = rpcClient.testNMTokenEnum(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testNMTokenEnum(): Incorrect value for inout param",
                             x.value(), y.value.value());
                assertEquals("testNMTokenEnum(): Incorrect value for out param", yOrig.value(), z.value
                    .value());
                assertEquals("testNMTokenEnum(): Incorrect return value", x.value(), ret.value());
            }
        }
    }

    @Test
    public void testAnyURIEnum() throws Exception {
        if (!shouldRunTest("AnyURIEnum")) {
            return;
        }
        String[] xx = {"http://www.iona.com", "http://www.google.com"};
        String[] yy = {"http://www.google.com", "http://www.iona.com"};

        Holder<AnyURIEnum> z = new Holder<AnyURIEnum>();
        for (int i = 0; i < 2; i++) {
            AnyURIEnum x = AnyURIEnum.fromValue(xx[i]);
            AnyURIEnum yOrig = AnyURIEnum.fromValue(yy[i]);
            Holder<AnyURIEnum> y = new Holder<AnyURIEnum>(yOrig);

            AnyURIEnum ret;
            if (testDocLiteral) {
                ret = docClient.testAnyURIEnum(x, y, z);
            } else if (testXMLBinding) {
                ret = xmlClient.testAnyURIEnum(x, y, z);
            } else {
                ret = rpcClient.testAnyURIEnum(x, y, z);
            }
            if (!perfTestOnly) {
                assertEquals("testAnyURIEnum(): Incorrect value for inout param", x.value(), y.value.value());
                assertEquals("testAnyURIEnum(): Incorrect value for out param", yOrig.value(), z.value
                    .value());
                assertEquals("testAnyURIEnum(): Incorrect return value", x.value(), ret.value());
            }
        }
    }

    @Test
    public void testSimpleRestriction() throws Exception {
        if (!shouldRunTest("SimpleRestriction")) {
            return;
        }
        // normal case, maxLength=10
        String x = "string_x";
        String yOrig = "string_y";
        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();
        String ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleRestriction(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleRestriction(x, y, z);
        } else {
            ret = rpcClient.testSimpleRestriction(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testSimpleRestriction(): Incorrect value for inout param", x, y.value);
            assertEquals("testSimpleRestriction(): Incorrect value for out param", yOrig, z.value);
            assertEquals("testSimpleRestriction(): Incorrect return value", x, ret);
        }

        // Enabled schema validation for doc literal tests
        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            x = "string_xxxxx";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = docClient.testSimpleRestriction(x, y, z);
                fail("x parameter maxLength=10 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }

            // abnormal case
            x = "string_x";
            yOrig = "string_yyyyyy";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = testDocLiteral ? docClient.testSimpleRestriction(x, y, z) : xmlClient
                    .testSimpleRestriction(x, y, z);
                fail("y parameter maxLength=10 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    @Test
    public void testSimpleRestriction2() throws Exception {
        if (!shouldRunTest("SimpleRestriction2")) {
            return;
        }
        // normal case, minLength=5
        String x = "str_x";
        String yOrig = "string_yyy";
        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleRestriction2(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleRestriction2(x, y, z);
        } else {
            ret = rpcClient.testSimpleRestriction2(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testSimpleRestriction2(): Incorrect value for inout param", x, y.value);
            assertEquals("testSimpleRestriction2(): Incorrect value for out param", yOrig, z.value);
            assertEquals("testSimpleRestriction2(): Incorrect return value", x, ret);
        }

        // Schema validation is enabled for doc-literal
        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            x = "str";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = testDocLiteral ? docClient.testSimpleRestriction2(x, y, z) : xmlClient
                    .testSimpleRestriction2(x, y, z);
                fail("minLength=5 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    @Test
    public void testSimpleRestriction3() throws Exception {
        if (!shouldRunTest("SimpleRestriction3")) {
            return;
        }
        // normal case, maxLength=10 && minLength=5
        String x = "str_x";
        String yOrig = "string_yyy";
        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleRestriction3(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleRestriction3(x, y, z);
        } else {
            ret = rpcClient.testSimpleRestriction3(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testSimpleRestriction3(): Incorrect value for inout param", x, y.value);
            assertEquals("testSimpleRestriction3(): Incorrect value for out param", yOrig, z.value);
            assertEquals("testSimpleRestriction3(): Incorrect return value", x, ret);
        }

        // Schema validation is enabled for doc-literal
        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            x = "str";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = docClient.testSimpleRestriction3(x, y, z);
                fail("x parameter maxLength=10 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }

            // abnormal case
            x = "string_x";
            yOrig = "string_yyyyyy";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = testDocLiteral ? docClient.testSimpleRestriction3(x, y, z) : xmlClient
                    .testSimpleRestriction3(x, y, z);
                fail("y parameter maxLength=10 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    @Test
    public void testSimpleRestriction4() throws Exception {
        if (!shouldRunTest("SimpleRestriction4")) {
            return;
        }
        // normal case, length=1
        String x = "x";
        String yOrig = "y";
        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleRestriction4(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleRestriction4(x, y, z);
        } else {
            ret = rpcClient.testSimpleRestriction4(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testSimpleRestriction4(): Incorrect value for inout param", x, y.value);
            assertEquals("testSimpleRestriction4(): Incorrect value for out param", yOrig, z.value);
            assertEquals("testSimpleRestriction4(): Incorrect return value", x, ret);
        }

        // Schema validation is enabled for doc-literal
        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            x = "str";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = testDocLiteral ? docClient.testSimpleRestriction4(x, y, z) : xmlClient
                    .testSimpleRestriction4(x, y, z);
                fail("x parameter minLength=5 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    @Test
    public void testSimpleRestriction5() throws Exception {
        if (!shouldRunTest("SimpleRestriction5")) {
            return;
        }
        // normal case, maxLength=10 for SimpleRestrction
        // && minLength=5 for SimpleRestriction5
        String x = "str_x";
        String yOrig = "string_yyy";
        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleRestriction5(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleRestriction5(x, y, z);
        } else {
            ret = rpcClient.testSimpleRestriction5(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testSimpleRestriction5(): Incorrect value for inout param", x, y.value);
            assertEquals("testSimpleRestriction5(): Incorrect value for out param", yOrig, z.value);
            assertEquals("testSimpleRestriction5(): Incorrect return value", x, ret);
        }

        // Schema validation is enabled for doc-literal
        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            x = "str";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = docClient.testSimpleRestriction5(x, y, z);
                fail("maxLength=10 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }

            // abnormal case
            x = "string_x";
            yOrig = "string_yyyyyy";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = testDocLiteral ? docClient.testSimpleRestriction5(x, y, z) : xmlClient
                    .testSimpleRestriction5(x, y, z);
                fail("maxLength=10 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    @Test
    public void testSimpleRestriction6() throws Exception {
        if (!shouldRunTest("SimpleRestriction6")) {
            return;
        }
        String x = "str_x";
        String yOrig = "y";
        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleRestriction6(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleRestriction6(x, y, z);
        } else {
            ret = rpcClient.testSimpleRestriction6(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testSimpleRestriction6(): Incorrect value for inout param", x, y.value);
            assertEquals("testSimpleRestriction6(): Incorrect value for out param", yOrig, z.value);
            assertEquals("testSimpleRestriction6(): Incorrect return value", x, ret);
        }

        // Schema validation is enabled for doc-literal
        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            x = "string_x";
            yOrig = "string_y";
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = testDocLiteral ? docClient.testSimpleRestriction6(x, y, z) : xmlClient
                    .testSimpleRestriction6(x, y, z);
                fail("maxLength=10 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    @Test
    public void testHexBinaryRestriction() throws Exception {
        if (!shouldRunTest("HexBinaryRestriction")) {
            return;
        }
        // normal case, maxLength=10 && minLength=1
        byte[] x = "x".getBytes();
        byte[] yOrig = "string_yyy".getBytes();
        Holder<byte[]> y = new Holder<byte[]>(yOrig);
        Holder<byte[]> z = new Holder<byte[]>();

        byte[] ret;
        if (testDocLiteral) {
            ret = docClient.testHexBinaryRestriction(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testHexBinaryRestriction(x, y, z);
        } else {
            ret = rpcClient.testHexBinaryRestriction(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testHexBinaryRestriction(): Incorrect value for inout param", equals(x, y.value));
            assertTrue("testHexBinaryRestriction(): Incorrect value for out param", equals(yOrig, z.value));
            assertTrue("testHexBinaryRestriction(): Incorrect return value", equals(x, ret));
        }

        // Schema validation is enabled for doc-literal
        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            x = "".getBytes();
            y = new Holder<byte[]>(yOrig);
            z = new Holder<byte[]>();
            try {
                ret = docClient.testHexBinaryRestriction(x, y, z);
                fail("maxLength=10 && minLength=1 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }

            // abnormal case
            x = "string_x".getBytes();
            yOrig = "string_yyyyyy".getBytes();
            y = new Holder<byte[]>(yOrig);
            z = new Holder<byte[]>();
            try {
                ret = testDocLiteral ? docClient.testHexBinaryRestriction(x, y, z) : xmlClient
                    .testHexBinaryRestriction(x, y, z);
                fail("maxLength=10 && minLength=1 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    protected boolean equals(byte[] x, byte[] y) {
        String xx = IOUtils.newStringFromBytes(x);
        String yy = IOUtils.newStringFromBytes(y);
        return xx.equals(yy);
    }

    @Test
    public void testBase64BinaryRestriction() throws Exception {
        if (!shouldRunTest("Base64BinaryRestriction")) {
            return;
        }
        byte[] x = "string_xxx".getBytes();
        byte[] yOrig = "string_yyy".getBytes();
        Holder<byte[]> y = new Holder<byte[]>(yOrig);
        Holder<byte[]> z = new Holder<byte[]>();

        byte[] ret;
        if (testDocLiteral) {
            ret = docClient.testBase64BinaryRestriction(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testBase64BinaryRestriction(x, y, z);
        } else {
            ret = rpcClient.testBase64BinaryRestriction(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testBase64BinaryRestriction(): Incorrect value for inout param", equals(x, y.value));
            assertTrue("testBase64BinaryRestriction(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testBase64BinaryRestriction(): Incorrect return value", equals(x, ret));
        }

        // Schema validation is enabled for doc-literal
        if (testDocLiteral) {
            // abnormal case
            x = "string_xxxxx".getBytes();
            y = new Holder<byte[]>(yOrig);
            z = new Holder<byte[]>();
            try {
                ret = docClient.testBase64BinaryRestriction(x, y, z);
                fail("length=10 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        }
    }

    @Test
    public void testSimpleListRestriction2() throws Exception {
        if (!shouldRunTest("SimpleListRestriction2")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<String> x = Arrays.asList("I", "am", "SimpleList");
            List<String> yOrig = Arrays.asList("Does", "SimpleList", "Work");
            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();
            List<String> ret = testDocLiteral ? docClient.testSimpleListRestriction2(x, y, z) : xmlClient
                .testSimpleListRestriction2(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testStringList(): Incorrect value for inout param", x.equals(y.value));
                assertTrue("testStringList(): Incorrect value for out param", yOrig.equals(z.value));
                assertTrue("testStringList(): Incorrect return value", x.equals(ret));
            }
            x = new ArrayList<String>();
            y = new Holder<List<String>>(yOrig);
            z = new Holder<List<String>>();
            try {
                ret = testDocLiteral ? docClient.testSimpleListRestriction2(x, y, z) : xmlClient
                    .testSimpleListRestriction2(x, y, z);
                fail("length=10 restriction is violated.");
            } catch (Exception ex) {
                // ex.printStackTrace();
            }
        } else {
            String[] x = {"I", "am", "SimpleList"};
            String[] yOrig = {"Does", "SimpleList", "Work"};
            Holder<String[]> y = new Holder<String[]>(yOrig);
            Holder<String[]> z = new Holder<String[]>();

            // normal case, maxLength=10 && minLength=1
            String[] ret = rpcClient.testSimpleListRestriction2(x, y, z);

            assertTrue(y.value.length == 3);
            assertTrue(z.value.length == 3);
            assertTrue(ret.length == 3);
            if (!perfTestOnly) {
                for (int i = 0; i < 3; i++) {
                    assertEquals("testStringList(): Incorrect value for inout param", x[i], y.value[i]);
                    assertEquals("testStringList(): Incorrect value for out param", yOrig[i], z.value[i]);
                    assertEquals("testStringList(): Incorrect return value", x[i], ret[i]);
                }
            }
        }
    }

    @Test
    public void testStringList() throws Exception {
        if (!shouldRunTest("StringList")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<String> x = Arrays.asList("I", "am", "SimpleList");
            List<String> yOrig = Arrays.asList("Does", "SimpleList", "Work");
            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();

            List<String> ret = testDocLiteral ? docClient.testStringList(x, y, z) : xmlClient
                .testStringList(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testStringList(): Incorrect value for inout param", x.equals(y.value));
                assertTrue("testStringList(): Incorrect value for out param", yOrig.equals(z.value));
                assertTrue("testStringList(): Incorrect return value", x.equals(ret));
            }
            if (testDocLiteral) {
                try {
                    ret = docClient.testStringList(null, y, z);
                } catch (SOAPFaultException ex) {
                    assertTrue(ex.getMessage(), ex.getMessage().contains("Unmarshalling"));
                }
            }
        } else {
            String[] x = {"I", "am", "SimpleList"};
            String[] yOrig = {"Does", "SimpleList", "Work"};
            Holder<String[]> y = new Holder<String[]>(yOrig);
            Holder<String[]> z = new Holder<String[]>();

            String[] ret = rpcClient.testStringList(x, y, z);

            assertTrue(y.value.length == 3);
            assertTrue(z.value.length == 3);
            assertTrue(ret.length == 3);
            if (!perfTestOnly) {
                for (int i = 0; i < 3; i++) {
                    assertEquals("testStringList(): Incorrect value for inout param", x[i], y.value[i]);
                    assertEquals("testStringList(): Incorrect value for out param", yOrig[i], z.value[i]);
                    assertEquals("testStringList(): Incorrect return value", x[i], ret[i]);
                }
            }
        }
    }

    @Test
    public void testNumberList() throws Exception {
        if (!shouldRunTest("NumberList")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<Integer> x = Arrays.asList(1, 2, 3);
            List<Integer> yOrig = Arrays.asList(10, 100, 1000);
            Holder<List<Integer>> y = new Holder<List<Integer>>(yOrig);
            Holder<List<Integer>> z = new Holder<List<Integer>>();

            List<Integer> ret = testDocLiteral ? docClient.testNumberList(x, y, z) : xmlClient
                .testNumberList(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testNumberList(): Incorrect value for inout param", x.equals(y.value));
                assertTrue("testNumberList(): Incorrect value for out param", yOrig.equals(z.value));
                assertTrue("testNumberList(): Incorrect return value", x.equals(ret));
            }
        } else {
            Integer[] x = {1, 2, 3};
            Integer[] yOrig = {10, 100, 1000};
            Holder<Integer[]> y = new Holder<Integer[]>(yOrig);
            Holder<Integer[]> z = new Holder<Integer[]>();

            Integer[] ret = rpcClient.testNumberList(x, y, z);

            assertTrue(y.value.length == 3);
            assertTrue(z.value.length == 3);
            assertTrue(ret.length == 3);
            if (!perfTestOnly) {
                for (int i = 0; i < 3; i++) {
                    assertEquals("testNumberList(): Incorrect value for inout param", x[i], y.value[i]);
                    assertEquals("testNumberList(): Incorrect value for out param", yOrig[i], z.value[i]);
                    assertEquals("testNumberList(): Incorrect return value", x[i], ret[i]);
                }
            }
        }
    }

    @Test
    public void testQNameList() throws Exception {
        if (!shouldRunTest("QNameList")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<QName> x = Arrays.asList(new QName("http://schemas.iona.com/type_test", "testqname1"),
                                          new QName("http://schemas.iona.com/type_test", "testqname2"),
                                          new QName("http://schemas.iona.com/type_test", "testqname3"));
            List<QName> yOrig = Arrays.asList(new QName("http://schemas.iona.com/type_test", "testqname4"),
                                              new QName("http://schemas.iona.com/type_test", "testqname5"),
                                              new QName("http://schemas.iona.com/type_test", "testqname6"));
            Holder<List<QName>> y = new Holder<List<QName>>(yOrig);
            Holder<List<QName>> z = new Holder<List<QName>>();

            List<QName> ret = testDocLiteral ? docClient.testQNameList(x, y, z) : xmlClient.testQNameList(x,
                                                                                                          y,
                                                                                                          z);
            if (!perfTestOnly) {
                assertTrue("testQNameList(): Incorrect value for inout param", x.equals(y.value));
                assertTrue("testQNameList(): Incorrect value for out param", yOrig.equals(z.value));
                assertTrue("testQNameList(): Incorrect return value", x.equals(ret));
            }
        } else {
            QName[] x = {new QName("http://schemas.iona.com/type_test", "testqname1"),
                         new QName("http://schemas.iona.com/type_test", "testqname2"),
                         new QName("http://schemas.iona.com/type_test", "testqname3")};
            QName[] yOrig = {new QName("http://schemas.iona.com/type_test", "testqname4"),
                             new QName("http://schemas.iona.com/type_test", "testqname5"),
                             new QName("http://schemas.iona.com/type_test", "testqname6")};
            Holder<QName[]> y = new Holder<QName[]>(yOrig);
            Holder<QName[]> z = new Holder<QName[]>();

            QName[] ret = rpcClient.testQNameList(x, y, z);

            assertTrue(y.value.length == 3);
            assertTrue(z.value.length == 3);
            assertTrue(ret.length == 3);
            if (!perfTestOnly) {
                for (int i = 0; i < 3; i++) {
                    assertEquals("testQNameList(): Incorrect value for inout param", x[i], y.value[i]);
                    assertEquals("testQNameList(): Incorrect value for out param", yOrig[i], z.value[i]);
                    assertEquals("testQNameList(): Incorrect return value", x[i], ret[i]);
                }
            }
        }
    }

    @Test
    public void testSimpleUnionList() throws Exception {
        if (!shouldRunTest("SimpleUnionList")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<String> x = Arrays.asList("5", "-7");
            List<String> yOrig = Arrays.asList("-9", "7");

            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();

            List<String> ret = testDocLiteral ? docClient.testSimpleUnionList(x, y, z) : xmlClient
                .testSimpleUnionList(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testSimpleUnionList(): Incorrect value for inout param", x.equals(y.value));
                assertTrue("testSimpleUnionList(): Incorrect value for out param", yOrig.equals(z.value));
                assertTrue("testSimpleUnionList(): Incorrect return value", x.equals(ret));
            }
        } else {
            String[] x = {"5", "-7"};
            String[] yOrig = {"-9", "7"};

            Holder<String[]> y = new Holder<String[]>(yOrig);
            Holder<String[]> z = new Holder<String[]>();

            String[] ret = rpcClient.testSimpleUnionList(x, y, z);

            assertTrue(y.value.length == 2);
            assertTrue(z.value.length == 2);
            assertTrue(ret.length == 2);
            if (!perfTestOnly) {
                for (int i = 0; i < 2; i++) {
                    assertEquals("testSimpleUnionList(): Incorrect value for inout param", x[i], y.value[i]);
                    assertEquals("testSimpleUnionList(): Incorrect value for out param",
                                 yOrig[i], z.value[i]);
                    assertEquals("testSimpleUnionList(): Incorrect return value", x[i], ret[i]);
                }
            }
        }
    }

}
