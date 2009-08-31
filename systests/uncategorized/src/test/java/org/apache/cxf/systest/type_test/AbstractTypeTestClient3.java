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
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.soap.Name;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPFactory;
import javax.xml.ws.Holder;

import org.w3c.dom.Element;

import org.apache.type_test.types1.DerivedChoiceBaseArray;
import org.apache.type_test.types1.DerivedChoiceBaseChoice;
import org.apache.type_test.types1.DerivedChoiceBaseStruct;
import org.apache.type_test.types1.DerivedEmptyBaseEmptyAll;
import org.apache.type_test.types1.DerivedEmptyBaseEmptyChoice;
import org.apache.type_test.types1.DerivedNoContent;
import org.apache.type_test.types1.DerivedStructBaseChoice;
import org.apache.type_test.types1.DerivedStructBaseEmpty;
import org.apache.type_test.types1.DerivedStructBaseStruct;
import org.apache.type_test.types1.RestrictedChoiceBaseChoice;
import org.apache.type_test.types1.SimpleChoice;
import org.apache.type_test.types1.SimpleStruct;
import org.apache.type_test.types1.UnboundedArray;
import org.apache.type_test.types2.ChoiceOfChoice;
import org.apache.type_test.types2.ChoiceOfSeq;
import org.apache.type_test.types2.ChoiceWithAnyAttribute;
import org.apache.type_test.types2.ChoiceWithBinary;
import org.apache.type_test.types2.ChoiceWithGroupChoice;
import org.apache.type_test.types2.ChoiceWithGroupSeq;
import org.apache.type_test.types2.ChoiceWithGroups;
import org.apache.type_test.types2.ComplexTypeWithAttributeGroup;
import org.apache.type_test.types2.ComplexTypeWithAttributeGroup1;
import org.apache.type_test.types2.ComplexTypeWithAttributes;
import org.apache.type_test.types2.ExtBase64Binary;
import org.apache.type_test.types2.GroupDirectlyInComplexType;
import org.apache.type_test.types2.IDTypeAttribute;
import org.apache.type_test.types2.MultipleOccursSequenceInSequence;
import org.apache.type_test.types2.SequenceWithGroupChoice;
import org.apache.type_test.types2.SequenceWithGroupSeq;
import org.apache.type_test.types2.SequenceWithGroups;
import org.apache.type_test.types2.SequenceWithOccuringGroup;
import org.apache.type_test.types2.StructWithAny;
import org.apache.type_test.types2.StructWithAnyArray;
import org.apache.type_test.types2.StructWithAnyAttribute;
import org.apache.type_test.types2.StructWithBinary;
import org.apache.type_test.types3.OccuringChoice;
import org.apache.type_test.types3.OccuringChoice1;
import org.apache.type_test.types3.OccuringChoice2;
import org.apache.type_test.types3.OccuringStruct;
import org.apache.type_test.types3.OccuringStruct1;
import org.apache.type_test.types3.OccuringStruct2;
import org.junit.Ignore;
import org.junit.Test;

public abstract class AbstractTypeTestClient3 extends AbstractTypeTestClient2 {

 
    protected boolean equals(ChoiceOfChoice x, ChoiceOfChoice y) {
        if (x.getVarInt() != null && y.getVarInt() != null) {
            return x.getVarInt().equals(y.getVarInt());
        }
        if (x.getVarFloat() != null && y.getVarFloat() != null) {
            return x.getVarFloat().equals(y.getVarFloat());
        }
        if (x.getVarOtherInt() != null && y.getVarOtherInt() != null) {
            return x.getVarOtherInt().equals(y.getVarOtherInt());
        }
        if (x.getVarString() != null && y.getVarString() != null) {
            return x.getVarString().equals(y.getVarString());
        }
        return false;
    }

    @Test
    public void testChoiceOfChoice() throws Exception {
        if (!shouldRunTest("ChoiceOfChoice")) {
            return;
        }
        ChoiceOfChoice x = new ChoiceOfChoice();
        ChoiceOfChoice yOrig = new ChoiceOfChoice();
        x.setVarFloat(3.14f);
        yOrig.setVarString("y456");

        Holder<ChoiceOfChoice> y = new Holder<ChoiceOfChoice>(yOrig);
        Holder<ChoiceOfChoice> z = new Holder<ChoiceOfChoice>();

        ChoiceOfChoice ret;
        if (testDocLiteral) {
            ret = docClient.testChoiceOfChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testChoiceOfChoice(x, y, z);
        } else {
            ret = rpcClient.testChoiceOfChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testChoiceOfChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testChoiceOfChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testChoiceOfChoice(): Incorrect return value", equals(x, ret));
        }
    }

    protected boolean equals(ChoiceOfSeq x, ChoiceOfSeq y) {
        if (x.getVarFloat() != null && x.getVarInt() != null 
            && y.getVarFloat() != null && y.getVarInt() != null) {
            return x.getVarInt().equals(y.getVarInt())
                && x.getVarFloat().compareTo(y.getVarFloat()) == 0;
        }
        if (x.getVarOtherInt() != null && y.getVarOtherInt() != null 
            && x.getVarString() != null && y.getVarString() != null) {
            return x.getVarOtherInt().equals(y.getVarOtherInt())
                && x.getVarString().equals(y.getVarString());
        }
        return false;
    }

