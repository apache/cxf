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
        = "/wsdl/type_test_corba/type_test_corba-corba.wsdl";
    
    
    protected static final QName SERVICE_NAME = new QName("http://apache.org/type_test/doc",
                                                          "TypeTestCORBAService");
    protected static final QName PORT_NAME = new QName("http://apache.org/type_test/doc",
                                                       "TypeTestCORBAPort");

    private static final Set<String> WORKING_TESTS = new HashSet<String>();
    private static final Set<String> NOT_RUN_TESTS = new HashSet<String>();
    static {
        String working[] = new String[] {
            "InheritanceEmptyAllDerivedEmpty",
            "DerivedEmptyBaseEmptyAll",
            "DerivedEmptyBaseEmptyChoice",
            "MultipleOccursSequenceInSequence",
            "StructWithBinary",
            "ChoiceWithBinary",
            "SimpleChoice",
            "UnboundedArray",
            "EmptyAll",
            "StructWithNillables",
            "AnonymousStruct",
            "FixedArray",
            "BoundedArray",
            "CompoundArray",
            "NestedArray",
            "EmptyChoice",
            "Name",
            "Void",
            "Oneway",
            "Byte",
            "Short",
            "UnsignedShort",
            "Int",
            //"UnsignedInt",
            "Long",
            "UnsignedLong",
            "Float",
            "Double",
            //"UnsignedByte",
            "Boolean",
            "String",
            //"StringI18N",
            "Date",
            //"DateTime",
            "Time",
            "GYear",
            "GYearMonth",
            "GMonth",
            "GMonthDay",
            "GDay",
            "Duration",
            "NormalizedString",
            "Token",
            "Language",
            "NMTOKEN",
            //"NMTOKENS",
            "NCName",
            "ID",
            "Decimal",
            "Integer",
            "PositiveInteger",
            "NonPositiveInteger",
            "NegativeInteger",
            "NonNegativeInteger",
            "HexBinary",
            "Base64Binary",
            "AnyURI",
            "ColourEnum",
            "NumberEnum",
            "StringEnum",
            "DecimalEnum",
            "NMTokenEnum",
            "AnyURIEnum",
            "SimpleRestriction",
            "SimpleRestriction4",
        };
        WORKING_TESTS.addAll(Arrays.asList(working));
    }
    

    @BeforeClass
    public static void startServers() throws Exception {
        boolean ok = launchServer(CORBADocLitServerImpl.class, true);
        assertTrue("failed to launch server", ok);
        initClient(AbstractTypeTestClient5.class, SERVICE_NAME, PORT_NAME, WSDL_PATH);
    }
    @AfterClass
    public static void printNotRun() throws Exception {
        File file = new File("./TypeTest.ref");
        file.delete();
        //for (String s : NOT_RUN_TESTS) {
            //System.out.println(s);
        //}
    }

    public boolean shouldRunTest(String name) {
        if (!WORKING_TESTS.contains(name)) {
            NOT_RUN_TESTS.add(name);
            return false;
        }
        return true;
    }
    
    
    @Test
    public void testA() throws Exception {
    }
    
    protected float[][] getTestFloatData() {
        return new float[][] {{0.0f, 1.0f}, {-1.0f, (float)java.lang.Math.PI}, {-100.0f, 100.0f}};
    }
    protected double[][] getTestDoubleData() {
        return new double[][] {{0.0f, 1.0f}, {-1, java.lang.Math.PI}, {-100.0, 100.0}};
    }

}
