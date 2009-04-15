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
package org.apache.cxf.systest.type_test.corba;


import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.cxf.systest.type_test.AbstractTypeTestClient5;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CORBADocLitClientTypeTest extends AbstractTypeTestClient5 {
    protected static final String WSDL_PATH 
        = "/wsdl_systest/type_test_corba/type_test_corba-corba.wsdl";
    
    
    protected static final QName SERVICE_NAME = new QName("http://apache.org/type_test/doc",
                                                          "TypeTestCORBAService");
    protected static final QName PORT_NAME = new QName("http://apache.org/type_test/doc",
                                                       "TypeTestCORBAPort");

    private static final Set<String> NOT_WORKING_TESTS = new HashSet<String>();
    private static final Set<String> RUN_TESTS = new HashSet<String>();
    static {
        
        String notWorking[] = new String[] {
            "DerivedChoiceBaseChoice",
            "UnionWithAnonEnum",
            "SimpleAll",
            "OccuringChoiceWithAnyAttribute",
            "ChoiceArray",
            "ChoiceOfSeq",
            "DerivedStructBaseStruct",
            "OccuringChoice2",
            "OccuringStructWithAnyAttribute",
            "OccuringStruct",
            "OccuringStruct2",
            "InheritanceUnboundedArrayDerivedChoice",
            "ComplexTypeWithAttributes",
            "OccuringChoice1",
            "StructWithSubstitutionGroup",
            "StructWithNillableStruct",
            "SimpleContent2",
            "OccuringAll",
            "Document",
            "ChoiceWithAnyAttribute",
            "RecursiveUnionData",
            "ExtBase64Binary",
            "RecursiveStructArray",
            "StructWithAnyArray",
            "UnsignedByte",
            "SimpleStruct",
            "RecOuterType",
            "UnionWithStringListRestriction",
            "RecElType",
            "AnonEnumList",
            "DerivedNoContent",
            "StructWithMultipleSubstitutionGroups",
            "StructWithNillableChoice",
            "RestrictedAllBaseAll",
            "NestedStruct",
            "StructWithOccuringStruct",
            "SequenceWithGroups",
            "RecursiveUnion",
            "ExtendsSimpleType",
            "InheritanceEmptyAllDerivedEmpty",
            "HexBinaryRestriction",
            "DerivedStructBaseChoice",
            "UnionSimpleContent",
            "ChoiceWithSubstitutionGroup",
            "UnionWithAnonList",
            "ComplexRestriction",
            "NMTOKENS",
            "SequenceWithGroupSeq",
            "IDTypeAttribute",
            "SimpleContent3",
            "SimpleUnionList",
            "OccuringChoice",
            "SimpleRestriction2",
            "SimpleContent1",
            "StructWithAnyXsi",
            "InheritanceNestedStruct",
            "StructWithInvalidAny",
            "SimpleRestriction6",
            "ComplexRestriction5",
            "ExtendsSimpleContent",
            "DateTime",
            "DerivedChoiceBaseStruct",
            "StringI18N",
            "ChoiceOfChoice",
            "ComplexRestriction3",
            "Base64BinaryRestriction",
            "StructWithList",
            "QNameList",
            "InheritanceSimpleStructDerivedStruct",
            "SimpleRestriction5",
            "ChoiceWithSubstitutionGroupAbstract",
            "ChoiceWithSubstitutionGroupNil",
            "ChoiceWithGroups",
            "SequenceWithGroupChoice",
            "DerivedChoiceBaseArray",
            "UnionWithStringList",
            "SimpleRestriction3",
            "ComplexRestriction2",
            "SimpleListRestriction2",
            "DerivedStructBaseEmpty",
            "RestrictedChoiceBaseChoice",
            "ChoiceWithGroupSeq",
            "EmptyStruct",
            "RestrictedStructBaseStruct",
            "InheritanceSimpleChoiceDerivedStruct",
            "RecursiveStruct",
            "MRecSeqA",
            "StructWithUnion",
            "StructWithOptionals",
            "OccuringStruct1",
            "ExtColourEnum",
            "RecSeqB6918",
            "StructWithSubstitutionGroupNil",
            "SimpleContentExtWithAnyAttribute",
            "StructWithAnyAttribute",
            "StringList",
            "StructWithOccuringChoice",
            "StructWithAny",
            "SequenceWithOccuringGroup",
            "ComplexTypeWithAttributeGroup1",
            "QName",
            "StructWithOccuringStruct2",
            "Occuri ngStruct2",
            "ChoiceWithGroupChoice",
            "StructWithInvalidAnyArray",
            "MRecSeqC",
            "AnonymousType",
            "AnyURIRestriction",
            "NumberList",
            "StructWithSubstitutionGroupAbstract",
            "GroupDirectlyInComplexType",
            "ComplexTypeWithAttributeGroup",
            "ComplexRestriction4",
        };
        NOT_WORKING_TESTS.addAll(Arrays.asList(notWorking));

        String notWorkingIBM[] = new String[] {
            "AnyURIEnum",
            "NMTokenEnum",
            "DecimalEnum",
            "StringEnum",
            "NumberEnum",
            "ColourEnum",
            "Base64Binary",
            "HexBinary",
            "Decimal",
            "UnsignedShort",
            "SimpleChoice",
            "EmptyChoice",
            "NestedArray",
            "CompoundArray",
            "UnboundedArray",
            "BoundedArray",
            "FixedArray",
            "AnonymousStruct",
            "StructWithNillables",
            "ChoiceWithBinary",
            "StructWithBinary",
            "MultipleOccursSequenceInSequence",
            "DerivedEmptyBaseEmptyChoice"
        };
        if (System.getProperty("java.vendor").contains("IBM")) {
            NOT_WORKING_TESTS.addAll(Arrays.asList(notWorkingIBM));
        }
    }
    

    @BeforeClass
    public static void startServers() throws Exception {
        boolean ok = launchServer(CORBADocLitServerImpl.class, true);
        assertTrue("failed to launch server", ok);
        initClient(AbstractTypeTestClient5.class, SERVICE_NAME, PORT_NAME, WSDL_PATH);
    }
    @AfterClass
    public static void deleteRefFile() throws Exception {
        File file = new File("./TypeTest.ref");
        file.delete();
        //for (String s : RUN_TESTS) {
            //System.out.println(s);
        //}
        //System.out.println(RUN_TESTS.size());
    }

    public boolean shouldRunTest(String name) {
        if (!NOT_WORKING_TESTS.contains(name)) {
            boolean b = super.shouldRunTest(name);
            if (b) {
                RUN_TESTS.add(name);
            }
            return b;
        }
        return false;
    }
    
    
    @Test
    public void testA() throws Exception {
    }
    
    protected float[][] getTestFloatData() {
        return new float[][] {{0.0f, 1.0f}, {-1.0f, (float)java.lang.Math.PI},
                              {-100.0f, 100.0f},
                              {Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY}};
    }
    protected double[][] getTestDoubleData() {
        return new double[][] {{0.0f, 1.0f}, {-1, java.lang.Math.PI}, {-100.0, 100.0},
                               {Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY}};
    }

}
