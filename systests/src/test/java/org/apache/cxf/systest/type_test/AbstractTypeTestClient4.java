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

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.ws.Holder;

import org.apache.type_test.types1.AnonymousType;
//importorg.apache.type_test.types1.ComplexArray;
//importorg.apache.type_test.types1.ComplexChoice;
//importorg.apache.type_test.types1.ComplexStruct;
//importorg.apache.type_test.types1.DerivedAllBaseAll;
//importorg.apache.type_test.types1.DerivedAllBaseChoice;
//importorg.apache.type_test.types1.DerivedAllBaseStruct;
//importorg.apache.type_test.types1.DerivedChoiceBaseAll;
import org.apache.type_test.types1.DerivedChoiceBaseArray;
//importorg.apache.type_test.types1.DerivedChoiceBaseComplex;
import org.apache.type_test.types1.DerivedEmptyBaseEmptyAll;
//importorg.apache.type_test.types1.DerivedStructBaseAll;
import org.apache.type_test.types1.DerivedStructBaseChoice;
import org.apache.type_test.types1.DerivedStructBaseStruct;
import org.apache.type_test.types1.EmptyAll;
import org.apache.type_test.types1.EmptyStruct;
import org.apache.type_test.types1.NestedStruct;
import org.apache.type_test.types1.OccuringAll;
import org.apache.type_test.types1.RecSeqB6918;
import org.apache.type_test.types1.RestrictedAllBaseAll;
import org.apache.type_test.types1.RestrictedStructBaseStruct;
//importorg.apache.type_test.types1.SimpleAll;
import org.apache.type_test.types1.SimpleChoice;
import org.apache.type_test.types1.SimpleStruct;
import org.apache.type_test.types1.UnboundedArray;
import org.apache.type_test.types2.OccuringChoiceWithAnyAttribute;
import org.apache.type_test.types2.OccuringStructWithAnyAttribute;
import org.apache.type_test.types2.SimpleContentExtWithAnyAttribute;
import org.apache.type_test.types3.ArrayOfMRecSeqD;
import org.apache.type_test.types3.MRecSeqA;
import org.apache.type_test.types3.MRecSeqB;
import org.apache.type_test.types3.MRecSeqC;
import org.apache.type_test.types3.MRecSeqD;
import org.apache.type_test.types3.StructWithNillableChoice;
import org.apache.type_test.types3.StructWithNillableStruct;
import org.apache.type_test.types3.StructWithOccuringChoice;
import org.apache.type_test.types3.StructWithOccuringStruct;
import org.junit.Test;

public abstract class AbstractTypeTestClient4 extends AbstractTypeTestClient3 {

    //org.apache.type_test.types2.SimpleContentExtWithAnyAttribute;

    protected boolean equals(SimpleContentExtWithAnyAttribute x,
                             SimpleContentExtWithAnyAttribute y) {
        if (!x.getValue().equals(y.getValue())) {
            return false;
        }
        if (!equalsNilable(x.getAttrib(), y.getAttrib())) {
            return false;
        }
        return equalsQNameStringPairs(x.getOtherAttributes(), y.getOtherAttributes());
    }

    @Test
    public void testSimpleContentExtWithAnyAttribute() throws Exception {
        if (!shouldRunTest("SimpleContentExtWithAnyAttribute")) {
            return;
        }
        QName xAt1Name = new QName("http://apache.org/type_test", "at_one");
        QName xAt2Name = new QName("http://apache.org/type_test", "at_two");
        QName yAt3Name = new QName("http://apache.org/type_test", "at_thr");
        QName yAt4Name = new QName("http://apache.org/type_test", "at_fou");

        SimpleContentExtWithAnyAttribute x = new SimpleContentExtWithAnyAttribute();
        x.setValue("foo");
        x.setAttrib(new Integer(2000));

        SimpleContentExtWithAnyAttribute y = new SimpleContentExtWithAnyAttribute();
        y.setValue("bar");
        y.setAttrib(new Integer(2001));

        Map<QName, String> xAttrMap = x.getOtherAttributes();
        xAttrMap.put(xAt1Name, "one");
        xAttrMap.put(xAt2Name, "two");

        Map<QName, String> yAttrMap = y.getOtherAttributes();
        yAttrMap.put(yAt3Name, "three");
        yAttrMap.put(yAt4Name, "four");

        Holder<SimpleContentExtWithAnyAttribute> yh = new Holder<SimpleContentExtWithAnyAttribute>(y);
        Holder<SimpleContentExtWithAnyAttribute> zh = new Holder<SimpleContentExtWithAnyAttribute>();
        SimpleContentExtWithAnyAttribute ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleContentExtWithAnyAttribute(x, yh, zh);
        } else if (testXMLBinding) {            
            ret = xmlClient.testSimpleContentExtWithAnyAttribute(x, yh, zh);
        } else {
            ret = rpcClient.testSimpleContentExtWithAnyAttribute(x, yh, zh);
        }