    @Test
    public void testChoiceOfSeq() throws Exception {
        if (!shouldRunTest("ChoiceOfSeq")) {
            return;
        }
        ChoiceOfSeq x = new ChoiceOfSeq();
        x.setVarInt(123);
        x.setVarFloat(3.14f);

        ChoiceOfSeq yOrig = new ChoiceOfSeq();
        yOrig.setVarOtherInt(456);
        yOrig.setVarString("y456");

        Holder<ChoiceOfSeq> y = new Holder<ChoiceOfSeq>(yOrig);
        Holder<ChoiceOfSeq> z = new Holder<ChoiceOfSeq>();

        ChoiceOfSeq ret;
        if (testDocLiteral) {
            ret = docClient.testChoiceOfSeq(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testChoiceOfSeq(x, y, z);
        } else {
            ret = rpcClient.testChoiceOfSeq(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testChoiceOfSeq(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testChoiceOfSeq(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testChoiceOfSeq(): Incorrect return value", equals(x, ret));
        }
    }

    protected boolean equals(DerivedStructBaseStruct x, DerivedStructBaseStruct y) {
        return equals((SimpleStruct)x, (SimpleStruct)y)
            && (x.getVarFloatExt() == y.getVarFloatExt())
            && (x.getVarStringExt().equals(y.getVarStringExt()))
            && (x.getAttrString1().equals(y.getAttrString1()))
            && (x.getAttrString2().equals(y.getAttrString2()));
    }

    @Test
    public void testDerivedStructBaseStruct() throws Exception {
        if (!shouldRunTest("DerivedStructBaseStruct")) {
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

        Holder<DerivedStructBaseStruct> y = new Holder<DerivedStructBaseStruct>(yOrig);
        Holder<DerivedStructBaseStruct> z = new Holder<DerivedStructBaseStruct>();
        DerivedStructBaseStruct ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedStructBaseStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedStructBaseStruct(x, y, z);
        } else {
            ret = rpcClient.testDerivedStructBaseStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedStructBaseStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedStructBaseStruct(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedStructBaseStruct(): Incorrect return value", equals(x, ret));
        }
    }
    
    protected boolean equals(DerivedStructBaseChoice x, DerivedStructBaseChoice y) {
        return equals((SimpleChoice)x, (SimpleChoice)y)
            && (Float.compare(x.getVarFloatExt(), y.getVarFloatExt()) == 0)
            && (x.getVarStringExt().equals(y.getVarStringExt()))
            && (x.getAttrString().equals(y.getAttrString()));
    }

    @Test
    public void testDerivedStructBaseChoice() throws Exception {
        if (!shouldRunTest("DerivedStructBaseChoice")) {
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

        Holder<DerivedStructBaseChoice> y = new Holder<DerivedStructBaseChoice>(yOrig);
        Holder<DerivedStructBaseChoice> z = new Holder<DerivedStructBaseChoice>();
        DerivedStructBaseChoice ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedStructBaseChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedStructBaseChoice(x, y, z);
        } else {
            ret = rpcClient.testDerivedStructBaseChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedStructBaseChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedStructBaseChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedStructBaseChoice(): Incorrect return value", equals(x, ret));
        }
    }

    protected boolean equals(DerivedChoiceBaseStruct x, DerivedChoiceBaseStruct y) {
        boolean isEquals = x.getAttrString().equals(y.getAttrString());
        if (x.getVarStringExt() != null && y.getVarStringExt() != null) {
            isEquals &= x.getVarStringExt().equals(y.getVarStringExt());
        } else {
            isEquals &= x.getVarFloatExt() != null && y.getVarFloatExt() != null
                && x.getVarFloatExt().compareTo(y.getVarFloatExt()) == 0;
        }
        return isEquals && equals((SimpleStruct)x, (SimpleStruct)y);
    }

    @Test
    public void testDerivedChoiceBaseStruct() throws Exception {
        if (!shouldRunTest("DerivedChoiceBaseStruct")) {
            return;
        }
        DerivedChoiceBaseStruct x = new DerivedChoiceBaseStruct();
        //Base
        x.setVarFloat(3.14f);
        x.setVarInt(new BigInteger("42"));
        x.setVarString("BaseStruct-x");
        x.setVarAttrString("BaseStructAttr-x");
        //Derived
        x.setVarStringExt("DerivedChoice-x");
        x.setAttrString("DerivedAttr-x");

        DerivedChoiceBaseStruct yOrig = new DerivedChoiceBaseStruct();
        // Base
        yOrig.setVarFloat(-9.14f);
        yOrig.setVarInt(new BigInteger("10"));
        yOrig.setVarString("BaseStruct-y");
        yOrig.setVarAttrString("BaseStructAttr-y");
        // Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedChoiceBaseStruct> y = new Holder<DerivedChoiceBaseStruct>(yOrig);
        Holder<DerivedChoiceBaseStruct> z = new Holder<DerivedChoiceBaseStruct>();
        DerivedChoiceBaseStruct ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedChoiceBaseStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedChoiceBaseStruct(x, y, z);
        } else {
            ret = rpcClient.testDerivedChoiceBaseStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedChoiceBaseStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedChoiceBaseStruct(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedChoiceBaseStruct(): Incorrect return value", equals(x, ret));
        }
    }
    
    //org.apache.type_test.types1.DerivedChoiceBaseArray

    protected boolean equals(DerivedChoiceBaseArray x, DerivedChoiceBaseArray y) {
        boolean isEquals = x.getAttrStringExt().equals(y.getAttrStringExt());
        if (x.getVarFloatExt() != null && y.getVarFloatExt() != null) {
            isEquals &= x.getVarFloatExt().compareTo(y.getVarFloatExt()) == 0;
        } else {
            isEquals &= x.getVarStringExt() != null && y.getVarStringExt() != null
                && x.getVarStringExt().equals(y.getVarStringExt());
        }
        return isEquals && equals((UnboundedArray)x, (UnboundedArray)y);
    }

    @Test
    public void testDerivedChoiceBaseArray() throws Exception {
        if (!shouldRunTest("DerivedChoiceBaseArray")) {
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

        Holder<DerivedChoiceBaseArray> y = new Holder<DerivedChoiceBaseArray>(yOrig);
        Holder<DerivedChoiceBaseArray> z = new Holder<DerivedChoiceBaseArray>();
        DerivedChoiceBaseArray ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedChoiceBaseArray(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedChoiceBaseArray(x, y, z);
        } else {
            ret = rpcClient.testDerivedChoiceBaseArray(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedChoiceBaseArray(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedChoiceBaseArray(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedChoiceBaseArray(): Incorrect return value",
                       equals(ret, x));
        }
    }

    //org.apache.type_test.types1.DerivedChoiceBaseChoice
    
    protected boolean equals(DerivedChoiceBaseChoice x, DerivedChoiceBaseChoice y) {
        boolean isEquals = x.getAttrString().equals(y.getAttrString());
        if (x.getVarStringExt() != null && y.getVarStringExt() != null) {
            isEquals &= x.getVarStringExt().equals(y.getVarStringExt());
        } else {
            isEquals &= x.getVarFloatExt() != null && y.getVarFloatExt() != null
                && x.getVarFloatExt().compareTo(y.getVarFloatExt()) == 0;
        }
        return isEquals && equals((SimpleChoice)x, (SimpleChoice)y);
    }

    @Test
    public void testDerivedChoiceBaseChoice() throws Exception {
        if (!shouldRunTest("DerivedChoiceBaseChoice")) {
            return;
        }
        DerivedChoiceBaseChoice x = new DerivedChoiceBaseChoice();
        //Base
        x.setVarString("BaseChoice-x");
        //Derived
        x.setVarStringExt("DerivedChoice-x");
        x.setAttrString("DerivedAttr-x");

        DerivedChoiceBaseChoice yOrig = new DerivedChoiceBaseChoice();
        //Base
        yOrig.setVarFloat(-9.14f);
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedChoiceBaseChoice> y = new Holder<DerivedChoiceBaseChoice>(yOrig);
        Holder<DerivedChoiceBaseChoice> z = new Holder<DerivedChoiceBaseChoice>();
        DerivedChoiceBaseChoice ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedChoiceBaseChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedChoiceBaseChoice(x, y, z);
        } else {
            ret = rpcClient.testDerivedChoiceBaseChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedChoiceBaseChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedChoiceBaseChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedChoiceBaseChoice(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types1.DerivedNoContent

    protected boolean equals(DerivedNoContent x, DerivedNoContent y) {
        return equals((SimpleStruct)x, (SimpleStruct)y)
            && x.getVarAttrString().equals(y.getVarAttrString());
    }

    @Test
    public void testDerivedNoContent() throws Exception {
        if (!shouldRunTest("DerivedNoContent")) {
            return;
        }
        DerivedNoContent x = new DerivedNoContent();
        x.setVarFloat(3.14f);
        x.setVarInt(new BigInteger("42"));
        x.setVarString("BaseStruct-x");
        x.setVarAttrString("BaseStructAttr-x");

        DerivedNoContent yOrig = new DerivedNoContent();
        yOrig.setVarFloat(1.414f);
        yOrig.setVarInt(new BigInteger("13"));
        yOrig.setVarString("BaseStruct-y");
        yOrig.setVarAttrString("BaseStructAttr-y");

        Holder<DerivedNoContent> y = new Holder<DerivedNoContent>(yOrig);
        Holder<DerivedNoContent> z = new Holder<DerivedNoContent>();
        DerivedNoContent ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedNoContent(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedNoContent(x, y, z);
        } else {
            ret = rpcClient.testDerivedNoContent(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedNoContent(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedNoContent(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedNoContent(): Incorrect return value", equals(ret, x));
        }
    }

    //org.apache.type_test.types1.DerivedStructBaseEmpty

    protected boolean equals(DerivedStructBaseEmpty x, DerivedStructBaseEmpty y) {
        return (x.getVarFloatExt() == y.getVarFloatExt())
            && (x.getVarStringExt().equals(y.getVarStringExt()))
            && (x.getAttrString().equals(y.getAttrString()));
    }

    @Test
    public void testDerivedStructBaseEmpty() throws Exception {
        if (!shouldRunTest("DerivedStructBaseEmpty")) {
            return;
        }
        DerivedStructBaseEmpty x = new DerivedStructBaseEmpty();
        //Derived
        x.setVarFloatExt(-3.14f);
        x.setVarStringExt("DerivedStruct-x");
        x.setAttrString("DerivedAttr-x");

        DerivedStructBaseEmpty yOrig = new DerivedStructBaseEmpty();
        //Derived
        yOrig.setVarFloatExt(1.414f);
        yOrig.setVarStringExt("DerivedStruct-y");
        yOrig.setAttrString("DerivedAttr-y");

        Holder<DerivedStructBaseEmpty> y = new Holder<DerivedStructBaseEmpty>(yOrig);
        Holder<DerivedStructBaseEmpty> z = new Holder<DerivedStructBaseEmpty>();
        DerivedStructBaseEmpty ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedStructBaseEmpty(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedStructBaseEmpty(x, y, z);
        } else {
            ret = rpcClient.testDerivedStructBaseEmpty(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testDerivedStructBaseEmpty(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testDerivedStructBaseEmpty(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testDerivedStructBaseEmpty(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types1.DerivedEmptyBaseEmptyAll

    @Test
    public void testDerivedEmptyBaseEmptyAll() throws Exception {
        if (!shouldRunTest("DerivedEmptyBaseEmptyAll")) {
            return;
        }
        DerivedEmptyBaseEmptyAll x = new DerivedEmptyBaseEmptyAll();
        DerivedEmptyBaseEmptyAll yOrig = new DerivedEmptyBaseEmptyAll();
        Holder<DerivedEmptyBaseEmptyAll> y = new Holder<DerivedEmptyBaseEmptyAll>(yOrig);
        Holder<DerivedEmptyBaseEmptyAll> z = new Holder<DerivedEmptyBaseEmptyAll>();
        DerivedEmptyBaseEmptyAll ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedEmptyBaseEmptyAll(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedEmptyBaseEmptyAll(x, y, z);
        } else {
            ret = rpcClient.testDerivedEmptyBaseEmptyAll(x, y, z);
        }
        // not much to check
        assertNotNull(y.value);
        assertNotNull(z.value);
        assertNotNull(ret);
        
        assertTrue(y.value instanceof DerivedEmptyBaseEmptyAll);
        assertTrue(z.value instanceof DerivedEmptyBaseEmptyAll);
        assertTrue(ret instanceof DerivedEmptyBaseEmptyAll);
    }

    //org.apache.type_test.types1.DerivedEmptyBaseEmptyChoice

    @Test
    public void testDerivedEmptyBaseEmptyChoice() throws Exception {
        if (!shouldRunTest("DerivedEmptyBaseEmptyChoice")) {
            return;
        }
        DerivedEmptyBaseEmptyChoice x = new DerivedEmptyBaseEmptyChoice();
        DerivedEmptyBaseEmptyChoice yOrig = new DerivedEmptyBaseEmptyChoice();
        Holder<DerivedEmptyBaseEmptyChoice> y = new Holder<DerivedEmptyBaseEmptyChoice>(yOrig);
        Holder<DerivedEmptyBaseEmptyChoice> z = new Holder<DerivedEmptyBaseEmptyChoice>();
        DerivedEmptyBaseEmptyChoice ret;
        if (testDocLiteral) {
            ret = docClient.testDerivedEmptyBaseEmptyChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testDerivedEmptyBaseEmptyChoice(x, y, z);
        } else {
            ret = rpcClient.testDerivedEmptyBaseEmptyChoice(x, y, z);
        }
        // not much to check
        assertNotNull(y.value);
        assertNotNull(z.value);
        assertNotNull(ret);
        
        assertTrue(y.value instanceof DerivedEmptyBaseEmptyChoice);
        assertTrue(z.value instanceof DerivedEmptyBaseEmptyChoice);
        assertTrue(ret instanceof DerivedEmptyBaseEmptyChoice);
    }

    //org.apache.type_test.types1.RestrictedChoiceBaseChoice

    protected boolean equals(RestrictedChoiceBaseChoice x, RestrictedChoiceBaseChoice y) {
        if (x.getVarFloat() != null && y.getVarFloat() != null) {
            return x.getVarFloat().compareTo(y.getVarFloat()) == 0;
        } else {
            return x.getVarInt() != null && y.getVarInt() != null
                && x.getVarInt().compareTo(y.getVarInt()) == 0;
        }
    }

    @Test
    public void testRestrictedChoiceBaseChoice() throws Exception {
        if (!shouldRunTest("RestrictedChoiceBaseChoice")) {
            return;
        }
        RestrictedChoiceBaseChoice x = new RestrictedChoiceBaseChoice();
        x.setVarInt(12);

        RestrictedChoiceBaseChoice yOrig = new RestrictedChoiceBaseChoice();
        yOrig.setVarFloat(-9.14f);

        Holder<RestrictedChoiceBaseChoice> y = new Holder<RestrictedChoiceBaseChoice>(yOrig);
        Holder<RestrictedChoiceBaseChoice> z = new Holder<RestrictedChoiceBaseChoice>();

        RestrictedChoiceBaseChoice ret;
        if (testDocLiteral) {
            ret = docClient.testRestrictedChoiceBaseChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testRestrictedChoiceBaseChoice(x, y, z);
        } else {
            ret = rpcClient.testRestrictedChoiceBaseChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testRestrictedChoiceBaseChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testRestrictedChoiceBaseChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testRestrictedChoiceBaseChoice(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types2.ComplexTypeWithAttributeGroup

    protected boolean equals(ComplexTypeWithAttributeGroup x,
                             ComplexTypeWithAttributeGroup y) {
        return x.getAttrInt().compareTo(y.getAttrInt()) == 0
            && x.getAttrString().equals(y.getAttrString());
    }

    @Test
    public void testComplexTypeWithAttributeGroup() throws Exception {
        if (!shouldRunTest("ComplexTypeWithAttributeGroup")) {
            return;
        }
        ComplexTypeWithAttributeGroup x = new ComplexTypeWithAttributeGroup();
        x.setAttrInt(new BigInteger("123"));
        x.setAttrString("x123");
        ComplexTypeWithAttributeGroup yOrig = new ComplexTypeWithAttributeGroup();
        yOrig.setAttrInt(new BigInteger("456"));
        yOrig.setAttrString("x456");

        Holder<ComplexTypeWithAttributeGroup> y = new Holder<ComplexTypeWithAttributeGroup>(yOrig);
        Holder<ComplexTypeWithAttributeGroup> z = new Holder<ComplexTypeWithAttributeGroup>();
        ComplexTypeWithAttributeGroup ret;
        if (testDocLiteral) {
            ret = docClient.testComplexTypeWithAttributeGroup(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexTypeWithAttributeGroup(x, y, z);
        } else {
            ret = rpcClient.testComplexTypeWithAttributeGroup(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testComplexTypeWithAttributeGroup(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testComplexTypeWithAttributeGroup(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testComplexTypeWithAttributeGroup(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.ComplexTypeWithAttributeGroup1

    protected boolean equals(ComplexTypeWithAttributeGroup1 x,
                             ComplexTypeWithAttributeGroup1 y) {
        return x.getAttrInt().compareTo(y.getAttrInt()) == 0
            && x.getAttrFloat().compareTo(y.getAttrFloat()) == 0
            && x.getAttrString().equals(y.getAttrString());
    }

    @Test
    public void testComplexTypeWithAttributeGroup1() throws Exception {
        if (!shouldRunTest("ComplexTypeWithAttributeGroup1")) {
            return;
        }
        ComplexTypeWithAttributeGroup1 x = new ComplexTypeWithAttributeGroup1();
        x.setAttrInt(new BigInteger("123"));
        x.setAttrString("x123");
        x.setAttrFloat(new Float(3.14f));
        ComplexTypeWithAttributeGroup1 yOrig = new ComplexTypeWithAttributeGroup1();
        yOrig.setAttrInt(new BigInteger("456"));
        yOrig.setAttrString("x456");
        yOrig.setAttrFloat(new Float(6.28f));

        Holder<ComplexTypeWithAttributeGroup1> y = new Holder<ComplexTypeWithAttributeGroup1>(yOrig);
        Holder<ComplexTypeWithAttributeGroup1> z = new Holder<ComplexTypeWithAttributeGroup1>();
        ComplexTypeWithAttributeGroup1 ret;
        if (testDocLiteral) {
            ret = docClient.testComplexTypeWithAttributeGroup1(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexTypeWithAttributeGroup1(x, y, z);
        } else {
            ret = rpcClient.testComplexTypeWithAttributeGroup1(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testComplexTypeWithAttributeGroup1(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testComplexTypeWithAttributeGroup1(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testComplexTypeWithAttributeGroup1(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.SequenceWithGroupSeq
    protected boolean equals(SequenceWithGroupSeq x, SequenceWithGroupSeq y) {
        return x.getVarInt() == y.getVarInt()
            && Float.compare(x.getVarFloat(), y.getVarFloat()) == 0
            && x.getVarString().equals(y.getVarString())
            && x.getVarOtherInt() == y.getVarOtherInt()
            && Float.compare(x.getVarOtherFloat(), y.getVarOtherFloat()) == 0
            && x.getVarOtherString().equals(y.getVarOtherString());
    }

    @Test
    public void testSequenceWithGroupSeq() throws Exception {
        if (!shouldRunTest("SequenceWithGroupSeq")) {
            return;
        }
        SequenceWithGroupSeq x = new SequenceWithGroupSeq();
        x.setVarInt(100);         
        x.setVarString("hello");
        x.setVarFloat(1.1f); 
        x.setVarOtherInt(11);
        x.setVarOtherString("world");
        x.setVarOtherFloat(10.1f);
        SequenceWithGroupSeq yOrig = new SequenceWithGroupSeq();
        yOrig.setVarInt(11);
        yOrig.setVarString("world");
        yOrig.setVarFloat(10.1f);
        yOrig.setVarOtherInt(100);
        yOrig.setVarOtherString("hello");
        yOrig.setVarOtherFloat(1.1f);

        Holder<SequenceWithGroupSeq> y = new Holder<SequenceWithGroupSeq>(yOrig);
        Holder<SequenceWithGroupSeq> z = new Holder<SequenceWithGroupSeq>();

        SequenceWithGroupSeq ret;
        if (testDocLiteral) {
            ret = docClient.testSequenceWithGroupSeq(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSequenceWithGroupSeq(x, y, z);
        } else {
            ret = rpcClient.testSequenceWithGroupSeq(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testSequenceWithGroupSeq(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testSequenceWithGroupSeq(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testSequenceWithGroupSeq(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.SequenceWithGroupChoice

    protected boolean equals(SequenceWithGroupChoice x, SequenceWithGroupChoice y) {
        if (x.getVarInt() != null && y.getVarInt() != null) {
            if (x.getVarInt().compareTo(y.getVarInt()) != 0) {
                return false;
            }
        } else if (x.getVarFloat() != null && y.getVarFloat() != null) {
            if (x.getVarFloat().compareTo(y.getVarFloat()) != 0) {
                return false;
            }
        } else if (x.getVarString() != null && y.getVarString() != null) {
            if (!x.getVarString().equals(y.getVarString())) {
                return false;
            }
        } else {
            return false;
        }
        if (x.getVarOtherInt() != null && y.getVarOtherInt() != null) {
            if (x.getVarOtherInt().compareTo(y.getVarOtherInt()) != 0) {
                return false;
            }
        } else if (x.getVarOtherFloat() != null && y.getVarOtherFloat() != null) {
            if (x.getVarOtherFloat().compareTo(y.getVarOtherFloat()) != 0) {
                return false;
            }
        } else if (x.getVarOtherString() != null && y.getVarOtherString() != null) {
            return x.getVarOtherString().equals(y.getVarOtherString());
        } else {
            return false;
        }
        return true;
    }
    @Test
    public void testSequenceWithGroupChoice() throws Exception {
        if (!shouldRunTest("SequenceWithGroupChoice")) {
            return;
        }
        SequenceWithGroupChoice x = new SequenceWithGroupChoice();
        x.setVarFloat(1.1f);
        x.setVarOtherString("world");
        SequenceWithGroupChoice yOrig = new SequenceWithGroupChoice();
        yOrig.setVarOtherFloat(2.2f);
        yOrig.setVarString("world");

        Holder<SequenceWithGroupChoice> y = new Holder<SequenceWithGroupChoice>(yOrig);
        Holder<SequenceWithGroupChoice> z = new Holder<SequenceWithGroupChoice>();

        SequenceWithGroupChoice ret;
        if (testDocLiteral) {
            ret = docClient.testSequenceWithGroupChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSequenceWithGroupChoice(x, y, z);
        } else {
            ret = rpcClient.testSequenceWithGroupChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testSequenceWithGroupChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testSequenceWithGroupChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testSequenceWithGroupChoice(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.SequenceWithGroups

    protected boolean equals(SequenceWithGroups x, SequenceWithGroups y) {
        if (x.getVarOtherInt() != null && y.getVarOtherInt() != null) {
            if (x.getVarOtherInt().compareTo(y.getVarOtherInt()) != 0) {
                return false;
            }
        } else if (x.getVarOtherFloat() != null && y.getVarOtherFloat() != null) {
            if (x.getVarOtherFloat().compareTo(y.getVarOtherFloat()) != 0) {
                return false;
            }
        } else if (x.getVarOtherString() != null && y.getVarOtherString() != null) {
            if (!x.getVarOtherString().equals(y.getVarOtherString())) {
                return false;
            }
        } else {
            return false;
        }
        return x.getVarInt() == y.getVarInt()
            && Float.compare(x.getVarFloat(), y.getVarFloat()) == 0
            && x.getVarString().equals(y.getVarString());
    }
    @Test
    public void testSequenceWithGroups() throws Exception {
        if (!shouldRunTest("SequenceWithGroups")) {
            return;
        }
        SequenceWithGroups x = new SequenceWithGroups();
        x.setVarInt(100);
        x.setVarString("hello");
        x.setVarFloat(1.1f);
        x.setVarOtherFloat(1.1f);

        SequenceWithGroups yOrig = new SequenceWithGroups();
        yOrig.setVarInt(11);
        yOrig.setVarString("world");
        yOrig.setVarFloat(10.1f);
        yOrig.setVarOtherString("world");

        Holder<SequenceWithGroups> y = new Holder<SequenceWithGroups>(yOrig);
        Holder<SequenceWithGroups> z = new Holder<SequenceWithGroups>();

        SequenceWithGroups ret;
        if (testDocLiteral) {
            ret = docClient.testSequenceWithGroups(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSequenceWithGroups(x, y, z);
        } else {
            ret = rpcClient.testSequenceWithGroups(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testSequenceWithGroups(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testSequenceWithGroups(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testSequenceWithGroups(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.SequenceWithOccuringGroup

    protected boolean equals(SequenceWithOccuringGroup x, SequenceWithOccuringGroup y) {
        return equalsFloatIntStringList(x.getBatchElementsSeq(), y.getBatchElementsSeq());
    }
    @Test
    public void testSequenceWithOccuringGroup() throws Exception {
        if (!shouldRunTest("SequenceWithOccuringGroup")) {
            return;
        }
        SequenceWithOccuringGroup x = new SequenceWithOccuringGroup();
        x.getBatchElementsSeq().add(1.1f);
        x.getBatchElementsSeq().add(100);
        x.getBatchElementsSeq().add("hello");

        SequenceWithOccuringGroup yOrig = new SequenceWithOccuringGroup();
        yOrig.getBatchElementsSeq().add(2.2f);
        yOrig.getBatchElementsSeq().add(200);
        yOrig.getBatchElementsSeq().add("world");

        Holder<SequenceWithOccuringGroup> y = new Holder<SequenceWithOccuringGroup>(yOrig);
        Holder<SequenceWithOccuringGroup> z = new Holder<SequenceWithOccuringGroup>();

        SequenceWithOccuringGroup ret;
        if (testDocLiteral) {
            ret = docClient.testSequenceWithOccuringGroup(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testSequenceWithOccuringGroup(x, y, z);
        } else {
            ret = rpcClient.testSequenceWithOccuringGroup(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testGroupDirectlyInComplexType(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testGroupDirectlyInComplexType(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testGroupDirectlyInComplexType(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.GroupDirectlyInComplexType

    protected boolean equals(GroupDirectlyInComplexType x, GroupDirectlyInComplexType y) {
        return x.getVarInt() == y.getVarInt() 
            && x.getVarString().equals(y.getVarString())
            && Float.compare(x.getVarFloat(), y.getVarFloat()) == 0
            && x.getAttr1().equals(y.getAttr1());
    }
    @Test
    public void testGroupDirectlyInComplexType() throws Exception {
        if (!shouldRunTest("GroupDirectlyInComplexType")) {
            return;
        }
        GroupDirectlyInComplexType x = new GroupDirectlyInComplexType();
        x.setVarInt(100);
        x.setVarString("hello");
        x.setVarFloat(1.1f);
        x.setAttr1(new Integer(1));
        GroupDirectlyInComplexType yOrig = new GroupDirectlyInComplexType();
        yOrig.setVarInt(11);
        yOrig.setVarString("world");
        yOrig.setVarFloat(10.1f);
        yOrig.setAttr1(new Integer(2)); 

        Holder<GroupDirectlyInComplexType> y = new Holder<GroupDirectlyInComplexType>(yOrig);
        Holder<GroupDirectlyInComplexType> z = new Holder<GroupDirectlyInComplexType>();

        GroupDirectlyInComplexType ret;
        if (testDocLiteral) {
            ret = docClient.testGroupDirectlyInComplexType(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testGroupDirectlyInComplexType(x, y, z);
        } else {
            ret = rpcClient.testGroupDirectlyInComplexType(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testGroupDirectlyInComplexType(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testGroupDirectlyInComplexType(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testGroupDirectlyInComplexType(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.ComplexTypeWithAttributes

    protected boolean equals(ComplexTypeWithAttributes x, ComplexTypeWithAttributes y) {
        return x.getAttrInt().equals(y.getAttrInt())
            && x.getAttrString().equals(y.getAttrString());
    }

    @Test
    public void testComplexTypeWithAttributes() throws Exception {
        if (!shouldRunTest("ComplexTypeWithAttributes")) {
            return;
        }
        ComplexTypeWithAttributes x = new ComplexTypeWithAttributes();
        x.setAttrInt(new BigInteger("123"));
        x.setAttrString("x123");
        ComplexTypeWithAttributes yOrig = new ComplexTypeWithAttributes();
        yOrig.setAttrInt(new BigInteger("456"));
        yOrig.setAttrString("x456");

        Holder<ComplexTypeWithAttributes> y = new Holder<ComplexTypeWithAttributes>(yOrig);
        Holder<ComplexTypeWithAttributes> z = new Holder<ComplexTypeWithAttributes>();
        ComplexTypeWithAttributes ret;
        if (testDocLiteral) {
            ret = docClient.testComplexTypeWithAttributes(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexTypeWithAttributes(x, y, z);
        } else {
            ret = rpcClient.testComplexTypeWithAttributes(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testComplexTypeWithAttributes(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testComplexTypeWithAttributes(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testComplexTypeWithAttributes(): Incorrect return value", equals(x, ret));
        }
    }

    //org.apache.type_test.types2.StructWithAny

    public void assertEqualsStructWithAny(StructWithAny a, StructWithAny b) throws Exception {
        assertEquals("StructWithAny names don't match", a.getName(), b.getName());
        assertEquals("StructWithAny addresses don't match", a.getAddress(), b.getAddress());
        assertEquals(a.getAny(), b.getAny());
    }
    
    public void assertEquals(Element elA, Element elB) throws Exception {
        if (elA instanceof SOAPElement && elB instanceof SOAPElement) {
            SOAPElement soapA = (SOAPElement)elA;
            SOAPElement soapB = (SOAPElement)elB;
            assertEquals("StructWithAny soap element names don't match",
                soapA.getElementName(), soapB.getElementName());
            assertEquals("StructWithAny soap element text nodes don't match",
                soapA.getValue(), soapB.getValue());
            
            Iterator itExp = soapA.getChildElements();
            Iterator itGen = soapB.getChildElements();
            while (itExp.hasNext()) {
                if (!itGen.hasNext()) {
                    fail("Incorrect number of child elements inside any");
                }
                Object objA = itExp.next();         
                Object objB = itGen.next();
                if (objA instanceof SOAPElement) {
                    if (objB instanceof SOAPElement) {
                        assertEquals((SOAPElement)objA, (SOAPElement)objB);
                    } else {
                        fail("No matching soap element.");
                    }
                }
            }
        }
    }
    @Test
    public void testStructWithAny() throws Exception {
        if (!shouldRunTest("StructWithAny")) {
            return;
        }
        StructWithAny swa = new StructWithAny();
        swa.setName("Name");
        swa.setAddress("Some Address");

        StructWithAny yOrig = new StructWithAny();
        yOrig.setName("Name2");
        yOrig.setAddress("Some Other Address");

        SOAPFactory factory = SOAPFactory.newInstance();
        SOAPElement x = factory.createElement("hello", "foo", "http://some.url.com");
        x.addNamespaceDeclaration("foo", "http://some.url.com");
        x.addTextNode("This is the text of the node");

        SOAPElement x2 = factory.createElement("hello2", "foo", "http://some.url.com");
        x2.addNamespaceDeclaration("foo", "http://some.url.com");
        x2.addTextNode("This is the text of the node for the second struct");

        swa.setAny(x);
        yOrig.setAny(x2);

        Holder<StructWithAny> y = new Holder<StructWithAny>(yOrig);
        Holder<StructWithAny> z = new Holder<StructWithAny>();

        StructWithAny ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithAny(swa, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithAny(swa, y, z);
        } else {
            ret = rpcClient.testStructWithAny(swa, y, z);
        }
        if (!perfTestOnly) {
            assertEqualsStructWithAny(swa, y.value);
            assertEqualsStructWithAny(yOrig, z.value);
            assertEqualsStructWithAny(swa, ret);
        }
    }

    @Test
    public void testStructWithAnyXsi() throws Exception {
        if (!shouldRunTest("StructWithAnyXsi")) {
            return;
        }
        StructWithAny swa = new StructWithAny();
        swa.setName("Name");
        swa.setAddress("Some Address");
        StructWithAny yOrig = new StructWithAny();
        yOrig.setName("Name2");
        yOrig.setAddress("Some Other Address");

        SOAPFactory sf = SOAPFactory.newInstance();
        Name elementName = sf.createName("UKAddress", "", "http://apache.org/type_test");
        Name xsiAttrName = sf.createName("type", "xsi", "http://www.w3.org/2001/XMLSchema-instance");
        SOAPElement x = sf.createElement(elementName);
        x.addNamespaceDeclaration("tns", "http://apache.org/type_test");
        x.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        x.addAttribute(xsiAttrName, "tns:UKAddressType11");
        x.addTextNode("This is the text of the node for the first struct");

        Name elementName2 = sf.createName("UKAddress", "", "http://apache.org/type_test");
        Name xsiAttrName2 = sf.createName("type", "xsi", "http://www.w3.org/2001/XMLSchema-instance");
        SOAPElement x2 = sf.createElement(elementName2);
        x2.addNamespaceDeclaration("tns", "http://apache.org/type_test");
        x2.addNamespaceDeclaration("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        x2.addAttribute(xsiAttrName2, "tns:UKAddressType22");
        x2.addTextNode("This is the text of the node for the second struct");

        swa.setAny(x);
        yOrig.setAny(x2);

        Holder<StructWithAny> y = new Holder<StructWithAny>(yOrig);
        Holder<StructWithAny> z = new Holder<StructWithAny>();
        StructWithAny ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithAny(swa, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithAny(swa, y, z);
        } else {
            ret = rpcClient.testStructWithAny(swa, y, z);
        }
        if (!perfTestOnly) {
            assertEqualsStructWithAny(swa, y.value);
            assertEqualsStructWithAny(yOrig, z.value);
            assertEqualsStructWithAny(swa, ret);
        }
    }

    // StructWithInvalidAny
    // XXX - no exception thrown
    @Test
    public void testStructWithInvalidAny() throws Exception {
        if (!shouldRunTest("StructWithInvalidAny")) {
            return;
        }
        StructWithAny swa = new StructWithAny();
        swa.setName("Name");
        swa.setAddress("Some Address");

        StructWithAny yOrig = new StructWithAny();
        yOrig.setName("Name2");
        yOrig.setAddress("Some Other Address");

        SOAPFactory factory = SOAPFactory.newInstance();
        SOAPElement x = factory.createElement("hello", "foo", "http://some.url.com");
        x.addTextNode("This is the text of the node");

        SOAPElement x2 = factory.createElement("hello2", "foo", "http://some.url.com");
        x2.addTextNode("This is the text of the node for the second struct");

        swa.setAny(x);
        yOrig.setAny(x2);

        Holder<StructWithAny> y = new Holder<StructWithAny>(yOrig);
        Holder<StructWithAny> z = new Holder<StructWithAny>();

        try {
            if (testDocLiteral) {
                docClient.testStructWithAny(swa, y, z);
            } else if (testXMLBinding) {
                xmlClient.testStructWithAny(swa, y, z);
            } else {
                rpcClient.testStructWithAny(swa, y, z);
            }
            //fail("testStructWithInvalidAny(): Did not catch expected exception.");
        } catch (Exception ex) {
            fail("testStructWithInvalidAny(): caught expected exception - woot.");
        }
    }

    //org.apache.type_test.types2.StructWithAnyArray

    public void assertEqualsStructWithAnyArray(StructWithAnyArray a, StructWithAnyArray b) throws Exception {
        assertEquals("StructWithAny names don't match", a.getName(), b.getName());
        assertEquals("StructWithAny addresses don't match", a.getAddress(), b.getAddress());

        List<Element> ae = a.getAny();
        List<Element> be = b.getAny();
        
        assertEquals("StructWithAny soap element lengths don't match", ae.size(), be.size());
        for (int i = 0; i < ae.size(); i++) {
            assertEquals(ae.get(i), be.get(i));
        }
    }

    @Test
    public void testStructWithAnyArray() throws Exception {
        if (!shouldRunTest("StructWithAnyArray")) {
            return;
        }
        StructWithAnyArray swa = new StructWithAnyArray();
        swa.setName("Name");
        swa.setAddress("Some Address");

        StructWithAnyArray yOrig = new StructWithAnyArray();
        yOrig.setName("Name2");
        yOrig.setAddress("Some Other Address");

        SOAPFactory factory = SOAPFactory.newInstance();
        SOAPElement x = factory.createElement("hello", "foo", "http://some.url.com");
        x.addNamespaceDeclaration("foo", "http://some.url.com");
        x.addTextNode("This is the text of the node");

        SOAPElement x2 = factory.createElement("hello2", "foo", "http://some.url.com");
        x2.addNamespaceDeclaration("foo", "http://some.url.com");
        x2.addTextNode("This is the text of the node for the second struct");

        swa.getAny().add(x);
        yOrig.getAny().add(x2);

        Holder<StructWithAnyArray> y = new Holder<StructWithAnyArray>(yOrig);
        Holder<StructWithAnyArray> z = new Holder<StructWithAnyArray>();

        StructWithAnyArray ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithAnyArray(swa, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithAnyArray(swa, y, z);
        } else {
            ret = rpcClient.testStructWithAnyArray(swa, y, z);
        }
        if (!perfTestOnly) {
            assertEqualsStructWithAnyArray(swa, y.value);
            assertEqualsStructWithAnyArray(yOrig, z.value);
            assertEqualsStructWithAnyArray(swa, ret);
        }
    }

    // StructWithInvalidAnyArray
    // XXX - no exception thrown
    @Test
    public void testStructWithInvalidAnyArray() throws Exception {
        if (!shouldRunTest("StructWithInvalidAnyArray")) {
            return;
        }
        StructWithAnyArray swa = new StructWithAnyArray();
        swa.setName("Name");
        swa.setAddress("Some Address");

        StructWithAnyArray yOrig = new StructWithAnyArray();
        yOrig.setName("Name2");
        yOrig.setAddress("Some Other Address");

        SOAPFactory factory = SOAPFactory.newInstance();
        SOAPElement x = factory.createElement("hello", "foo", "http://some.url.com");
        x.addTextNode("This is the text of the node");

        SOAPElement x2 = factory.createElement("hello2", "foo", "http://some.url.com");
        x2.addTextNode("This is the text of the node for the second struct");

        swa.getAny().add(x);
        yOrig.getAny().add(x2);

        Holder<StructWithAnyArray> y = new Holder<StructWithAnyArray>(yOrig);
        Holder<StructWithAnyArray> z = new Holder<StructWithAnyArray>();

        try {
            if (testDocLiteral) {
                docClient.testStructWithAnyArray(swa, y, z);
            } else if (testXMLBinding) {
                xmlClient.testStructWithAnyArray(swa, y, z);
            } else {
                rpcClient.testStructWithAnyArray(swa, y, z);
            }
            //fail("testStructWithInvalidAnyArray(): Did not catch expected exception.");
        } catch (Exception ex) {
            // Expected
            fail("testStructWithInvalidAnyArray(): caught expected exception - woot.");
        }
    }

    @Test
    @Ignore
    public void testStructWithAnyStrict() throws Exception {
        if (!shouldRunTest("StructWithAnyStrict")) {
            return;
        }
        // XXX - only added to the soap typetest
    }

    @Test
    @Ignore
    public void testStructWithAnyArrayLax() throws Exception {
        if (!shouldRunTest("StructWithAnyArrayLax")) {
            return;
        }
        // XXX - only added to the soap typetest
    }

    //org.apache.type_test.types2.IDTypeAttribute

    protected boolean equalsIDTypeAttribute(IDTypeAttribute x, IDTypeAttribute y) {
        return equalsNilable(x.getId(), y.getId());
    }

    @Test
    public void testIDTypeAttribute() throws Exception {
        if (!shouldRunTest("IDTypeAttribute")) {
            return;
        }
        // n.b. to be valid elements with an ID in the response message
        // must have a unique ID, so this test does not return x as the
        // return value (like the other tests).
        IDTypeAttribute x = new IDTypeAttribute();
        x.setId("x123");
        IDTypeAttribute yOrig = new IDTypeAttribute();
        yOrig.setId("x456");

        Holder<IDTypeAttribute> y = new Holder<IDTypeAttribute>(yOrig);
        Holder<IDTypeAttribute> z = new Holder<IDTypeAttribute>();
        //IDTypeAttribute ret;
        if (testDocLiteral) {
            /*ret =*/ docClient.testIDTypeAttribute(x, y, z);
        } else if (testXMLBinding) {
            /*ret =*/ xmlClient.testIDTypeAttribute(x, y, z);
        } else {
            /*ret =*/ rpcClient.testIDTypeAttribute(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testIDTypeAttribute(): Incorrect value for inout param",
                       equalsIDTypeAttribute(x, y.value));
            assertTrue("testIDTypeAttribute(): Incorrect value for out param",
                       equalsIDTypeAttribute(yOrig, z.value));
        }
    }

    //org.apache.type_test.types2.MultipleOccursSequenceInSequence
    protected boolean equals(MultipleOccursSequenceInSequence x, MultipleOccursSequenceInSequence y) {
        int size = x.getValue().size();
        if (size != y.getValue().size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (x.getValue().get(i).compareTo(y.getValue().get(i)) != 0) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testMultipleOccursSequenceInSequence() throws Exception {
        if (!shouldRunTest("MultipleOccursSequenceInSequence")) {
            return;
        }
        MultipleOccursSequenceInSequence x = new MultipleOccursSequenceInSequence();
        x.getValue().add(new BigInteger("32"));
        MultipleOccursSequenceInSequence yOriginal = new MultipleOccursSequenceInSequence();
        yOriginal.getValue().add(new BigInteger("3200"));

        Holder<MultipleOccursSequenceInSequence> y =
            new Holder<MultipleOccursSequenceInSequence>(yOriginal);
        Holder<MultipleOccursSequenceInSequence> z =
            new Holder<MultipleOccursSequenceInSequence>();

        MultipleOccursSequenceInSequence ret;
        if (testDocLiteral) {
            ret = docClient.testMultipleOccursSequenceInSequence(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testMultipleOccursSequenceInSequence(x, y, z);
        } else {
            ret = rpcClient.testMultipleOccursSequenceInSequence(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testMultipleOccursSequenceInSequence(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testMultipleOccursSequenceInSequence(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testMultipleOccursSequenceInSequence(): Incorrect return value",
                       equals(x, ret));
        }
    }
 
    //org.apache.type_test.types2.StructWithBinary;
    
    protected boolean equals(StructWithBinary x, StructWithBinary y) {
        return Arrays.equals(x.getBase64(), y.getBase64())
            && Arrays.equals(x.getHex(), y.getHex());
    }
    @Test
    public void testStructWithBinary() throws Exception {
        if (!shouldRunTest("StructWithBinary")) {
            return;
        }
        StructWithBinary x = new StructWithBinary();
        x.setBase64("base64Binary_x".getBytes());
        x.setHex("hexBinary_x".getBytes());

        StructWithBinary yOriginal = new StructWithBinary();
        yOriginal.setBase64("base64Binary_y".getBytes());
        yOriginal.setHex("hexBinary_y".getBytes());

        Holder<StructWithBinary> y = new Holder<StructWithBinary>(yOriginal);
        Holder<StructWithBinary> z = new Holder<StructWithBinary>();

        StructWithBinary ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithBinary(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithBinary(x, y, z);
        } else {
            ret = rpcClient.testStructWithBinary(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testStructWithBinary(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testStructWithBinary(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testStructWithBinary(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types2.ChoiceWithBinary;

    protected boolean equals(ChoiceWithBinary x, ChoiceWithBinary y) {
        if (x.getBase64() != null && y.getBase64() != null) {
            return Arrays.equals(x.getBase64(), y.getBase64());
        } else {
            return x.getHex() != null && y.getHex() != null
                && Arrays.equals(x.getHex(), y.getHex());
        }
    }

    @Test
    public void testChoiceWithBinary() throws Exception {
        if (!shouldRunTest("ChoiceWithBinary")) {
            return;
        }
        ChoiceWithBinary x = new ChoiceWithBinary();
        x.setBase64("base64Binary_x".getBytes());

        ChoiceWithBinary yOriginal = new ChoiceWithBinary();
        yOriginal.setHex("hexBinary_y".getBytes());

        Holder<ChoiceWithBinary> y = new Holder<ChoiceWithBinary>(yOriginal);
        Holder<ChoiceWithBinary> z = new Holder<ChoiceWithBinary>();

        ChoiceWithBinary ret;
        if (testDocLiteral) {
            ret = docClient.testChoiceWithBinary(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testChoiceWithBinary(x, y, z);
        } else {
            ret = rpcClient.testChoiceWithBinary(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testChoiceWithBinary(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testChoiceWithBinary(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testChoiceWithBinary(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types2.ChoiceWithGroupChoice;

    protected boolean equals(ChoiceWithGroupChoice x, ChoiceWithGroupChoice y) {
        if (x.getVarFloat() != null  && y.getVarFloat() != null) {
            return x.getVarFloat().compareTo(y.getVarFloat()) == 0;
        }
        if (x.getVarInt() != null  && y.getVarInt() != null) {
            return x.getVarInt().compareTo(y.getVarInt()) == 0;
        }
        if (x.getVarString() != null  && y.getVarString() != null) {
            return x.getVarString().equals(y.getVarString());
        }
        if (x.getVarOtherFloat() != null  && y.getVarOtherFloat() != null) {
            return x.getVarOtherFloat().compareTo(y.getVarOtherFloat()) == 0;
        }
        if (x.getVarOtherInt() != null  && y.getVarOtherInt() != null) {
            return x.getVarOtherInt().compareTo(y.getVarOtherInt()) == 0;
        }
        if (x.getVarOtherString() != null  && y.getVarOtherString() != null) {
            return x.getVarOtherString().equals(y.getVarOtherString());
        }
        return false;
    }

    // XXX - Generated code flattens nested choice
    @Test
    public void testChoiceWithGroupChoice() throws Exception {
        if (!shouldRunTest("ChoiceWithGroupChoice")) {
            return;
        }
        ChoiceWithGroupChoice x = new ChoiceWithGroupChoice();
        x.setVarFloat(1.1f);
        ChoiceWithGroupChoice yOrig = new ChoiceWithGroupChoice();
        yOrig.setVarOtherString("world");

        Holder<ChoiceWithGroupChoice> y = new Holder<ChoiceWithGroupChoice>(yOrig);
        Holder<ChoiceWithGroupChoice> z = new Holder<ChoiceWithGroupChoice>();

        ChoiceWithGroupChoice ret;
        if (testDocLiteral) {
            ret = docClient.testChoiceWithGroupChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testChoiceWithGroupChoice(x, y, z);
        } else {
            ret = rpcClient.testChoiceWithGroupChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testChoiceWithGroupChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testChoiceWithGroupChoice(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testChoiceWithGroupChoice(): Incorrect return value",
                       equals(x, ret));
        }
    }
    
    //org.apache.type_test.types2.ChoiceWithGroupSeq;

    protected boolean equals(ChoiceWithGroupSeq x, ChoiceWithGroupSeq y) {
        if (x.getVarInt() != null && x.getVarFloat() != null
            && x.getVarString() != null) {
            if (x.getVarInt().compareTo(y.getVarInt()) != 0) {
                return false;
            }
            if (x.getVarFloat().compareTo(y.getVarFloat()) != 0) {
                return false;
            }
            return x.getVarString().equals(y.getVarString());
        }
        if (x.getVarOtherInt() != null && x.getVarOtherFloat() != null
            && x.getVarOtherString() != null) {
            if (x.getVarOtherInt().compareTo(y.getVarOtherInt()) != 0) {
                return false;
            }
            if (x.getVarOtherFloat().compareTo(y.getVarOtherFloat()) != 0) {
                return false;
            }
            return x.getVarOtherString().equals(y.getVarOtherString());
        }
        return false;
    }
    
    // XXX - Generated code flattens nested structs
    @Test
    public void testChoiceWithGroupSeq() throws Exception {
        if (!shouldRunTest("ChoiceWithGroupSeq")) {
            return;
        }
        ChoiceWithGroupSeq x = new ChoiceWithGroupSeq();
        x.setVarInt(100);
        x.setVarString("hello");
        x.setVarFloat(1.1f);
        ChoiceWithGroupSeq yOrig = new ChoiceWithGroupSeq();
        yOrig.setVarOtherInt(11);
        yOrig.setVarOtherString("world");
        yOrig.setVarOtherFloat(10.1f);

        Holder<ChoiceWithGroupSeq> y = new Holder<ChoiceWithGroupSeq>(yOrig);
        Holder<ChoiceWithGroupSeq> z = new Holder<ChoiceWithGroupSeq>();

        ChoiceWithGroupSeq ret;
        if (testDocLiteral) {
            ret = docClient.testChoiceWithGroupSeq(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testChoiceWithGroupSeq(x, y, z);
        } else {
            ret = rpcClient.testChoiceWithGroupSeq(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testChoiceWithGroupSeq(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testChoiceWithGroupSeq(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testChoiceWithGroupSeq(): Incorrect return value",
                       equals(x, ret));
        }
    }
    
    //org.apache.type_test.types2.ChoiceWithGroups;

    protected boolean equals(ChoiceWithGroups x, ChoiceWithGroups y) {
        if (x.getVarInt() != null && x.getVarString() != null
            && x.getVarFloat() != null) {
            if (x.getVarInt().compareTo(y.getVarInt()) == 0 
                && x.getVarString().equals(y.getVarString())
                && x.getVarFloat().compareTo(y.getVarFloat()) == 0) {
                return true;
            }
            return false;
        } else {
            if (x.getVarOtherFloat() != null && y.getVarOtherFloat() != null) {
                return x.getVarOtherFloat().compareTo(y.getVarOtherFloat()) == 0;
            }
            if (x.getVarOtherInt() != null && y.getVarOtherInt() != null) {
                return x.getVarOtherInt().compareTo(y.getVarOtherInt()) == 0;
            }
            if (x.getVarOtherString() != null && y.getVarOtherString() != null) {
                return x.getVarOtherString().equals(y.getVarOtherString());
            }
            return false;
        }
    }
    
    // XXX - Generated code flattens nested structs
    @Test
    public void testChoiceWithGroups() throws Exception {
        if (!shouldRunTest("ChoiceWithGroups")) {
            return;
        }
        ChoiceWithGroups x = new ChoiceWithGroups();
        x.setVarInt(100);
        x.setVarString("hello");
        x.setVarFloat(1.1f);
        ChoiceWithGroups yOrig = new ChoiceWithGroups();
        yOrig.setVarOtherString("world");

        Holder<ChoiceWithGroups> y = new Holder<ChoiceWithGroups>(yOrig);
        Holder<ChoiceWithGroups> z = new Holder<ChoiceWithGroups>();

        ChoiceWithGroups ret;
        if (testDocLiteral) {
            ret = docClient.testChoiceWithGroups(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testChoiceWithGroups(x, y, z);
        } else {
            ret = rpcClient.testChoiceWithGroups(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testChoiceWithGroups(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testChoiceWithGroups(): Incorrect value for out param",
                       equals(yOrig, z.value));
            assertTrue("testChoiceWithGroups(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types2.ExtBase64Binary;

    protected boolean equals(ExtBase64Binary x, ExtBase64Binary y) {
        return x.getId() == y.getId() && Arrays.equals(x.getValue(), y.getValue());
    }

    @Test
    public void testExtBase64Binary() throws Exception {
        if (!shouldRunTest("ExtBase64Binary")) {
            return;
        }
        ExtBase64Binary x1 = new ExtBase64Binary();
        x1.setValue("base64a".getBytes());
        x1.setId(1);

        ExtBase64Binary y1 = new ExtBase64Binary();
        y1.setValue("base64b".getBytes());
        y1.setId(2);

        Holder<ExtBase64Binary> y1Holder = new Holder<ExtBase64Binary>(y1);
        Holder<ExtBase64Binary> z1 = new Holder<ExtBase64Binary>();
        ExtBase64Binary ret;
        if (testDocLiteral) {
            ret = docClient.testExtBase64Binary(x1, y1Holder, z1);
        } else if (testXMLBinding) {
            ret = xmlClient.testExtBase64Binary(x1, y1Holder, z1);
        } else {
            ret = rpcClient.testExtBase64Binary(x1, y1Holder, z1);
        }

        if (!perfTestOnly) {
            assertTrue("testExtBase64Binary(): Incorrect value for inout param",
                       equals(x1, y1Holder.value));
            assertTrue("testExtBase64Binary(): Incorrect value for out param",
                       equals(y1, z1.value));
            assertTrue("testExtBase64Binary(): Incorrect return value", equals(x1, ret));
        }
    }

    //org.apache.type_test.types2.StructWithAnyAttribute;

    protected boolean equals(StructWithAnyAttribute x, StructWithAnyAttribute y) {
        if (!(x.getVarString().equals(y.getVarString()))
            || (x.getVarInt() != y.getVarInt())) {
            return false;
        }
        if (!equalsNilable(x.getAtString(), y.getAtString())
            || !equalsNilable(x.getAtInt(), y.getAtInt())) {
            return false;
        }
        return equalsQNameStringPairs(x.getOtherAttributes(), y.getOtherAttributes());
    }

    protected boolean equalsQNameStringPairs(Map<QName, String> x, Map<QName, String> y) {
        if ((x == null && y != null)
            || (x != null && y == null)) {
            return false;
        }
        if (x.isEmpty() && y.isEmpty()) {
            return true;
        }
        if (x.size() != y.size()) {
            return false;
        }

        Iterator<QName> itx = x.keySet().iterator();
        while (itx.hasNext()) {
            QName attName = itx.next();
            if (attName == null) {
                return false;
            }

            String attValue = y.get(attName);
            if (attValue == null || !attValue.equals(x.get(attName))) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testStructWithAnyAttribute() throws Exception {
        if (!shouldRunTest("StructWithAnyAttribute")) {
            return;
        }
        QName xAt1Name = new QName("http://schemas.iona.com/type_test", "at_one");
        QName xAt2Name = new QName("http://schemas.iona.com/type_test", "at_two");
        QName yAt3Name = new QName("http://apache.org/type_test", "at_thr");
        QName yAt4Name = new QName("http://apache.org/type_test", "at_fou");

        StructWithAnyAttribute x = new StructWithAnyAttribute();
        StructWithAnyAttribute y = new StructWithAnyAttribute();

        x.setVarString("hello");
        x.setVarInt(1000);
        x.setAtString("hello attribute");
        x.setAtInt(new Integer(2000));

        y.setVarString("there");
        y.setVarInt(1001);
        y.setAtString("there attribute");
        y.setAtInt(new Integer(2002));

        Map<QName, String> xAttrMap = x.getOtherAttributes();
        xAttrMap.put(xAt1Name, "one");
        xAttrMap.put(xAt2Name, "two");

        Map<QName, String> yAttrMap = y.getOtherAttributes();
        yAttrMap.put(yAt3Name, "three");
        yAttrMap.put(yAt4Name, "four");

        Holder<StructWithAnyAttribute> yh = new Holder<StructWithAnyAttribute>(y);
        Holder<StructWithAnyAttribute> zh = new Holder<StructWithAnyAttribute>();
        StructWithAnyAttribute ret;
        if (testDocLiteral) {
            ret = docClient.testStructWithAnyAttribute(x, yh, zh);
        } else if (testXMLBinding) {
            ret = xmlClient.testStructWithAnyAttribute(x, yh, zh);
        } else {
            ret = rpcClient.testStructWithAnyAttribute(x, yh, zh);
        }

        if (!perfTestOnly) {
            assertTrue("testStructWithAnyAttribute(): Incorrect value for inout param",
                equals(x, yh.value));
            assertTrue("testStructWithAnyAttribute(): Incorrect value for out param",
                equals(y, zh.value));
            assertTrue("testStructWithAnyAttribute(): Incorrect return value",
                equals(ret, x));
        }
    }

    //org.apache.type_test.types2.ChoiceWithAnyAttribute;

    protected boolean equals(ChoiceWithAnyAttribute x, ChoiceWithAnyAttribute y) {
        String xString = x.getVarString();
        String yString = y.getVarString();
        Integer xInt = x.getVarInt();
        Integer yInt = y.getVarInt();
        if (xString != null) {
            if (yString == null || !xString.equals(yString)) {
                fail(xString + " != " + yString);
                return false;
            }
        } else if (xInt != null) {
            if (yInt == null || !(xInt.equals(yInt))) {
                fail(xInt + " != " + yInt);
                return false;
            }
        } else {
            fail("null choice");
            return false;
        }

        if (!equalsNilable(x.getAtString(), y.getAtString())
            || !equalsNilable(x.getAtInt(), y.getAtInt())) {
            fail("grrr");
            return false;
        } 
        return equalsQNameStringPairs(x.getOtherAttributes(), y.getOtherAttributes());
    }

    @Test
    public void testChoiceWithAnyAttribute() throws Exception {
        if (!shouldRunTest("ChoiceWithAnyAttribute")) {
            return;
        }
        QName xAt1Name = new QName("http://schemas.iona.com/type_test", "at_one");
        QName xAt2Name = new QName("http://schemas.iona.com/type_test", "at_two");
        QName yAt3Name = new QName("http://apache.org/type_test", "at_thr");
        QName yAt4Name = new QName("http://apache.org/type_test", "at_fou");

        ChoiceWithAnyAttribute x = new ChoiceWithAnyAttribute();
        ChoiceWithAnyAttribute y = new ChoiceWithAnyAttribute();

        x.setVarString("hello");
        x.setAtString("hello attribute");
        x.setAtInt(new Integer(2000));

        y.setVarInt(1001);
        y.setAtString("there attribute");
        y.setAtInt(new Integer(2002));

        Map<QName, String> xAttrMap = x.getOtherAttributes();
        xAttrMap.put(xAt1Name, "one");
        xAttrMap.put(xAt2Name, "two");

        Map<QName, String> yAttrMap = y.getOtherAttributes();
        yAttrMap.put(yAt3Name, "three");
        yAttrMap.put(yAt4Name, "four");

        Holder<ChoiceWithAnyAttribute> yh = new Holder<ChoiceWithAnyAttribute>(y);
        Holder<ChoiceWithAnyAttribute> zh = new Holder<ChoiceWithAnyAttribute>();
        ChoiceWithAnyAttribute ret;
        if (testDocLiteral) {
            ret = docClient.testChoiceWithAnyAttribute(x, yh, zh);
        } else if (testXMLBinding) {
            ret = xmlClient.testChoiceWithAnyAttribute(x, yh, zh);
        } else {
            ret = rpcClient.testChoiceWithAnyAttribute(x, yh, zh);
        }

        if (!perfTestOnly) {
            assertTrue("testChoiceWithAnyAttribute(): Incorrect value for inout param",
                equals(x, yh.value));
            assertTrue("testChoiceWithAnyAttribute(): Incorrect value for out param",
                equals(y, zh.value));
            assertTrue("testChoiceWithAnyAttribute(): Incorrect return value",
                equals(ret, x));
        }
    }

    //org.apache.type_test.types3.OccuringStruct;

    protected boolean equals(OccuringStruct x, OccuringStruct y) {
        if (!equalsNilable(x.getVarAttrib(), y.getVarAttrib())) {
            return false;
        }
        return equalsFloatIntStringList(x.getVarFloatAndVarIntAndVarString(),
                                        y.getVarFloatAndVarIntAndVarString());
    }

    protected boolean equalsFloatIntStringList(List<Serializable> xList,
                                               List<Serializable> yList) {
        int size = xList.size();
        if (size != yList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (xList.get(i) instanceof Float && yList.get(i) instanceof Float) {
                Float fx = (Float)xList.get(i);
                Float fy = (Float)yList.get(i);
                if (fx.compareTo(fy) != 0) {
                    return false;
                }
            } else if (xList.get(i) instanceof Integer && yList.get(i) instanceof Integer) {
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
    @Test
    public void testOccuringStruct() throws Exception {
        if (!shouldRunTest("OccuringStruct")) {
            return;
        }
        OccuringStruct x = new OccuringStruct();
        List<Serializable> theList = x.getVarFloatAndVarIntAndVarString(); 
        theList.add(1.14f);
        theList.add(new Integer(0));
        theList.add("x1");
        theList.add(11.14f);
        theList.add(new Integer(1));
        theList.add("x2");
        x.setVarAttrib("x_attr");

        OccuringStruct yOriginal = new OccuringStruct();
        theList = yOriginal.getVarFloatAndVarIntAndVarString();
        theList.add(3.14f);
        theList.add(new Integer(42));
        theList.add("y");
        yOriginal.setVarAttrib("y_attr");

        Holder<OccuringStruct> y = new Holder<OccuringStruct>(yOriginal);
        Holder<OccuringStruct> z = new Holder<OccuringStruct>();

        OccuringStruct ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringStruct(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringStruct(x, y, z);
        } else {
            ret = rpcClient.testOccuringStruct(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testOccuringStruct(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringStruct(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringStruct(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types3.OccuringStruct1;
    
    protected boolean equals(OccuringStruct1 x, OccuringStruct1 y) {
        return equalsFloatIntStringList(x.getVarFloatAndVarIntAndVarString(),
                                        y.getVarFloatAndVarIntAndVarString());
    }
    @Test
    public void testOccuringStruct1() throws Exception {
        if (!shouldRunTest("OccuringStruct1")) {
            return;
        }
        OccuringStruct1 x = new OccuringStruct1();
        List<Serializable> theList = x.getVarFloatAndVarIntAndVarString(); 
        theList.add(1.1f);
        theList.add(2);
        theList.add("xX");

        OccuringStruct1 yOriginal = new OccuringStruct1();
        theList = yOriginal.getVarFloatAndVarIntAndVarString();
        theList.add(11.11f);
        theList.add(22);
        theList.add("yY");

        Holder<OccuringStruct1> y = new Holder<OccuringStruct1>(yOriginal);
        Holder<OccuringStruct1> z = new Holder<OccuringStruct1>();

        OccuringStruct1 ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringStruct1(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringStruct1(x, y, z);
        } else {
            ret = rpcClient.testOccuringStruct1(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testOccuringStruct1(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringStruct1(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringStruct1(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types3.OccuringStruct2;
    
    protected boolean equals(OccuringStruct2 x, OccuringStruct2 y) {
        if (Float.compare(x.getVarFloat(), y.getVarFloat()) != 0) {
            return false;
        }
        List<Serializable> xList = x.getVarIntAndVarString();
        List<Serializable> yList = y.getVarIntAndVarString();
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

    @Test
    public void testOccuringStruct2() throws Exception {
        if (!shouldRunTest("OccuringStruct2")) {
            return;
        }
        OccuringStruct2 x = new OccuringStruct2();
        x.setVarFloat(1.14f);
        List<Serializable> theList = x.getVarIntAndVarString();
        theList.add(0);
        theList.add("x1");
        theList.add(1);
        theList.add("x2");

        OccuringStruct2 yOriginal = new OccuringStruct2();
        yOriginal.setVarFloat(3.14f);
        theList = yOriginal.getVarIntAndVarString();
        theList.add(42);
        theList.add("the answer");
        theList.add(6);
        theList.add("hammer");
        theList.add(2);
        theList.add("anvil");

        Holder<OccuringStruct2> y = new Holder<OccuringStruct2>(yOriginal);
        Holder<OccuringStruct2> z = new Holder<OccuringStruct2>();

        OccuringStruct2 ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringStruct2(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringStruct2(x, y, z);
        } else {
            ret = rpcClient.testOccuringStruct2(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testOccuringStruct2(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringStruct2(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringStruct2(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types3.OccuringChoice;

    protected boolean equals(OccuringChoice x, OccuringChoice y) {
        if (!equalsNilable(x.getVarAttrib(), y.getVarAttrib())) {
            return false;
        }
        return equalsFloatIntStringList(x.getVarFloatOrVarIntOrVarString(),
                                        y.getVarFloatOrVarIntOrVarString());
    }
    @Test
    public void testOccuringChoice() throws Exception {
        if (!shouldRunTest("OccuringChoice")) {
            return;
        }
        OccuringChoice x = new OccuringChoice();
        List<Serializable> theList = x.getVarFloatOrVarIntOrVarString();
        theList.add(0);
        theList.add(1.14f);
        theList.add("x1");
        theList.add(1);
        theList.add(11.14f);
        x.setVarAttrib("x_attr");

        OccuringChoice yOriginal = new OccuringChoice();
        theList = yOriginal.getVarFloatOrVarIntOrVarString();
        theList.add(3.14f);
        theList.add("y");
        theList.add(42);
        yOriginal.setVarAttrib("y_attr");

        Holder<OccuringChoice> y = new Holder<OccuringChoice>(yOriginal);
        Holder<OccuringChoice> z = new Holder<OccuringChoice>();

        OccuringChoice ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringChoice(x, y, z);
        } else {
            ret = rpcClient.testOccuringChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testOccuringChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringChoice(): Incorrect return value",
                       equals(x, ret));
        }

        theList.add(52);
        theList.add(4.14f);

        y = new Holder<OccuringChoice>(yOriginal);
        z = new Holder<OccuringChoice>();

        if (testDocLiteral) {
            ret = docClient.testOccuringChoice(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringChoice(x, y, z);
        } else {
            ret = rpcClient.testOccuringChoice(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testOccuringChoice(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringChoice(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringChoice(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types3.OccuringChoice1;
    
    protected boolean equals(OccuringChoice1 x, OccuringChoice1 y) {
        List<Comparable> xList = x.getVarFloatOrVarInt();
        List<Comparable> yList = y.getVarFloatOrVarInt();
        int size = xList.size();
        if (size != yList.size()) {
            return false;
        }
        for (int i = 0; i < size; i++) {
            if (xList.get(i) instanceof Integer && yList.get(i) instanceof Integer) {
                Integer xi = (Integer)xList.get(i);
                Integer yi = (Integer)yList.get(i);
                if (xi.compareTo(yi) != 0) {
                    return false;
                }
            }
            if (xList.get(i) instanceof Float && yList.get(i) instanceof Float) {
                Float xf = (Float)xList.get(i);
                Float yf = (Float)yList.get(i);
                if (xf.compareTo(yf) != 0) {
                    return false;
                }
            }
        }
        return true;
    }
    @Test
    public void testOccuringChoice1() throws Exception {
        if (!shouldRunTest("OccuringChoice1")) {
            return;
        }
        OccuringChoice1 x = new OccuringChoice1();
        List<Comparable> theList = x.getVarFloatOrVarInt();
        theList.add(0);
        theList.add(new Float(1.14f));
        theList.add(1);
        theList.add(new Float(11.14f));
        // leave y empty
        OccuringChoice1 yOriginal = new OccuringChoice1();

        Holder<OccuringChoice1> y = new Holder<OccuringChoice1>(yOriginal);
        Holder<OccuringChoice1> z = new Holder<OccuringChoice1>();

        OccuringChoice1 ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringChoice1(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringChoice1(x, y, z);
        } else {
            ret = rpcClient.testOccuringChoice1(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testOccuringChoice1(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringChoice1(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringChoice1(): Incorrect return value",
                       equals(x, ret));
        }
    }

    //org.apache.type_test.types3.OccuringChoice2;

    protected boolean equals(OccuringChoice2 x, OccuringChoice2 y) {
        if (x.getVarString() != null && !x.getVarString().equals(y.getVarString())) {
            return false;
        }
        if (x.getVarInt() != null && x.getVarInt() != y.getVarInt()) {
            return false;
        }
        return true;
    }

    @Test
    public void testOccuringChoice2() throws Exception {
        if (!shouldRunTest("OccuringChoice2")) {
            return;
        }
        OccuringChoice2 x = new OccuringChoice2();
        x.setVarString("x1");
        OccuringChoice2 yOriginal = new OccuringChoice2();
        yOriginal.setVarString("y1");
        Holder<OccuringChoice2> y = new Holder<OccuringChoice2>(yOriginal);
        Holder<OccuringChoice2> z = new Holder<OccuringChoice2>();
        OccuringChoice2 ret;
        if (testDocLiteral) {
            ret = docClient.testOccuringChoice2(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringChoice2(x, y, z);
        } else {
            ret = rpcClient.testOccuringChoice2(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testOccuringChoice2(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringChoice2(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringChoice2(): Incorrect return value",
                       equals(x, ret));
        }

        x = new OccuringChoice2();
        yOriginal = new OccuringChoice2();
        yOriginal.setVarString("y1");
        y = new Holder<OccuringChoice2>(yOriginal);
        z = new Holder<OccuringChoice2>();
        if (testDocLiteral) {
            ret = docClient.testOccuringChoice2(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testOccuringChoice2(x, y, z);
        } else {
            ret = rpcClient.testOccuringChoice2(x, y, z);
        }

        if (!perfTestOnly) {
            assertTrue("testOccuringChoice2(): Incorrect value for inout param",
                       equals(x, y.value));
            assertTrue("testOccuringChoice2(): Incorrect value for out param",
                       equals(yOriginal, z.value));
            assertTrue("testOccuringChoice2(): Incorrect return value",
                       equals(x, ret));
        }
    }
    @Test
    public void testAnonEnumList() throws Exception {
        if (!shouldRunTest("AnonEnumList")) {
            return;
        }
        if (testDocLiteral || testXMLBinding) {
            List<Short> x = Arrays.asList((short)10, (short)100);
            List<Short> yOrig = Arrays.asList((short)1000, (short)10);

            Holder<List<Short>> y = new Holder<List<Short>>(yOrig);
            Holder<List<Short>> z = new Holder<List<Short>>();

            List<Short> ret = testDocLiteral ? docClient.testAnonEnumList(x, y, z) : xmlClient
                .testAnonEnumList(x, y, z);
            if (!perfTestOnly) {
                assertTrue("testAnonEnumList(): Incorrect value for inout param", x.equals(y.value));
                assertTrue("testAnonEnumList(): Incorrect value for out param", yOrig.equals(z.value));
                assertTrue("testAnonEnumList(): Incorrect return value", x.equals(ret));
            }
        } else {
            Short[] x = {(short)10, (short)100};
            Short[] yOrig = {(short)1000, (short)10};

            Holder<Short[]> y = new Holder<Short[]>(yOrig);
            Holder<Short[]> z = new Holder<Short[]>();

            Short[] ret = rpcClient.testAnonEnumList(x, y, z);

            assertTrue(y.value.length == 2);
            assertTrue(z.value.length == 2);
            assertTrue(ret.length == 2);
            if (!perfTestOnly) {
                for (int i = 0; i < 2; i++) {
                    assertEquals("testAnonEnumList(): Incorrect value for inout param", x[i].shortValue(),
                                 y.value[i].shortValue());
                    assertEquals("testAnonEnumList(): Incorrect value for out param", yOrig[i].shortValue(),
                                 z.value[i].shortValue());
                    assertEquals("testAnonEnumList(): Incorrect return value", x[i].shortValue(), ret[i]
                        .shortValue());
                }
            }
        }
    }

    @Test
    public void testUnionWithAnonEnum() throws Exception {
        if (!shouldRunTest("UnionWithAnonEnum")) {
            return;
        }
        String x = "5";
        String yOrig = "n/a";

        Holder<String> y = new Holder<String>(yOrig);
        Holder<String> z = new Holder<String>();
        String ret;
        if (testDocLiteral) {
            ret = docClient.testUnionWithAnonEnum(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testUnionWithAnonEnum(x, y, z);
        } else {
            ret = rpcClient.testUnionWithAnonEnum(x, y, z);
        }
        assertEquals("testUnionWithAnonEnum(): Incorrect value for inout param", x, y.value);
        assertEquals("testUnionWithAnonEnum(): Incorrect value for out param", yOrig, z.value);
        assertEquals("testUnionWithAnonEnum(): Incorrect return value", x, ret);
    }
    
}
