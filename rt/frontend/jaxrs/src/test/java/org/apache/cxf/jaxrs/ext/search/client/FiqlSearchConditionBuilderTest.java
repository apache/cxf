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
package org.apache.cxf.jaxrs.ext.search.client;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

import static junit.framework.Assert.assertEquals;

import org.apache.cxf.jaxrs.ext.search.client.SearchConditionBuilder.PartialCondition;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class FiqlSearchConditionBuilderTest {
    private static FiqlSearchConditionBuilder b = new FiqlSearchConditionBuilder();
    private static TimeZone tz;
    private DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm Z");
    
    @BeforeClass
    public static void beforeClass() {
        tz = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
    }
    
    @AfterClass
    public static void afterClass() {
        // restoring defaults
        TimeZone.setDefault(tz);
    }
    
    
    @Test
    public void testEmptyBuild() {
        assertEquals("", b.build());
    }

    @Test
    public void testEqualToString() {
        String ret = b.query().is("foo").equalTo("literalOrPattern*").build();
        assertEquals("foo==literalOrPattern*", ret);
    }

    @Test
    public void testEqualToNumber() {
        String ret = b.query().is("foo").equalTo(123.5).build();
        assertEquals("foo==123.5", ret);
    }

    @Test
    public void testEqualToDate() throws ParseException {
        Date d = df.parse("2011-03-01 12:34 +0000");
        String ret = b.query().is("foo").equalTo(d).build();
        assertEquals("foo==2011-03-01T12:34:00.000+00:00", ret);
    }

    @Test
    public void testEqualToDuration() throws ParseException, DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0); 
        String ret = b.query().is("foo").equalTo(d).build();
        assertEquals("foo==-P0Y0M1DT12H0M0S", ret);
    }

    @Test
    public void testNotEqualToString() {
        String ret = b.query().is("foo").notEqualTo("literalOrPattern*").build();
        assertEquals("foo!=literalOrPattern*", ret);
    }

    @Test
    public void testNotEqualToNumber() {
        String ret = b.query().is("foo").notEqualTo(123.5).build();
        assertEquals("foo!=123.5", ret);
    }

    @Test
    public void testNotEqualToDate() throws ParseException {
        Date d = df.parse("2011-03-01 12:34 +0000");
        String ret = b.query().is("foo").notEqualTo(d).build();
        assertEquals("foo!=2011-03-01T12:34:00.000+00:00", ret);
    }

    @Test
    public void testNotEqualToDuration() throws ParseException, DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0); 
        String ret = b.query().is("foo").notEqualTo(d).build();
        assertEquals("foo!=-P0Y0M1DT12H0M0S", ret);
    }

    @Test
    public void testGreaterThanString() {
        String ret = b.query().is("foo").lexicalAfter("abc").build();
        assertEquals("foo=gt=abc", ret);
    }

    @Test
    public void testLessThanString() {
        String ret = b.query().is("foo").lexicalBefore("abc").build();
        assertEquals("foo=lt=abc", ret);
    }

    @Test
    public void testLessOrEqualToString() {
        String ret = b.query().is("foo").lexicalNotAfter("abc").build();
        assertEquals("foo=le=abc", ret);
    }

    @Test
    public void testGreaterOrEqualToString() {
        String ret = b.query().is("foo").lexicalNotBefore("abc").build();
        assertEquals("foo=ge=abc", ret);
    }
    
    @Test
    public void testGreaterThanNumber() {
        String ret = b.query().is("foo").greaterThan(25).build();
        assertEquals("foo=gt=25.0", ret);
    }

    @Test
    public void testLessThanNumber() {
        String ret = b.query().is("foo").lessThan(25.333).build();
        assertEquals("foo=lt=25.333", ret);
    }

    @Test
    public void testLessOrEqualToNumber() {
        String ret = b.query().is("foo").lessOrEqualTo(0).build();
        assertEquals("foo=le=0.0", ret);
    }

    @Test
    public void testGreaterOrEqualToNumber() {
        String ret = b.query().is("foo").greaterOrEqualTo(-5).build();
        assertEquals("foo=ge=-5.0", ret);
    }

    @Test
    public void testGreaterThanDate() throws ParseException {
        Date d = df.parse("2011-03-02 22:33 +0000");
        String ret = b.query().is("foo").after(d).build();
        assertEquals("foo=gt=2011-03-02T22:33:00.000+00:00", ret);
    }

    @Test
    public void testLessThanDate() throws ParseException {
        Date d = df.parse("2011-03-02 22:33 +0000");
        String ret = b.query().is("foo").before(d).build();
        assertEquals("foo=lt=2011-03-02T22:33:00.000+00:00", ret);
    }

    @Test
    public void testLessOrEqualToDate() throws ParseException {
        Date d = df.parse("2011-03-02 22:33 +0000");
        String ret = b.query().is("foo").notAfter(d).build();
        assertEquals("foo=le=2011-03-02T22:33:00.000+00:00", ret);
    }

    @Test
    public void testGreaterOrEqualToDate() throws ParseException {
        Date d = df.parse("2011-03-02 22:33 +0000");
        String ret = b.query().is("foo").notBefore(d).build();
        assertEquals("foo=ge=2011-03-02T22:33:00.000+00:00", ret);
    }

    @Test
    public void testGreaterThanDuration() throws DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0); 
        String ret = b.query().is("foo").after(d).build();
        assertEquals("foo=gt=-P0Y0M1DT12H0M0S", ret);
    }

    @Test
    public void testLessThanDuration() throws DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0); 
        String ret = b.query().is("foo").before(d).build();
        assertEquals("foo=lt=-P0Y0M1DT12H0M0S", ret);
    }

    @Test
    public void testLessOrEqualToDuration() throws DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0); 
        String ret = b.query().is("foo").notAfter(d).build();
        assertEquals("foo=le=-P0Y0M1DT12H0M0S", ret);
    }

    @Test
    public void testGreaterOrEqualToDuration() throws DatatypeConfigurationException {
        Duration d = DatatypeFactory.newInstance().newDuration(false, 0, 0, 1, 12, 0, 0); 
        String ret = b.query().is("foo").notBefore(d).build();
        assertEquals("foo=ge=-P0Y0M1DT12H0M0S", ret);
    }
    
    @Test
    public void testOrSimple() {
        String ret = b.query().is("foo").greaterThan(20).or().is("foo").lessThan(10).build();
        assertEquals("foo=gt=20.0,foo=lt=10.0", ret);
    }    
    
    @Test
    public void testAndSimple() {
        String ret = b.query().is("foo").greaterThan(20).and().is("bar").equalTo("plonk").build();
        assertEquals("foo=gt=20.0;bar==plonk", ret);
    }
    
    @Test
    public void testOrComplex() {
        PartialCondition c = b.query();
        String ret = c.or(c.is("foo").equalTo("aaa"), c.is("bar").equalTo("bbb")).build();
        assertEquals("(foo==aaa,bar==bbb)", ret);
    }    

    @Test
    public void testAndComplex() {
        PartialCondition c = b.query();
        String ret = c.and(c.is("foo").equalTo("aaa"), c.is("bar").equalTo("bbb")).build();
        assertEquals("(foo==aaa;bar==bbb)", ret);
    }    

    @Test
    public void testComplex1() {
        PartialCondition c = b.query();
        String ret = c.is("foo").equalTo(123.4).or().and(
            c.is("bar").equalTo("asadf*"), 
            c.is("baz").lessThan(20)).build();
        assertEquals("foo==123.4,(bar==asadf*;baz=lt=20.0)", ret);
    }

    @Test
    public void testComplex2() {
        PartialCondition c = b.query();
        String ret = c.is("foo").equalTo(123.4).or().is("foo").equalTo("null").and().or(
            c.is("bar").equalTo("asadf*"), 
            c.is("baz").lessThan(20).and().or(
                c.is("sub1").equalTo(0),
                c.is("sub2").equalTo(0))).build();
        
        assertEquals("foo==123.4,foo==null;(bar==asadf*,baz=lt=20.0;(sub1==0.0,sub2==0.0))", ret);
    }
}