        if (!perfTestOnly) {
            assertTrue("testSimpleContentExtWithAnyAttribute(): Incorrect value for inout param",
                equals(x, yh.value));
            assertTrue("testSimpleContentExtWithAnyAttribute(): Incorrect value for out param",
                equals(y, zh.value));
            assertTrue("testSimpleContentExtWithAnyAttribute(): Incorrect return value",
                equals(ret, x));
        }
    }

    //org.apache.type_test.types3.OccuringAll;

    protected boolean equals(OccuringAll x, OccuringAll y) {
        if (x.getVarAttrString() == null && y.getVarAttrString() == null) {
            return x.getVarInt() == null && y.getVarInt() == null;
        } else if (!equalsNilable(x.getVarAttrString(), y.getVarAttrString())) {
            return false;
        }
        return x.getVarInt().compareTo(y.getVarInt()) == 0;
    }

    @Test
    public void testOccuringAll() throws Exception {
        if (!shouldRunTest("OccuringAll")) {
            return;
        }
        OccuringAll x = new OccuringAll();
        x.setVarInt(new Integer(42));
        x.setVarAttrString("x_attr");
        OccuringAll yOrig = new OccuringAll();
        Holder<OccuringAll> y = new Holder<OccuringAll>(yOrig);
        Holder<OccuringAll> z = new Holder<OccuringAll>();
        OccuringAll ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringAll(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringAll(x, y, z);
        } else {
            ret = rpcClient.testOccuringAll(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testOccuringAll(): Incorrect value for inout param", equals(x, y.value));
            assertTrue("testOccuringAll(): Incorrect value for out param", equals(yOrig, z.value));
            assertTrue("testOccuringAll(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.OccuringStructWithAnyAttribute;

    protected boolean equals(OccuringStructWithAnyAttribute x,
                             OccuringStructWithAnyAttribute y) {
        if (!equalsNilable(x.getAtString(), y.getAtString())
            || !equalsNilable(x.getAtInt(), y.getAtInt())) {
            return false;
        }
        List<Serializable> xList = x.getVarStringAndVarInt();
        List<Serializable> yList = y.getVarStringAndVarInt();
        if (!equalsStringIntList(xList, yList)) {
            return false;
        }
        return equalsQNameStringPairs(x.getOtherAttributes(), y.getOtherAttributes());
    }

    private boolean equalsStringIntList(List<Serializable> xList, List<Serializable> yList) {
        int size = xList.size();
        if (size != yList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (xList.get(i) instanceof String && yList.get(i) instanceof String) {
                if (!xList.get(i).equals(yList.get(i))) {
                    return false;
                }
            } else if (xList.get(i) instanceof Integer && yList.get(i) instanceof Integer) {
                Integer ix = (Integer)xList.get(i);
                Integer iy = (Integer)yList.get(i);
                if (iy.compareTo(ix) != 0) {
                    return false;
                }
            } else {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testOccuringStructWithAnyAttribute() throws Exception {
        if (!shouldRunTest("OccuringStructWithAnyAttribute")) {
            return;
        }
        QName xAt1Name = new QName("http://apache.org/type_test", "at_one");
        QName xAt2Name = new QName("http://apache.org/type_test", "at_two");
        QName yAt3Name = new QName("http://apache.org/type_test", "at_thr");
        QName yAt4Name = new QName("http://apache.org/type_test", "at_fou");

        OccuringStructWithAnyAttribute x = new OccuringStructWithAnyAttribute();
        OccuringStructWithAnyAttribute y = new OccuringStructWithAnyAttribute();
        List<Serializable> xVarStringAndVarInt = x.getVarStringAndVarInt();
        xVarStringAndVarInt.add("x1");
        xVarStringAndVarInt.add(0);
        xVarStringAndVarInt.add("x2");
        xVarStringAndVarInt.add(1);
        x.setAtString("attribute");
        x.setAtInt(new Integer(2000));

        List<Serializable> yVarStringAndVarInt = y.getVarStringAndVarInt();
        yVarStringAndVarInt.add("there");
        yVarStringAndVarInt.add(1001);
        y.setAtString("another attribute");
        y.setAtInt(new Integer(2002));

        Map<QName, String> xAttrMap = x.getOtherAttributes();
        xAttrMap.put(xAt1Name, "one");
        xAttrMap.put(xAt2Name, "two");

        Map<QName, String> yAttrMap = y.getOtherAttributes();
        yAttrMap.put(yAt3Name, "three");
        yAttrMap.put(yAt4Name, "four");

        Holder<OccuringStructWithAnyAttribute> yh = new Holder<OccuringStructWithAnyAttribute>(y);
        Holder<OccuringStructWithAnyAttribute> zh = new Holder<OccuringStructWithAnyAttribute>();
        OccuringStructWithAnyAttribute ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringStructWithAnyAttribute(x, yh, zh);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringStructWithAnyAttribute(x, yh, zh);
        } else {
            ret = rpcClient.testOccuringStructWithAnyAttribute(x, yh, zh);
        }

        if (!perfTestOnly) {
            assertTrue("testOccuringStructWithAnyAttribute(): Incorrect value for inout param",
                equals(x, yh.value));
            assertTrue("testOccuringStructWithAnyAttribute(): Incorrect value for inout param",
                equals(y, zh.value));
            assertTrue("testOccuringStructWithAnyAttribute(): Incorrect value for inout param",
                equals(ret, x));
        }
    }

    //org.apache.type_test.types2.OccuringChoiceWithAnyAttribute;

    protected boolean equals(OccuringChoiceWithAnyAttribute x,
                             OccuringChoiceWithAnyAttribute y) {
        if (!equalsNilable(x.getAtString(), y.getAtString())
            || !equalsNilable(x.getAtInt(), y.getAtInt())) {
            return false;
        }
        List<Serializable> xList = x.getVarStringOrVarInt();
        List<Serializable> yList = y.getVarStringOrVarInt();
        if (!equalsStringIntList(xList, yList)) {
            return false;
        }
        return equalsQNameStringPairs(x.getOtherAttributes(), y.getOtherAttributes());
    }

    @Test
    public void testOccuringChoiceWithAnyAttribute() throws Exception {
        if (!shouldRunTest("OccuringChoiceWithAnyAttribute")) {
            return;
        }
        QName xAt1Name = new QName("http://schemas.iona.com/type_test", "at_one");
        QName xAt2Name = new QName("http://schemas.iona.com/type_test", "at_two");
        QName yAt3Name = new QName("http://apache.org/type_test", "at_thr");
        QName yAt4Name = new QName("http://apache.org/type_test", "at_fou");

        OccuringChoiceWithAnyAttribute x = new OccuringChoiceWithAnyAttribute();
        OccuringChoiceWithAnyAttribute y = new OccuringChoiceWithAnyAttribute();

        List<Serializable> xVarStringOrVarInt = x.getVarStringOrVarInt();
        xVarStringOrVarInt.add("hello");
        xVarStringOrVarInt.add(1);
        x.setAtString("attribute");
        x.setAtInt(new Integer(2000));

        List<Serializable> yVarStringOrVarInt = y.getVarStringOrVarInt();
        yVarStringOrVarInt.add(1001);
        y.setAtString("the attribute");
        y.setAtInt(new Integer(2002));

        Map<QName, String> xAttrMap = x.getOtherAttributes();
        xAttrMap.put(xAt1Name, "one");
        xAttrMap.put(xAt2Name, "two");

        Map<QName, String> yAttrMap = y.getOtherAttributes();
        yAttrMap.put(yAt3Name, "three");
        yAttrMap.put(yAt4Name, "four");

        Holder<OccuringChoiceWithAnyAttribute> yh = new Holder<OccuringChoiceWithAnyAttribute>(y);
        Holder<OccuringChoiceWithAnyAttribute> zh = new Holder<OccuringChoiceWithAnyAttribute>();
        OccuringChoiceWithAnyAttribute ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringChoiceWithAnyAttribute(x, yh, zh);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringChoiceWithAnyAttribute(x, yh, zh);
        } else {
            ret = rpcClient.testOccuringChoiceWithAnyAttribute(x, yh, zh);
        }

        if (!perfTestOnly) {
            assertTrue("testOccuringChoiceWithAnyAttribute(): Incorrect value for inout param",
                equals(x, yh.value));
            assertTrue("testOccuringChoiceWithAnyAttribute(): Incorrect value for out param",
                equals(y, zh.value));
            assertTrue("testOccuringChoiceWithAnyAttribute(): Incorrect return value",
                equals(ret, x));
        }
    }

    //org.apache.type_test.types3.MRecSeqA;

    protected boolean equals(MRecSeqA x, MRecSeqA y) {
        List<MRecSeqB> xList = x.getSeqB();
        List<MRecSeqB> yList = y.getSeqB();
        int size = xList.size();
        if (size != yList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!equals(xList.get(i), yList.get(i))) {
                return false;
            }
        }
        return x.getVarIntA() == y.getVarIntA();
    }

    protected boolean equals(MRecSeqB x, MRecSeqB y) {
        return x.getVarIntB() == y.getVarIntB()
            && equals(x.getSeqA(), y.getSeqA());
    }

    @Test
    public void testMRecSeqA() throws Exception {
        if (!shouldRunTest("MRecSeqA")) {
            return;
        }
        MRecSeqA xA = new MRecSeqA();
        MRecSeqA yA = new MRecSeqA();
        MRecSeqA zA = new MRecSeqA();
        MRecSeqB xB = new MRecSeqB();
        MRecSeqB yB = new MRecSeqB();
        xA.setVarIntA(11);
        yA.setVarIntA(12);
        zA.setVarIntA(13);
        xB.setVarIntB(21);
        yB.setVarIntB(22);
        xB.setSeqA(yA);
        yB.setSeqA(zA);
        xA.getSeqB().add(xB);
        yA.getSeqB().add(yB);
        Holder<MRecSeqA> yh = new Holder<MRecSeqA>(yA);
        Holder<MRecSeqA> zh = new Holder<MRecSeqA>();
        MRecSeqA ret;
        if (testDocLiteral) {
            ret = docClient.testMRecSeqA(xA, yh, zh);
        } else if (testXMLBinding) {
            ret = xmlClient.testMRecSeqA(xA, yh, zh);
        } else {
            ret = rpcClient.testMRecSeqA(xA, yh, zh);
        }
        if (!perfTestOnly) {
            assertTrue("test_MRecSeqA(): Incorrect value for inout param",
                equals(xA, yh.value));
            assertTrue("test_MRecSeqA(): Incorrect value for out param",
                equals(yA, zh.value));
            assertTrue("test_MRecSeqA(): Incorrect return value",
                equals(ret, xA));
        }
    }

    //org.apache.type_test.types3.MRecSeqC;

    protected boolean equals(MRecSeqC x, MRecSeqC y) {
        return x.getVarIntC() == y.getVarIntC()
            && equals(x.getSeqDs(), y.getSeqDs());
    }

    protected boolean equals(ArrayOfMRecSeqD x, ArrayOfMRecSeqD y) {
        List<MRecSeqD> xList = x.getSeqD();
        List<MRecSeqD> yList = y.getSeqD();
        int size = xList.size();
        if (size != yList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (!equals(xList.get(i), yList.get(i))) {
                return false;
            }
        }
        return true;
    }

    protected boolean equals(MRecSeqD x, MRecSeqD y) {
        return x.getVarIntD() == y.getVarIntD()
            && equals(x.getSeqC(), y.getSeqC());
    }

    @Test
    public void testMRecSeqC() throws Exception {
        if (!shouldRunTest("MRecSeqC")) {
            return;
        }
        MRecSeqC xC = new MRecSeqC();
        MRecSeqC yC = new MRecSeqC();
        MRecSeqC zC = new MRecSeqC();
        ArrayOfMRecSeqD xDs = new ArrayOfMRecSeqD();
        ArrayOfMRecSeqD yDs = new ArrayOfMRecSeqD();
        ArrayOfMRecSeqD zDs = new ArrayOfMRecSeqD();
        MRecSeqD xD = new MRecSeqD();
        MRecSeqD yD = new MRecSeqD();
        xC.setVarIntC(11);
        yC.setVarIntC(12);
        zC.setVarIntC(13);
        xD.setVarIntD(21);
        yD.setVarIntD(22);
        xDs.getSeqD().add(xD);
        yDs.getSeqD().add(yD);
        xC.setSeqDs(xDs);
        yC.setSeqDs(yDs);
        zC.setSeqDs(zDs);
        xD.setSeqC(yC);
        yD.setSeqC(zC);
        Holder<MRecSeqC> yh = new Holder<MRecSeqC>(yC);
        Holder<MRecSeqC> zh = new Holder<MRecSeqC>();
        MRecSeqC ret;
        if (testDocLiteral) {
            ret = docClient.testMRecSeqC(xC, yh, zh);
        } else if (testXMLBinding) {
            ret = xmlClient.testMRecSeqC(xC, yh, zh);
        } else {
            ret = rpcClient.testMRecSeqC(xC, yh, zh);
        }
        if (!perfTestOnly) {
            assertTrue("test_MRecSeqC(): Incorrect value for inout param",
                equals(xC, yh.value));
            assertTrue("test_MRecSeqC(): Incorrect value for out param",
                equals(yC, zh.value));
            assertTrue("test_MRecSeqC(): Incorrect return value",
                equals(ret, xC));
        }
    }

    //org.apache.type_test.types3.StructWithNillableChoice;

    protected boolean equals(StructWithNillableChoice x, StructWithNillableChoice y) {
        if (x.getVarInteger() != y.getVarInteger()) {
            return false;
        }

        if (x.getVarString() != null) {
            return x.getVarString().equals(y.getVarString());
        } else if (x.getVarInt() != null) {
            return x.getVarInt() == y.getVarInt();
        }
        return y.getVarInt() == null && y.getVarString() == null;
    }

    protected boolean isNormalized(StructWithNillableChoice x) {
        return x == null || x.getVarInt() == null && x.getVarString() == null;
    }

    @Test
    public void testStructWithNillableChoice() throws Exception {
        if (!shouldRunTest("StructWithNillableChoice")) {
            return;
        }
        // Test 1
        //
        // x: non-nil choice
        // y: nil choice 
        //
        StructWithNillableChoice x = new StructWithNillableChoice();
        x.setVarInteger(2);
        x.setVarInt(3);

        StructWithNillableChoice yOriginal = new StructWithNillableChoice();
        yOriginal.setVarInteger(1);

        Holder<StructWithNillableChoice> y = new Holder<StructWithNillableChoice>(yOriginal);
        Holder<StructWithNillableChoice> z = new Holder<StructWithNillableChoice>();
        StructWithNillableChoice ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithNillableChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithNillableChoice(x, y, z);
        } else {
            ret = rpcClient.testStructWithNillableChoice(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithNillableChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithNillableChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithNillableChoice(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithNillableChoice(): Incorrect form for out param",
                       isNormalized(z.value));
        }

        // Test 2
        //
        // x: nil choice 
        // y: non-nil choice
        //
        y = new Holder<StructWithNillableChoice>(x);
        x = yOriginal;
        yOriginal = y.value;
        z = new Holder<StructWithNillableChoice>();
        if (testDocLiteral) {
            ret = docClient.testStructWithNillableChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithNillableChoice(x, y, z);
        } else {
            ret = rpcClient.testStructWithNillableChoice(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithNillableChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithNillableChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithNillableChoice(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithNillableChoice(): Incorrect form for inout param",
                       isNormalized(y.value));
            assertTrue("testStructWithNillableChoice(): Incorrect return form",
                       isNormalized(ret));
        }
    }

    //org.apache.type_test.types3.StructWithOccuringChoice;

    protected boolean equals(StructWithOccuringChoice x, StructWithOccuringChoice y) {
        if (x.getVarInteger() != y.getVarInteger()) {
            fail(x.getVarInteger() + " != " + y.getVarInteger());
            return false;
        }

        List<Serializable> xList = x.getVarIntOrVarString();
        List<Serializable> yList = y.getVarIntOrVarString();
        int size = xList.size();
        if (size != yList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (xList.get(i) instanceof Integer && yList.get(i) instanceof Integer) {
                Integer ix = (Integer)xList.get(i);
                Integer iy = (Integer)yList.get(i);
                if (iy.compareTo(ix) != 0) {
                    return false;
                }
            } else if (xList.get(i) instanceof String && yList.get(i) instanceof String) {
                String sx = (String)xList.get(i);
                String sy = (String)yList.get(i);
                if (!sx.equals(sy)) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    protected boolean isNormalized(StructWithOccuringChoice x) {
        return x == null || x.getVarIntOrVarString().size() == 0;
    }

    @Test
    public void testStructWithOccuringChoice() throws Exception {
        if (!shouldRunTest("StructWithOccuringChoice")) {
            return;
        }
        // Test 1
        //
        // x: choice occurs twice
        // y: choice doesn't occur
        //
        StructWithOccuringChoice x = new StructWithOccuringChoice();
        x.setVarInteger(2);
        x.getVarIntOrVarString().add(3);
        x.getVarIntOrVarString().add("hello"); 

        StructWithOccuringChoice yOriginal = new StructWithOccuringChoice();
        yOriginal.setVarInteger(1);

        Holder<StructWithOccuringChoice> y = new Holder<StructWithOccuringChoice>(yOriginal);
        Holder<StructWithOccuringChoice> z = new Holder<StructWithOccuringChoice>();
        StructWithOccuringChoice ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringChoice(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringChoice(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithOccuringChoice(): Incorrect form for out param",
                       isNormalized(z.value));
        }

        // Test 2
        //
        // x: choice occurs twice
        // y: choice occurs once
        //
        yOriginal.getVarIntOrVarString().add("world");

        y = new Holder<StructWithOccuringChoice>(yOriginal);
        z = new Holder<StructWithOccuringChoice>();
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringChoice(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect return value",
                       equals(x, ret));
        }

        // Test 3
        //
        // x: choice occurs once
        // y: choice occurs twice
        //
        y = new Holder<StructWithOccuringChoice>(x);
        x = yOriginal;
        yOriginal = y.value;
        z = new Holder<StructWithOccuringChoice>();
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringChoice(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringChoice(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect return value",
                       equals(x, ret));
        }

        // Test 4
        //
        // x: choice doesn't occur
        // y: choice occurs twice
        //
        x.getVarIntOrVarString().clear();

        y = new Holder<StructWithOccuringChoice>(yOriginal);
        z = new Holder<StructWithOccuringChoice>();
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringChoice(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringChoice(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithOccuringChoice(): Incorrect form for inout param",
                       isNormalized(y.value));
            assertTrue("testStructWithOccuringChoice(): Incorrect return form",
                       isNormalized(ret));
        }
    }

    //org.apache.type_test.types3.StructWithNillableStruct;

    protected boolean equals(StructWithNillableStruct x, StructWithNillableStruct y) {
        if (x.getVarInteger() != y.getVarInteger()) {
            fail(x.getVarInteger() + " != " + y.getVarInteger());
            return false;
        }

        if (x.getVarInt() == null) {
            if (x.getVarFloat() == null) {
                return y.getVarInt() == null && y.getVarFloat() == null;
            } else {
                return false;
            }
        } else {
            if (x.getVarFloat() == null || y.getVarInt() == null || y.getVarFloat() == null) {
                return false;
            }
        }
        return x.getVarFloat().compareTo(y.getVarFloat()) == 0 
            && x.getVarInt() == y.getVarInt();
    }

    protected boolean isNormalized(StructWithNillableStruct x) {
        return x.getVarInt() == null && x.getVarFloat() == null;
    }

    @Test
    public void testStructWithNillableStruct() throws Exception {
        if (!shouldRunTest("StructWithNillableStruct")) {
            return;
        }
        // Test 1
        //
        // x: non-nil sequence
        // y: nil sequence (non-null holder object)
        //
        StructWithNillableStruct x = new StructWithNillableStruct();
        x.setVarInteger(100);
        x.setVarInt(101);
        x.setVarFloat(101.5f);
        StructWithNillableStruct yOriginal = new StructWithNillableStruct();
        yOriginal.setVarInteger(200);

        Holder<StructWithNillableStruct> y =
            new Holder<StructWithNillableStruct>(yOriginal);
        Holder<StructWithNillableStruct> z = new Holder<StructWithNillableStruct>();
        StructWithNillableStruct ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithNillableStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithNillableStruct(x, y, z);
        } else {
            ret = rpcClient.testStructWithNillableStruct(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithNillableStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithNillableStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithNillableStruct(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithNillableStruct(): Incorrect form for out param",
                       isNormalized(z.value));
        }

        // Test 2
        //
        // x: non-nil sequence
        // y: nil sequence (null holder object)
        //
        yOriginal.setVarInt(null);
        yOriginal.setVarFloat(null);

        y = new Holder<StructWithNillableStruct>(yOriginal);
        z = new Holder<StructWithNillableStruct>();
        if (testDocLiteral) {
            ret = docClient.testStructWithNillableStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithNillableStruct(x, y, z);
        } else {
            ret = rpcClient.testStructWithNillableStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testStructWithNillableStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithNillableStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithNillableStruct(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithNillableStruct(): Incorrect form for out param",
                       isNormalized(z.value));
        }

        // Test 3
        //
        // x: nil sequence (null holder object)
        // y: non-nil sequence
        //
        y = new Holder<StructWithNillableStruct>(x);
        x = yOriginal;
        yOriginal = y.value;
        z = new Holder<StructWithNillableStruct>();
        if (testDocLiteral) {
            ret = docClient.testStructWithNillableStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithNillableStruct(x, y, z);
        } else {
            ret = rpcClient.testStructWithNillableStruct(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithNillableStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithNillableStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithNillableStruct(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithNillableStruct(): Incorrect form for inout param",
                       isNormalized(y.value));
            assertTrue("testStructWithNillableStruct(): Incorrect return form",
                       isNormalized(ret));
        }
    }

    //org.apache.type_test.types3.StructWithOccuringStruct;

    protected boolean equals(StructWithOccuringStruct x, StructWithOccuringStruct y) {
        if (x.getVarInteger() != y.getVarInteger()) {
            return false;
        }

        List<Comparable> xList = x.getVarIntAndVarFloat();
        List<Comparable> yList = y.getVarIntAndVarFloat();
        int xSize = (xList == null) ? 0 : xList.size();
        int ySize = (yList == null) ? 0 : yList.size();
        if (xSize != ySize) {
            return false;
        }
        for (int i = 0; i < xSize; ++i) {
            if (xList.get(i) instanceof Integer && yList.get(i) instanceof Integer) {
                if (((Integer)xList.get(i)).compareTo((Integer)yList.get(i)) != 0) {
                    return false;
                }
            } else if (xList.get(i) instanceof Float && yList.get(i) instanceof Float) {
                if (((Float)xList.get(i)).compareTo((Float)yList.get(i)) != 0) {
                    return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    protected boolean isNormalized(StructWithOccuringStruct x) {
        return x.getVarIntAndVarFloat() != null;
    }

    
    @Test
    public void testStructWithOccuringStruct() throws Exception {
        if (!shouldRunTest("StructWithOccuringStruct")) {
            return;
        }
        // Test 1
        //
        // x: sequence occurs twice
        // y: sequence doesn't occur (null holder object)
        //
        StructWithOccuringStruct x = new StructWithOccuringStruct();
        x.setVarInteger(100);
        x.getVarIntAndVarFloat().add(101);
        x.getVarIntAndVarFloat().add(101.5f);
        x.getVarIntAndVarFloat().add(102);
        x.getVarIntAndVarFloat().add(102.5f);

        StructWithOccuringStruct yOriginal = new StructWithOccuringStruct();
        yOriginal.setVarInteger(200);

        Holder<StructWithOccuringStruct> y = new Holder<StructWithOccuringStruct>(yOriginal);
        Holder<StructWithOccuringStruct> z = new Holder<StructWithOccuringStruct>();
        StructWithOccuringStruct ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringStruct(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringStruct(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithOccuringStruct(): Incorrect form for out param",
                       isNormalized(z.value));
        }

        // Test 2
        //
        // x: sequence occurs twice
        // y: sequence occurs once
        //
        yOriginal.getVarIntAndVarFloat().add(201);
        yOriginal.getVarIntAndVarFloat().add(202.5f);

        y = new Holder<StructWithOccuringStruct>(yOriginal);
        z = new Holder<StructWithOccuringStruct>();
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringStruct(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect return value",
                       equals(x, ret));
        }

    }

    @Test
    public void testStructWithOccuringStruct2() throws Exception {
        if (!shouldRunTest("StructWithOccuringStruct2")) {
            return;
        }
        StructWithOccuringStruct x = new StructWithOccuringStruct();
        x.setVarInteger(100);
        x.getVarIntAndVarFloat().add(101);
        x.getVarIntAndVarFloat().add(101.5f);
        x.getVarIntAndVarFloat().add(102);
        x.getVarIntAndVarFloat().add(102.5f);

        StructWithOccuringStruct yOriginal = new StructWithOccuringStruct();
        yOriginal.setVarInteger(200);

        Holder<StructWithOccuringStruct> y = new Holder<StructWithOccuringStruct>(yOriginal);
        Holder<StructWithOccuringStruct> z = new Holder<StructWithOccuringStruct>();
        StructWithOccuringStruct ret;

        // Test 3
        //
        // x: sequence occurs once
        // y: sequence occurs twice
        //
        y = new Holder<StructWithOccuringStruct>(x);
        x = yOriginal;
        yOriginal = y.value;
        z = new Holder<StructWithOccuringStruct>();
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringStruct(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringStruct(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect return value",
                       equals(x, ret));
        }

        // Test 4
        //
        // x: sequence doesn't occur (array of size 0)
        // y: sequence occurs twice
        //
        x.getVarIntAndVarFloat().clear();

        y = new Holder<StructWithOccuringStruct>(yOriginal);
        z = new Holder<StructWithOccuringStruct>();
        if (testDocLiteral) {
            ret = docClient.testStructWithOccuringStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithOccuringStruct(x, y, z);
        } else {
            ret = rpcClient.testStructWithOccuringStruct(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithOccuringStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect return value",
                       equals(x, ret));
            assertTrue("testStructWithOccuringStruct(): Incorrect form for inout param",
                       isNormalized(y.value));
            assertTrue("testStructWithOccuringStruct(): Incorrect return form",
                       isNormalized(ret));
        }

    }
    //org.apache.type_test.types1.AnonymousType;

    protected boolean equals(AnonymousType x, AnonymousType y) {
        return x.getFoo().getFoo().equals(y.getFoo().getFoo())
            && x.getFoo().getBar().equals(y.getFoo().getBar());
    }

    @Test
    public void testAnonymousType() throws Exception {
        if (!shouldRunTest("AnonymousType")) {
            return;
        }
        AnonymousType x = new AnonymousType();
        AnonymousType.Foo fx = new AnonymousType.Foo();
        fx.setFoo("hello");
        fx.setBar("there");
        x.setFoo(fx);

        AnonymousType yOrig = new AnonymousType();
        AnonymousType.Foo fy = new AnonymousType.Foo();
        fy.setFoo("good");
        fy.setBar("bye");
        yOrig.setFoo(fy);

        Holder<AnonymousType> y = new Holder<AnonymousType>(yOrig);
        Holder<AnonymousType> z = new Holder<AnonymousType>();

        AnonymousType ret;
        if (testDocLiteral) {
            ret = docClient.testAnonymousType(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testAnonymousType(x, y, z);
        } else {
            ret = rpcClient.testAnonymousType(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testAnonymousType(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testAnonymousType(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testAnonymousType(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types1.RecSeqB6918;

    protected boolean equals(RecSeqB6918 x, RecSeqB6918 y) {
        List<Object> xList = x.getNextSeqAndVarInt();
        List<Object> yList = y.getNextSeqAndVarInt();
        int size = xList.size();
        if (size != yList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            Object xo = xList.get(i);
            Object yo = yList.get(i);
            if (xo instanceof Integer) {
                if (yo instanceof Integer) {
                    if (((Integer)xo).compareTo((Integer)yo) != 0) {
                        return false;
                    }
                } else {
                    return false;
                }
            } else if (xo instanceof RecSeqB6918) {
                if (yo instanceof RecSeqB6918) {
                    return equals((RecSeqB6918)xo, (RecSeqB6918)yo);
                } else {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testRecSeqB6918() throws Exception {
        if (!shouldRunTest("RecSeqB6918")) {
            return;
        }
        RecSeqB6918 x = new RecSeqB6918();
        List<Object> theList = x.getNextSeqAndVarInt();
        theList.add(new Integer(6));
        theList.add(new RecSeqB6918());
        theList.add(new Integer(42));
        RecSeqB6918 yOrig = new RecSeqB6918();
        theList = yOrig.getNextSeqAndVarInt();
        theList.add(x);
        theList.add(new Integer(2));
        Holder<RecSeqB6918> y = new Holder<RecSeqB6918>(yOrig);
        Holder<RecSeqB6918> z = new Holder<RecSeqB6918>();

        RecSeqB6918 ret;
        if (testDocLiteral) {
            ret = docClient.testRecSeqB6918(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testRecSeqB6918(x, y, z);
        } else {
            ret = rpcClient.testRecSeqB6918(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testRecSeqB6918(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testRecSeqB6918(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testRecSeqB6918(): Incorrect return value", equals(x, ret));
        }
    }

    /* XXX - The DerivedChoiceBaseAll, DerivedStructBaseAll, DerivedAll* types
     *  result in an error creating the Schema object:
     *  cos-all-limited.1.2: An 'all' model group must appear in a particle with
     *  {min occurs} = {max occurs} = 1, and that particle must be part of a
     *  pair which constitutes the {content type} of a complex type definition.
     *
     
    //org.apache.type_test.types1.ComplexArray
     
    protected boolean equals(ComplexArray x, ComplexArray y) {
        List<DerivedAllBaseStruct> xx = x.getVarDerivedItem();
        List<DerivedAllBaseStruct> yy = y.getVarDerivedItem();
        if (xx.size() != yy.size()) {
            return false;
        }
        for (int i = 0; i < xx.size(); i++) {
            if (!equals(xx.get(i), yy.get(i))) {
                return false;
            }
        }
        return true;
    }

    @Test
    @Ignore
    public void testComplexArray() throws Exception {
        if (!shouldRunTest("ComplexArray")) {
            return;
        }
        DerivedChoiceBaseStruct xx = new DerivedChoiceBaseStruct();
        //Base
        xx.setVarFloat(3.14f);
        xx.setVarInt(new BigInteger("42"));
        xx.setVarString("BaseStruct-x");
        xx.setVarAttrString("BaseStructAttr-x");
        //Derived
        xx.setVarFloatExt(-3.14f);
        xx.setVarStringExt("DerivedAll-x");
        xx.setAttrString("DerivedAttr-x");

        DerivedAllBaseStruct yy = new DerivedAllBaseStruct();
        //Base
        yy.setVarFloat(-9.14f);
        yy.setVarInt(new BigInteger("10"));
        yy.setVarString("BaseStruct-y");
        yy.setVarAttrString("BaseStructAttr-y");
        //Derived
        yy.setVarFloatExt(1.414f);
        yy.setVarStringExt("DerivedAll-y");
        yy.setAttrString("DerivedAttr-y");

        ComplexArray x = new ComplexArray();
        x.getVarDerivedItem().add(xx);
        x.getVarDerivedItem().add(yy);

        ComplexArray yOrig = new ComplexArray();
        yOrig.getVarDerivedItem().add(yy);

        Holder<ComplexArray> y = new Holder<ComplexArray>(yOrig);
        Holder<ComplexArray> z = new Holder<ComplexArray>();
        ComplexArray ret;
        if (testDocLiteral) {
            ret = docClient.testComplexArray(x, y, z);
        } else {
            ret = rpcClient.testComplexArray(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testComplexArray(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testComplexArray(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testComplexArray(): Incorrect return value", equals(ret, x));
        }
    }
    
    //org.apache.type_test.types1.ComplexChoice

    protected boolean equals(ComplexChoice x, ComplexChoice y) {
        DerivedChoiceBaseComplex xx = x.getVarDerivedStruct();
        DerivedChoiceBaseComplex yy = y.getVarDerivedStruct();
        return (xx != null && yy != null && equals(xx, yy))
            || (x.getVarFloat() != null && y.getVarFloat() != null
                && x.getVarFloat().compareTo(y.getVarFloat()) == 0);
    }

    public void testComplexChoice() throws Exception {
        if (!shouldRunTest("ComplexChoice")) {
            return;
        }
        DerivedChoiceBaseComplex xx = new DerivedChoiceBaseComplex();
        //Base (Sequence)
        xx.setVarFloat(3.14f);
        xx.setVarInt(new BigInteger("42"));
        xx.setVarString("BaseSequence-x");
        xx.setVarAttrString("BaseStructAttr-x");
        //Derived (All)
        xx.setVarFloatExt(-3.14f);
        xx.setVarStringExt("DerivedAll-x");
        xx.setAttrString("DerivedAttr-x");
        //Most Derived (Choice)
        xx.setVarStringExtExt("MostDerivedChoice-x");
        xx.setAttrStringExtExt("MostDerivedAttr-x");

        ComplexChoice x = new ComplexChoice();
        x.setVarDerivedStruct(xx);

        ComplexChoice yOrig = new ComplexChoice();
        yOrig.setVarFloat(10.14f);

        Holder<ComplexChoice> y = new Holder<ComplexChoice>(yOrig);
        Holder<ComplexChoice> z = new Holder<ComplexChoice>();
        ComplexChoice ret;
        if (testDocLiteral) {
            ret = docClient.testComplexChoice(x, y, z);
        } else {
            ret = rpcClient.testComplexChoice(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testComplexChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testComplexChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testComplexChoice(): Incorrect return value", equals(ret, x));
        }
    }

    //org.apache.type_test.types1.ComplexStruct

    protected boolean equals(ComplexStruct x, ComplexStruct y) {
        return equals(x.getVarDerivedStruct(), y.getVarDerivedStruct())
            && Float.compare(x.getVarFloat(), y.getVarFloat()) == 0;
    }

    public void testComplexStruct() throws Exception {
        if (!shouldRunTest("ComplexStruct")) {
            return;
        }
        DerivedChoiceBaseComplex xx = new DerivedChoiceBaseComplex();
        //Base (Sequence)
        xx.setVarFloat(3.14f);
        xx.setVarInt(new BigInteger("42"));
        xx.setVarString("BaseSequence-x");
        xx.setVarAttrString("BaseStructAttr-x");
        //Derived (All)
        xx.setVarFloatExt(-3.14f);
        xx.setVarStringExt("DerivedAll-x");
        xx.setAttrString("DerivedAttr-x");
        //Most Derived (Choice)
        xx.setVarStringExtExt("MostDerivedChoice-x");
        xx.setAttrStringExtExt("MostDerivedAttr-x");

        ComplexStruct x = new ComplexStruct();
        x.setVarFloat(30.14f);
        x.setVarDerivedStruct(xx);

        DerivedChoiceBaseComplex yy = new DerivedChoiceBaseComplex();
        //Base
        yy.setVarFloat(-9.14f);
        yy.setVarInt(new BigInteger("10"));
        yy.setVarString("BaseSequence-y");
        yy.setVarAttrString("BaseStructAttr-y");
        //Derived
        yy.setVarFloatExt(1.414f);
        yy.setVarStringExt("DerivedAll-y");
        yy.setAttrString("DerivedAttr-y");
        //Most Derived
        yy.setVarFloatExtExt(19.144f);
        yy.setAttrStringExtExt("MostDerivedAttr-y");

        ComplexStruct yOrig = new ComplexStruct();
        yOrig.setVarFloat(10.14f);
        yOrig.setVarDerivedStruct(yy);

        Holder<ComplexStruct> y = new Holder<ComplexStruct>(yOrig);
        Holder<ComplexStruct> z = new Holder<ComplexStruct>();
        ComplexStruct ret;
        if (testDocLiteral) {
            ret = docClient.testComplexStruct(x, y, z);
        } else {
            ret = rpcClient.testComplexStruct(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testComplexStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testComplexStruct(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testComplexStruct(): Incorrect return value", equals(ret, x));
        }
    }
    
    //org.apache.type_test.types1.DerivedChoiceBaseComplex

    protected boolean equals(DerivedChoiceBaseComplex x, DerivedChoiceBaseComplex y) {
        return equals((DerivedAllBaseStruct)x, (DerivedAllBaseStruct)y)
            && ((x.getVarStringExtExt() != null && y.getVarStringExtExt() != null
                 && x.getVarStringExtExt().equals(y.getVarStringExtExt()))
            || (x.getVarFloatExtExt() != null && y.getVarFloatExtExt() != null
                 && x.getVarFloatExtExt().compareTo(y.getVarFloatExtExt()) == 0));
    }

    public void testDerivedChoiceBaseComplex() throws Exception {
        if (!shouldRunTest("DerivedChoiceBaseComplex")) {
            return;
        }
        DerivedChoiceBaseComplex x = new DerivedChoiceBaseComplex();
        //Base (Sequence)
        x.setVarFloat(3.14f);
        x.setVarInt(new BigInteger("42"));
        x.setVarString("BaseSequence-x");
        x.setVarAttrString("BaseStructAttr-x");
        //Derived (All)
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedAll-x");
        x.setAttrString("DerivedAttr-x");
        //Most Derived (Choice)
        x.setVarStringExtExt("MostDerivedChoice-x");
        x.setAttrStringExtExt("MostDerivedAttr-x");

        DerivedChoiceBaseComplex yOrig = new DerivedChoiceBaseComplex();
        //Base
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(new BigInteger("10"));
        yOrig.setVarString("BaseSequence-y");
        yOrig.setVarAttrString("BaseStructAttr-y");
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedAll-y");
        yOrig.setAttrString("DerivedAttr-y");
        //Most Derived
        yOrig.setVarFloatExtExt(19.144f);
        yOrig.setAttrStringExtExt("MostDerivedAttr-y");

        Holder<DerivedChoiceBaseComplex> y = new Holder<DerivedChoiceBaseComplex>(yOrig);
        Holder<DerivedChoiceBaseComplex> z = new Holder<DerivedChoiceBaseComplex>();
        DerivedChoiceBaseComplex ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedChoiceBaseComplex(x, y, z);
        } else {
            ret = rpcClient.testDerivedChoiceBaseComplex(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testDerivedChoiceBaseComplex(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedChoiceBaseComplex(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedChoiceBaseComplex(): Incorrect return value", equals(x, ret));
        }
    }
    
    //org.apache.type_test.types1.DerivedAllBaseAll

    protected boolean equals(DerivedAllBaseAll x, DerivedAllBaseAll y) {
        return equals((SimpleAll)x, (SimpleAll)y)
            && (Float.compare(x.getVarFloatExt(), y.getVarFloatExt()) == 0)
            && (x.getVarStringExt().equals(y.getVarStringExt()))
            && (x.getAttrString().equals(y.getAttrString()));
    }

    public void testDerivedAllBaseAll() throws Exception {
        if (!shouldRunTest("DerivedAllBaseAll")) {
            return;
        }
        DerivedAllBaseAll x = new DerivedAllBaseAll();
        //Base
        x.setVarFloat(3.14f);
        x.setVarInt(42);
        x.setVarString("BaseAll-x");
        x.setVarAttrString("BaseAllAttr-x");
        //Derived
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedAll-x");
        x.setAttrString("DerivedAttr-x");

        DerivedAllBaseAll yOrig = new DerivedAllBaseAll();
        //Base
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(10);
        yOrig.setVarString("BaseAll-y");
        yOrig.setVarAttrString("BaseAllAttr-y");
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedAll-y");
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedAllBaseAll> y = new Holder<DerivedAllBaseAll>(yOrig);
        Holder<DerivedAllBaseAll> z = new Holder<DerivedAllBaseAll>();

        DerivedAllBaseAll ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedAllBaseAll(x, y, z);
        } else {
            ret = rpcClient.testDerivedAllBaseAll(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedAllBaseAll(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedAllBaseAll(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedAllBaseAll(): Incorrect return value", equals(x, ret));
        }
    }
    
    //org.apache.type_test.types1.DerivedAllBaseChoice

    protected boolean equals(DerivedAllBaseChoice x, DerivedAllBaseChoice y) {
        return equals((SimpleChoice)x, (SimpleChoice)y)
            && Float.compare(x.getVarFloatExt(), y.getVarFloatExt()) == 0
            && x.getVarStringExt().equals(y.getVarStringExt())
            && x.getAttrString().equals(y.getAttrString());
    }

    public void testDerivedAllBaseChoice() throws Exception {
        if (!shouldRunTest("DerivedAllBaseChoice")) {
            return;
        }
        DerivedAllBaseChoice x = new DerivedAllBaseChoice();
        //Base
        x.setVarString("BaseChoice-x");
        //Derived
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedAll-x");
        x.setAttrString("DerivedAttr-x");

        DerivedAllBaseChoice yOrig = new DerivedAllBaseChoice();
        //Base
        yOrig.setVarFloat(-9.14f);
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedAll-y");
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedAllBaseChoice> y = new Holder<DerivedAllBaseChoice>(yOrig);
        Holder<DerivedAllBaseChoice> z = new Holder<DerivedAllBaseChoice>();

        DerivedAllBaseChoice ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedAllBaseChoice(x, y, z);
        } else {
            ret = rpcClient.testDerivedAllBaseChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedAllBaseChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedAllBaseChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedAllBaseChoice(): Incorrect return value", equals(x, ret));
        }
    }
    
    //org.apache.type_test.types1.DerivedAllBaseStruct

    protected boolean equals(DerivedAllBaseStruct x, DerivedAllBaseStruct y) {
        return equals((SimpleStruct)x, (SimpleStruct)y)
            && (x.getVarFloatExt() == y.getVarFloatExt())
            && (x.getVarStringExt().equals(y.getVarStringExt()))
            && (x.getAttrString().equals(y.getAttrString()));
    }

    public void testDerivedAllBaseStruct() throws Exception {
        if (!shouldRunTest("DerivedAllBaseStruct")) {
            return;
        }
        DerivedAllBaseStruct x = new DerivedAllBaseStruct();
        //Base
        x.setVarFloat(3.14f);
        x.setVarInt(new BigInteger("42"));
        x.setVarString("BaseStruct-x");
        x.setVarAttrString("BaseStructAttr-x");
        //Derived
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedAll-x");
        x.setAttrString("DerivedAttr-x");

        DerivedAllBaseStruct yOrig = new DerivedAllBaseStruct();
        //Base
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(new BigInteger("10"));
        yOrig.setVarString("BaseStruct-y");
        yOrig.setVarAttrString("BaseStructAttr-y");
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedAll-y");
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedAllBaseStruct> y = new Holder<DerivedAllBaseStruct>(yOrig);
        Holder<DerivedAllBaseStruct> z = new Holder<DerivedAllBaseStruct>();

        DerivedAllBaseStruct ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedAllBaseStruct(x, y, z);
        } else {
            ret = rpcClient.testDerivedAllBaseStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedAllBaseStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedAllBaseStruct(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedAllBaseStruct(): Incorrect return value",
                       equals(x, ret));
        }
    }
     
    //org.apache.type_test.types1.DerivedChoiceBaseAll

    protected boolean equals(DerivedChoiceBaseAll x, DerivedChoiceBaseAll y) {
        if (x.getVarStringExt() != null && y.getVarStringExt() != null
            && !x.getVarStringExt().equals(y.getVarStringExt())) {
            return false;
        } else if (x.getVarFloatExt() != null && y.getVarFloatExt() != null
            && x.getVarFloatExt().compareTo(y.getVarFloatExt()) != 0) {
            return false;
        }
        return equals((SimpleAll)x, (SimpleAll)y)
            && x.getAttrString().equals(y.getAttrString());
    }

    public void testDerivedChoiceBaseAll() throws Exception {
        if (!shouldRunTest("DerivedChoiceBaseAll")) {
            return;
        }
        DerivedChoiceBaseAll x = new DerivedChoiceBaseAll();
        //Base
        x.setVarFloat(3.14f);
        x.setVarInt(42);
        x.setVarString("BaseAll-x");
        x.setVarAttrString("BaseAllAttr-x");
        //Derived
        x.setVarStringExt("DerivedChoice-x");
        x.setAttrString("DerivedAttr-x");

        DerivedChoiceBaseAll yOrig = new DerivedChoiceBaseAll();
        //Base
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(10);
        yOrig.setVarString("BaseAll-y");
        yOrig.setVarAttrString("BaseAllAttr-y");
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedChoiceBaseAll> y = new Holder<DerivedChoiceBaseAll>(yOrig);
        Holder<DerivedChoiceBaseAll> z = new Holder<DerivedChoiceBaseAll>();

        DerivedChoiceBaseAll ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedChoiceBaseAll(x, y, z);
        } else {
            ret = rpcClient.testDerivedChoiceBaseAll(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedChoiceBaseAll(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedChoiceBaseAll(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedChoiceBaseAll(): Incorrect return value", equals(x, ret));
        }
    }
    
    //org.apache.type_test.types1.DerivedStructBaseAll

    protected boolean equals(DerivedStructBaseAll x, DerivedStructBaseAll y) {
        return equals((SimpleAll)x, (SimpleAll)y)
            && (Float.compare(x.getVarFloatExt(), y.getVarFloatExt()) == 0)
            && (x.getVarStringExt().equals(y.getVarStringExt()))
            && (x.getAttrString().equals(y.getAttrString()));
    }

    public void testDerivedStructBaseAll() throws Exception {
        if (!shouldRunTest("DerivedStructBaseAll")) {
            return;
        }
        DerivedStructBaseAll x = new DerivedStructBaseAll();
        //Base
        x.setVarFloat(3.14f);
        x.setVarInt(42);
        x.setVarString("BaseAll-x");
        x.setVarAttrString("BaseAllAttr-x");
        //Derived
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedStruct-x");
        x.setAttrString("DerivedAttr-x");

        DerivedStructBaseAll yOrig = new DerivedStructBaseAll();
        //Base
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(10);
        yOrig.setVarString("BaseAll-y");
        yOrig.setVarAttrString("BaseAllAttr-y");
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedStruct-y");
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedStructBaseAll> y = new Holder<DerivedStructBaseAll>(yOrig);
        Holder<DerivedStructBaseAll> z = new Holder<DerivedStructBaseAll>();

        DerivedStructBaseAll ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedStructBaseAll(x, y, z);
        } else {
            ret = rpcClient.testDerivedStructBaseAll(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedStructBaseAll(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedStructBaseAll(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedStructBaseAll(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types1.DerivedChoiceBaseSimpleContent

    protected void equals(String msg, DerivedChoiceBaseSimpleContent x,
            DerivedChoiceBaseSimpleContent y) throws Exception {
        equals(msg, (Document)x, (Document)y);
        assertEquals(msg, x.getAttrStringExt(), y.getAttrStringExt());
        if (x.getVarStringExt() != null) {
            assertNotNull(msg, y.getVarStringExt());               
            assertEquals(msg, x.getVarStringExt(), y.getVarStringExt());
            assertTrue(msg, x.getVarFloatExt() == y.getVarFloatExt());
        }
    }

    public void testDerivedChoiceBaseSimpleContent() throws Exception {
        if (!shouldRunTest("DerivedChoiceBaseSimpleContent")) {
            return;
        }
        DerivedChoiceBaseSimpleContent x = new DerivedChoiceBaseSimpleContent();
        //Base
        x.setID("Base-x");
        x.setValue("BART");
        //Derived
        x.setVarStringExt("DerivedChoice-x");
        x.setAttrStringExt("DerivedAttr-x");

        DerivedChoiceBaseSimpleContent yOrig = new DerivedChoiceBaseSimpleContent();
        //Base
        yOrig.setID("Base-y");
        yOrig.setValue("LISA");
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setAttrStringExt("DerivedAttr-y");

        Holder<DerivedChoiceBaseSimpleContent> y = new Holder<DerivedChoiceBaseSimpleContent>(yOrig);
        Holder<DerivedChoiceBaseSimpleContent> z = new Holder<DerivedChoiceBaseSimpleContent>();

        DerivedChoiceBaseSimpleContent ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedChoiceBaseSimpleContent(x, y, z);
        } else {
            ret = rpcClient.testDerivedChoiceBaseSimpleContent(x, y, z);
        }
        if (!perfTestOnly) {
            equals("testDerivedChoiceBaseSimpleContent(): Incorrect value for inout param", x, y.value);
            equals("testDerivedChoiceBaseSimpleContent(): Incorrect value for out param", yOrig, z.value);
            equals("testDerivedChoiceBaseSimpleContent(): Incorrect return value", x, ret);
        }
    }
    */

    //org.apache.type_test.types1.RestrictedStructBaseStruct;

    protected boolean equals(RestrictedStructBaseStruct x, RestrictedStructBaseStruct y) {
        return (x.getVarFloat() == y.getVarFloat())
            && (x.getVarInt().equals(y.getVarInt()))
            && (x.getVarAttrString().equals(y.getVarAttrString()));
    }

    @Test
    public void testRestrictedStructBaseStruct() throws Exception {
        if (!shouldRunTest("RestrictedStructBaseStruct")) {
            return;
        }
        RestrictedStructBaseStruct x = new RestrictedStructBaseStruct();
        x.setVarFloat(3.14f);
        x.setVarInt(new BigInteger("42"));
        x.setVarAttrString("BaseStructAttr-x");
        RestrictedStructBaseStruct yOrig = new RestrictedStructBaseStruct();
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(new BigInteger("10"));
        yOrig.setVarAttrString("BaseStructAttr-y");

        Holder<RestrictedStructBaseStruct> y = new Holder<RestrictedStructBaseStruct>(yOrig);
        Holder<RestrictedStructBaseStruct> z = new Holder<RestrictedStructBaseStruct>();

        RestrictedStructBaseStruct ret;
        if (testDocLiteral) {
            ret = docClient.testRestrictedStructBaseStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testRestrictedStructBaseStruct(x, y, z);
        } else {
            ret = rpcClient.testRestrictedStructBaseStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testRestrictedStructBaseStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testRestrictedStructBaseStruct(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testRestrictedStructBaseStruct(): Incorrect return value", equals(x, ret));
        }
    }
    
    //org.apache.type_test.types1.RestrictedAllBaseAll;

    protected boolean equals(RestrictedAllBaseAll x, RestrictedAllBaseAll y) {
        return (x.getVarFloat() == y.getVarFloat())
            && (x.getVarInt() == y.getVarInt())
            && (x.getVarAttrString().equals(y.getVarAttrString()));
    }

    @Test
    public void testRestrictedAllBaseAll() throws Exception {
        if (!shouldRunTest("RestrictedAllBaseAll")) {
            return;
        }
        RestrictedAllBaseAll x = new RestrictedAllBaseAll();
        x.setVarFloat(3.14f);
        x.setVarInt(42);
        x.setVarAttrString("BaseAllAttr-x");
        RestrictedAllBaseAll yOrig = new RestrictedAllBaseAll();
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(10);
        yOrig.setVarAttrString("BaseAllAttr-y");

        Holder<RestrictedAllBaseAll> y = new Holder<RestrictedAllBaseAll>(yOrig);
        Holder<RestrictedAllBaseAll> z = new Holder<RestrictedAllBaseAll>();

        RestrictedAllBaseAll ret;
        if (testDocLiteral) {
            ret = docClient.testRestrictedAllBaseAll(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testRestrictedAllBaseAll(x, y, z);
        } else {
            ret = rpcClient.testRestrictedAllBaseAll(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testRestrictedAllBaseAll(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testRestrictedAllBaseAll(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testRestrictedAllBaseAll(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types1.UnionWithStringList;

    @Test
    public void testUnionWithStringList() throws Exception {
        if (!shouldRunTest("UnionWithStringList")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<String> x = Arrays.asList("5");
            List<String> yOrig = Arrays.asList("I", "am", "SimpleList");

            // Invoke testUnionWithStringList
            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();
            List<String> ret = testDocLiteral ? docClient.testUnionWithStringList(x, y, z) 
                    : xmlClient.testUnionWithStringList(x, y, z);
            if (!perfTestOnly) {
                assertEquals("testUnionWithStringList(): Incorrect value for inout param",
                             x, y.value);
                assertEquals("testUnionWithStringList(): Incorrect value for out param",
                             yOrig, z.value);
                assertEquals("testUnionWithStringList(): Incorrect return value", x, ret);
            }
        } else {
            String[] x = {"5"};
            String[] yOrig = {"I", "am", "SimpleList"};

            Holder<String[]> y = new Holder<String[]>(yOrig);
            Holder<String[]> z = new Holder<String[]>();

            String[] ret = rpcClient.testUnionWithStringList(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testUnionWithStringList(): Incorrect value for inout param",
                           Arrays.equals(x, y.value));
                assertTrue("testUnionWithStringList(): Incorrect value for out param",
                           Arrays.equals(yOrig, z.value));
                assertTrue("testUnionWithStringList(): Incorrect return value",
                           Arrays.equals(x, ret));
            }
        }
    }

    //org.apache.type_test.types1.UnionWithStringListRestriction;

    @Test
    public void testUnionWithStringListRestriction() throws Exception {
        if (!shouldRunTest("UnionWithStringListRestriction")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<String> x = Arrays.asList("5");
            List<String> yOrig = Arrays.asList("I", "am", "SimpleList");

            // Invoke testUnionWithStringListRestriction
            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();
            List<String> ret = testDocLiteral ? docClient.testUnionWithStringListRestriction(x, y, z) 
                    : xmlClient.testUnionWithStringListRestriction(x, y, z);
            if (!perfTestOnly) {
                assertEquals("testUnionWithStringListRestriction(): Incorrect value for inout param",
                             x, y.value);
                assertEquals("testUnionWithStringListRestriction(): Incorrect value for out param",
                             yOrig, z.value);
                assertEquals("testUnionWithStringListRestriction(): Incorrect return value", x, ret);
            }
        } else {
            String[] x = {"5"};
            String[] yOrig = {"I", "am", "SimpleList"};

            Holder<String[]> y = new Holder<String[]>(yOrig);
            Holder<String[]> z = new Holder<String[]>();

            String[] ret = rpcClient.testUnionWithStringListRestriction(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testUnionWithStringListRestriction(): Incorrect value for inout param",
                           Arrays.equals(x, y.value));
                assertTrue("testUnionWithStringListRestriction(): Incorrect value for out param",
                           Arrays.equals(yOrig, z.value));
                assertTrue("testUnionWithStringListRestriction(): Incorrect return value",
                           Arrays.equals(x, ret));
            }
        }
    }

    //org.apache.type_test.types1.UnionWithAnonList;

    @Test
    public void testUnionWithAnonList() throws Exception {
        if (!shouldRunTest("UnionWithAnonList")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<String> x = Arrays.asList("5");
            // Need to specify valid floats according to schema lexical
            // representation, not java floats... to avoid validation error
            // with xerces and ibm jdk.
            //List<String> yOrig = Arrays.asList("0.5f", "1.5f", "2.5f");
            List<String> yOrig = Arrays.asList("-1E4", "1267.43233E12",
                "12.78e-2", "12", "-0", "INF");

            // Invoke testUnionWithAnonList
            Holder<List<String>> y = new Holder<List<String>>(yOrig);
            Holder<List<String>> z = new Holder<List<String>>();
            List<String> ret = testDocLiteral ? docClient.testUnionWithAnonList(x, y, z) 
                    : xmlClient.testUnionWithAnonList(x, y, z);
            if (!perfTestOnly) {
                assertEquals("testUnionWithAnonList(): Incorrect value for inout param", x, y.value);
                assertEquals("testUnionWithAnonList(): Incorrect value for out param", yOrig, z.value);
                assertEquals("testUnionWithAnonList(): Incorrect return value", x, ret);
            }
        } else {
            String[] x = {"5"};
            // Use consistent values as above...
            //String[] yOrig = {"0.5f", "1.5f", "2.5f"};
            String[] yOrig = {"-1E4", "1267.43233E12", "12.78e-2", "12", "-0", "INF"};

            Holder<String[]> y = new Holder<String[]>(yOrig);
            Holder<String[]> z = new Holder<String[]>();

            String[] ret = rpcClient.testUnionWithStringListRestriction(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testUnionWithAnonList(): Incorrect value for inout param",
                           Arrays.equals(x, y.value));
                assertTrue("testUnionWithAnonList(): Incorrect value for out param",
                           Arrays.equals(yOrig, z.value));
                assertTrue("testUnionWithAnonList(): Incorrect return value",
                           Arrays.equals(x, ret));
            }
        }
    }

    @Test
    public void testAnyURIRestriction() throws Exception {
        if (!shouldRunTest("AnyURIRestriction")) {
            return;
        }
        // normal case, maxLength = 50 for anyURI
        String x = new String("http://cxf.apache.org/");
        String yOrig = new String("http://www.iona.com/info/services/oss/");
        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();

        String ret;
        if (testDocLiteral) {
            ret = docClient.testAnyURIRestriction(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testAnyURIRestriction(x, y, z);
        } else {
            ret = rpcClient.testAnyURIRestriction(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testString(): Incorrect value for inout param", x, y.value);
            assertEquals("testString(): Incorrect value for out param", yOrig, z.value);
            assertEquals("testString(): Incorrect return value", x, ret);
        }

        if (testDocLiteral || testXMLBinding) {
            // abnormal case
            yOrig = new String("http://www.iona.com/info/services/oss/info_services_oss_train.html");
            y = new Holder<String>(yOrig);
            z = new Holder<String>();
            try {
                ret = testDocLiteral ? docClient.testAnyURIRestriction(x, y, z) 
                        : xmlClient.testAnyURIRestriction(x, y, z);
                fail("maxLength=50 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    // Test Inheritance

    // test internal inheritance
    @Test
    public void testInheritanceNestedStruct() throws Exception {
        if (!shouldRunTest("InheritanceNestedStruct")) {
            return;
        }
        DerivedStructBaseStruct xs = new DerivedStructBaseStruct();
        //Base
        xs.setVarFloat(3.14f);
        xs.setVarInt(new BigInteger("42"));
        xs.setVarString("BaseStruct-x");
        xs.setVarAttrString("BaseStructAttr-x");
        //Derived
        xs.setVarFloatExt(-3.14f);
        xs.setVarStringExt("DerivedStruct-x");
        xs.setAttrString1("DerivedAttr1-x");
        xs.setAttrString2("DerivedAttr2-x");

        DerivedStructBaseStruct ys = new DerivedStructBaseStruct();
        //Base
        ys.setVarFloat(-9.14f);
        ys.setVarInt(new BigInteger("10"));
        ys.setVarString("BaseStruct-y");
        ys.setVarAttrString("BaseStructAttr-y");
        //Derived
        ys.setVarFloatExt(1.414f);
        ys.setVarStringExt("DerivedStruct-y");
        ys.setAttrString1("DerivedAttr1-y");
        ys.setAttrString2("DerivedAttr2-y");

        NestedStruct x = new NestedStruct();
        x.setVarFloat(new BigDecimal("3.14"));
        x.setVarInt(42);
        x.setVarString("Hello There");
        x.setVarEmptyStruct(new EmptyStruct());
        x.setVarStruct(xs);

        NestedStruct yOrig = new NestedStruct();
        yOrig.setVarFloat(new BigDecimal("1.414"));
        yOrig.setVarInt(13);
        yOrig.setVarString("Cheerio");
        yOrig.setVarEmptyStruct(new EmptyStruct());
        yOrig.setVarStruct(ys);
        Holder<NestedStruct> y = new Holder<NestedStruct>(yOrig);
        Holder<NestedStruct> z = new Holder<NestedStruct>();

        NestedStruct ret;
        if (testDocLiteral) {
            ret = docClient.testNestedStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testNestedStruct(x, y, z);
        } else {
            ret = rpcClient.testNestedStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testNestedStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testNestedStruct(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testNestedStruct(): Incorrect return value", equals(x, ret));
        }
    }

    // test first level inheritance (parameters)
    @Test
    public void testInheritanceSimpleStructDerivedStruct() throws Exception {
        if (!shouldRunTest("InheritanceSimpleStructDerivedStruct")) {
            return;
        }
        DerivedStructBaseStruct x = new DerivedStructBaseStruct();
        //Base
        x.setVarFloat(3.14f);
        x.setVarInt(new BigInteger("42"));
        x.setVarString("BaseStruct-x");
        x.setVarAttrString("BaseStructAttr-x");
        //Derived
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedStruct-x");
        x.setAttrString1("DerivedAttr1-x");
        x.setAttrString2("DerivedAttr2-x");

        DerivedStructBaseStruct yOrig = new DerivedStructBaseStruct();
        //Base
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(new BigInteger("10"));
        yOrig.setVarString("BaseStruct-y");
        yOrig.setVarAttrString("BaseStructAttr-y");
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedStruct-y");
        yOrig.setAttrString1("DerivedAttr1-y");
        yOrig.setAttrString2("DerivedAttr2-y");

        Holder<SimpleStruct> y = new Holder<SimpleStruct>(yOrig);
        Holder<SimpleStruct> z = new Holder<SimpleStruct>();

        SimpleStruct ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleStruct(x, y, z); 
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleStruct(x, y, z);
        } else {
            ret = rpcClient.testSimpleStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testInheritanceSimpleDerived(): Incorrect value for inout param",
                       equals(x, (DerivedStructBaseStruct)y.value));
            assertTrue("testInheritanceSimpleDerived(): Incorrect value for out param",
                       equals(yOrig, (DerivedStructBaseStruct)z.value));
            assertTrue("testInheritanceSimpleDerived(): Incorrect return value",
                       equals(x, (DerivedStructBaseStruct)ret));
        }
    }

    @Test
    public void testInheritanceSimpleChoiceDerivedStruct() throws Exception {
        if (!shouldRunTest("InheritanceSimpleChoiceDerivedStruct")) {
            return;
        }
        DerivedStructBaseChoice x = new DerivedStructBaseChoice();
        //Base
        x.setVarString("BaseChoice-x");
        //Derived
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedStruct-x");
        x.setAttrString("DerivedAttr-x");

        DerivedStructBaseChoice yOrig = new DerivedStructBaseChoice();
        //Base
        yOrig.setVarFloat(-9.14f);
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedStruct-y");
        yOrig.setAttrString("DerivedAttr-y");

        Holder<SimpleChoice> y = new Holder<SimpleChoice>(yOrig);
        Holder<SimpleChoice> z = new Holder<SimpleChoice>();

        SimpleChoice ret;
        if (testDocLiteral) {
            ret = docClient.testSimpleChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSimpleChoice(x, y, z);
        } else {
            ret = rpcClient.testSimpleChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testInheritanceSimpleChoiceDerivedStruct(): Incorrect value for inout param",
                       equals(x, (DerivedStructBaseChoice)y.value));
            assertTrue("testInheritanceSimpleChoiceDerivedStruct(): Incorrect value for out param",
                       equals(yOrig, (DerivedStructBaseChoice)z.value));
            assertTrue("testInheritanceSimpleChoiceDerivedStruct(): Incorrect return value",
                       equals(x, (DerivedStructBaseChoice)ret));
        }
    }

    @Test
    public void testInheritanceUnboundedArrayDerivedChoice() throws Exception {
        if (!shouldRunTest("InheritanceUnboundedArrayDerivedChoice")) {
            return;
        }
        DerivedChoiceBaseArray x = new DerivedChoiceBaseArray();
        //Base
        x.getItem().addAll(Arrays.asList("AAA", "BBB", "CCC"));
        //Derived
        x.setVarStringExt("DerivedChoice-x");
        x.setAttrStringExt("DerivedAttr-x");

        DerivedChoiceBaseArray yOrig = new DerivedChoiceBaseArray();
        //Base
        yOrig.getItem().addAll(Arrays.asList("XXX", "YYY", "ZZZ"));
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setAttrStringExt("DerivedAttr-y");

        Holder<UnboundedArray> y = new Holder<UnboundedArray>(yOrig);
        Holder<UnboundedArray> z = new Holder<UnboundedArray>();
        UnboundedArray ret;
        if (testDocLiteral) {
            ret = docClient.testUnboundedArray(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testUnboundedArray(x, y, z);
        } else {
            ret = rpcClient.testUnboundedArray(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testInheritanceUnboundedArrayDerivedChoice(): Incorrect value for inout param",
                       equals(x, (DerivedChoiceBaseArray)y.value));
            assertTrue("testInheritanceUnboundedArrayDerivedChoice(): Incorrect value for out param",
                       equals(yOrig, (DerivedChoiceBaseArray)z.value));
            assertTrue("testInheritanceUnboundedArrayDerivedChoice(): Incorrect return value",
                       equals(x, (DerivedChoiceBaseArray)ret));
        }
    }

    @Test
    public void testInheritanceEmptyAllDerivedEmpty() throws Exception {
        if (!shouldRunTest("InheritanceEmptyAllDerivedEmpty")) {
            return;
        }
        DerivedEmptyBaseEmptyAll x = new DerivedEmptyBaseEmptyAll();
        DerivedEmptyBaseEmptyAll yOrig = new DerivedEmptyBaseEmptyAll();
        Holder<EmptyAll> y = new Holder<EmptyAll>(yOrig);
        Holder<EmptyAll> z = new Holder<EmptyAll>();

        EmptyAll ret;
        if (testDocLiteral) {
            ret = docClient.testEmptyAll(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testEmptyAll(x, y, z);
        } else {
            ret = rpcClient.testEmptyAll(x, y, z);
        }
        assertNotNull("testInheritanceEmptyAllDerivedEmpty()", y.value);
        assertNotNull("testInheritanceEmptyAllDerivedEmpty()", z.value);
        assertNotNull("testInheritanceEmptyAllDerivedEmpty()", ret);
        
        assertTrue(y.value.getClass().getName(), y.value instanceof DerivedEmptyBaseEmptyAll);
        assertTrue(z.value.getClass().getName(), z.value instanceof DerivedEmptyBaseEmptyAll);
        assertTrue(ret.getClass().getName(), ret instanceof DerivedEmptyBaseEmptyAll);
    }
}
