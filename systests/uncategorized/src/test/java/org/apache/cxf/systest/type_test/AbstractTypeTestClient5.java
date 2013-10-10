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

import java.util.Arrays;
import java.util.List;

import javax.xml.ws.Holder;

import org.apache.type_test.types1.ComplexRestriction;
import org.apache.type_test.types1.ComplexRestriction2;
import org.apache.type_test.types1.ComplexRestriction3;
import org.apache.type_test.types1.ComplexRestriction4;
import org.apache.type_test.types1.ComplexRestriction5;
import org.apache.type_test.types1.FixedArray;
import org.apache.type_test.types1.MixedArray;
import org.apache.type_test.types1.MixedArray.Array10;
import org.apache.type_test.types1.MixedArray.Array9;
import org.apache.type_test.types1.UnboundedArray;
import org.junit.Test;

public abstract class AbstractTypeTestClient5 extends AbstractTypeTestClient4 {

    //org.apache.type_test.types1.ComplexRestriction

    @Test
    public void testComplexRestriction() throws Exception {
        if (!shouldRunTest("ComplexRestriction")) {
            return;
        }
        // normal case, maxLength=10
        ComplexRestriction x = new ComplexRestriction();
        x.setValue("str_x");
        ComplexRestriction yOrig = new ComplexRestriction();
        yOrig.setValue("string_yyy");
        Holder<ComplexRestriction> y = new Holder<ComplexRestriction>(yOrig);
        Holder<ComplexRestriction> z = new Holder<ComplexRestriction>();

        ComplexRestriction ret;
        if (testDocLiteral) {
            ret = docClient.testComplexRestriction(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexRestriction(x, y, z);            
        } else {
            ret = rpcClient.testComplexRestriction(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testComplexRestriction(): Incorrect value for inout param",
                         x.getValue(), y.value.getValue());
            assertEquals("testComplexRestriction(): Incorrect value for out param",
                         yOrig.getValue(), z.value.getValue());
            assertEquals("testComplexRestriction(): Incorrect return value",
                         x.getValue(), ret.getValue());
        }

        // abnormal case
        if (testDocLiteral || testXMLBinding) {
            try {
                x = new ComplexRestriction();
                x.setValue("string_x");
                yOrig = new ComplexRestriction();
                yOrig.setValue("string_yyyyyy");
                y = new Holder<ComplexRestriction>(yOrig);
                z = new Holder<ComplexRestriction>();

                ret = testDocLiteral ? docClient.testComplexRestriction(x, y, z) 
                        : xmlClient.testComplexRestriction(x, y, z);
                fail("maxLength=10 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        } 
    }

    //org.apache.type_test.types1.ComplexRestriction2

    @Test
    public void testComplexRestriction2() throws Exception {
        if (!shouldRunTest("ComplexRestriction2")) {
            return;
        }
        // normal case, length=10
        ComplexRestriction2 x = new ComplexRestriction2();
        x.setValue("string_xxx");
        ComplexRestriction2 yOrig = new ComplexRestriction2();
        yOrig.setValue("string_yyy");
        Holder<ComplexRestriction2> y = new Holder<ComplexRestriction2>(yOrig);
        Holder<ComplexRestriction2> z = new Holder<ComplexRestriction2>();

        ComplexRestriction2 ret;
        if (testDocLiteral) {
            ret = docClient.testComplexRestriction2(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexRestriction2(x, y, z);
        } else {
            ret = rpcClient.testComplexRestriction2(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testComplexRestriction2(): Incorrect value for inout param",
                         x.getValue(), y.value.getValue());
            assertEquals("testComplexRestriction2(): Incorrect value for out param",
                         yOrig.getValue(), z.value.getValue());
            assertEquals("testComplexRestriction2(): Incorrect return value",
                         x.getValue(), ret.getValue());
        }

        // abnormal case
        if (testDocLiteral || testXMLBinding) {
            try {
                x = new ComplexRestriction2();
                x.setValue("str_x");
                yOrig = new ComplexRestriction2();
                yOrig.setValue("string_yyy");
                y = new Holder<ComplexRestriction2>(yOrig);
                z = new Holder<ComplexRestriction2>();

                ret = testDocLiteral ? docClient.testComplexRestriction2(x, y, z) 
                        : xmlClient.testComplexRestriction2(x, y, z);
                fail("length=10 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    //org.apache.type_test.types1.ComplexRestriction3

    @Test
    public void testComplexRestriction3() throws Exception {
        if (!shouldRunTest("ComplexRestriction3")) {
            return;
        }
        // normal case, maxLength=10 for ComplexRestriction
        // && minLength=5 for ComplexRestriction3
        ComplexRestriction3 x = new ComplexRestriction3();
        x.setValue("str_x");
        ComplexRestriction3 yOrig = new ComplexRestriction3();
        yOrig.setValue("string_yyy");
        Holder<ComplexRestriction3> y = new Holder<ComplexRestriction3>(yOrig);
        Holder<ComplexRestriction3> z = new Holder<ComplexRestriction3>();

        ComplexRestriction3 ret;
        if (testDocLiteral) {
            ret = docClient.testComplexRestriction3(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexRestriction3(x, y, z);
        } else {
            ret = rpcClient.testComplexRestriction3(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testComplexRestriction3(): Incorrect value for inout param",
                         x.getValue(), y.value.getValue());
            assertEquals("testComplexRestriction3(): Incorrect value for out param",
                         yOrig.getValue(), z.value.getValue());
            assertEquals("testComplexRestriction3(): Incorrect return value",
                         x.getValue(), ret.getValue());
        }

        // abnormal cases
        if (testDocLiteral || testXMLBinding) {
            try {
                x = new ComplexRestriction3();
                x.setValue("str");
                y = new Holder<ComplexRestriction3>(yOrig);
                z = new Holder<ComplexRestriction3>();
                ret = testDocLiteral ? docClient.testComplexRestriction3(x, y, z) 
                        : xmlClient.testComplexRestriction3(x, y, z);
                fail("maxLength=10 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
            try {
                x = new ComplexRestriction3();
                x.setValue("string_x");
                yOrig = new ComplexRestriction3();
                yOrig.setValue("string_yyyyyy");
                y = new Holder<ComplexRestriction3>(yOrig);
                z = new Holder<ComplexRestriction3>();
                ret = testDocLiteral ? docClient.testComplexRestriction3(x, y, z) 
                        : xmlClient.testComplexRestriction3(x, y, z);
                fail("maxLength=10 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        } 
    }

    //org.apache.type_test.types1.ComplexRestriction4

    @Test
    public void testComplexRestriction4() throws Exception {
        if (!shouldRunTest("ComplexRestriction4")) {
            return;
        }
        // normal case, maxLength=10 for ComplexRestriction
        // && maxLength=5 for ComplexRestriction4
        ComplexRestriction4 x = new ComplexRestriction4();
        x.setValue("str_x");
        ComplexRestriction4 yOrig = new ComplexRestriction4();
        yOrig.setValue("y");
        Holder<ComplexRestriction4> y = new Holder<ComplexRestriction4>(yOrig);
        Holder<ComplexRestriction4> z = new Holder<ComplexRestriction4>();

        ComplexRestriction4 ret;
        if (testDocLiteral) {
            ret = docClient.testComplexRestriction4(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexRestriction4(x, y, z);
        } else {
            ret = rpcClient.testComplexRestriction4(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testComplexRestriction4(): Incorrect value for inout param",
                         x.getValue(), y.value.getValue());
            assertEquals("testComplexRestriction4(): Incorrect value for out param",
                         yOrig.getValue(), z.value.getValue());
            assertEquals("testComplexRestriction4(): Incorrect return value",
                         x.getValue(), ret.getValue());
        }

        // abnormal case
        if (testDocLiteral || testXMLBinding) {
            try {
                x = new ComplexRestriction4();
                x.setValue("str_xxx");
                y = new Holder<ComplexRestriction4>(yOrig);
                z = new Holder<ComplexRestriction4>();
                ret = testDocLiteral ? docClient.testComplexRestriction4(x, y, z) 
                        : xmlClient.testComplexRestriction4(x, y, z);
                fail("maxLength=5 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    //org.apache.type_test.types1.ComplexRestriction5
    @Test
    public void testComplexRestriction5() throws Exception {
        if (!shouldRunTest("ComplexRestriction5")) {
            return;
        }
        // normal case, maxLength=50 && minLength=5 for ComplexRestriction5
        ComplexRestriction5 x = new ComplexRestriction5();
        x.setValue("http://www.iona.com");
        ComplexRestriction5 yOrig = new ComplexRestriction5();
        yOrig.setValue("http://www.iona.com/info/services/oss/");
        Holder<ComplexRestriction5> y = new Holder<ComplexRestriction5>(yOrig);
        Holder<ComplexRestriction5> z = new Holder<ComplexRestriction5>();

        ComplexRestriction5 ret;
        if (testDocLiteral) {
            ret = docClient.testComplexRestriction5(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testComplexRestriction5(x, y, z);
        } else {
            ret = rpcClient.testComplexRestriction5(x, y, z);
        }
        if (!perfTestOnly) {
            assertEquals("testComplexRestriction5(): Incorrect value for inout param",
                         x.getValue(), y.value.getValue());
            assertEquals("testComplexRestriction5(): Incorrect value for out param",
                         yOrig.getValue(), z.value.getValue());
            assertEquals("testComplexRestriction5(): Incorrect return value",
                         x.getValue(), ret.getValue());
        }

        // abnormal cases
        if (testDocLiteral || testXMLBinding) {
            try {
                x = new ComplexRestriction5();
                x.setValue("uri");
                y = new Holder<ComplexRestriction5>(yOrig);
                z = new Holder<ComplexRestriction5>();
                ret = docClient.testComplexRestriction5(x, y, z);
                fail("maxLength=50 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }

            try {
                x = new ComplexRestriction5();
                x.setValue("http://www.iona.com");
                yOrig = new ComplexRestriction5();
                yOrig.setValue("http://www.iona.com/info/services/oss/info_services_oss_train.html");
                y = new Holder<ComplexRestriction5>(yOrig);
                z = new Holder<ComplexRestriction5>();
                ret = testDocLiteral ? docClient.testComplexRestriction5(x, y, z) 
                        : xmlClient.testComplexRestriction5(x, y, z);
                fail("maxLength=50 && minLength=5 restriction is violated.");
            } catch (Exception ex) {
                //ex.printStackTrace();
            }
        }
    }

    //org.apache.type_test.types1.MixedArray

    protected boolean equals(MixedArray x, MixedArray y) {
        boolean simpleArraysEqual = x.getArray1().equals(y.getArray1())
            && x.getArray3().equals(y.getArray3())
            && x.getArray5().equals(y.getArray5())
            && x.getArray7().equals(y.getArray7());
        boolean complexArraysEqual = x.getArray2().getItem().equals(y.getArray2().getItem())
            && x.getArray4().getItem().equals(y.getArray4().getItem())
            && x.getArray6().getItem().equals(y.getArray6().getItem())
            && x.getArray8().getItem().equals(y.getArray8().getItem())
            && listsOfArray9equal(x.getArray9(), y.getArray9())
            && listsOfArray10equal(x.getArray10(), y.getArray10());
        return simpleArraysEqual && complexArraysEqual;
    }

    protected boolean listsOfArray9equal(List<Array9> x, List<Array9> y) {
        if ((x == null && y == null) || (x == y)) {
            return true;
        }
        if (x == null || y == null || x.size() != y.size()) {
            return false;
        }
        for (int i = 0; i < x.size(); i++) {
            Array9 a1 = x.get(i);
            Array9 a2 = y.get(i);
            if (a1 == null && a2 == null) {
                continue;
            }
            if (a1 == null || a2 == null) {
                return false;
            }
            if (a1.getItem() == null && a2.getItem() == null) {
                continue;
            }
            if (a1.getItem() == null || a2.getItem() == null) {
                return false;
            }
            if (!a1.getItem().equals(a2.getItem())) {
                return false;
            }
        }
        return true;
    }
    protected boolean listsOfArray10equal(List<Array10> x, List<Array10> y) {
        if ((x == null && y == null) || (x == y)) {
            return true;
        }
        if (x == null || y == null || x.size() != y.size()) {
            return false;
        }
        for (int i = 0; i < x.size(); i++) {
            Array10 a1 = x.get(i);
            Array10 a2 = y.get(i);
            if (a1 == null && a2 == null) {
                continue;
            }
            if (a1 == null || a2 == null) {
                return false;
            }
            if (a1.getItem() == null && a2.getItem() == null) {
                continue;
            }
            if (a1.getItem() == null || a2.getItem() == null) {
                return false;
            }
            if (!a1.getItem().equals(a2.getItem())) {
                return false;
            }
        }
        return true;
    }

    @Test
    public void testMixedArray() throws Exception {
        if (!shouldRunTest("MixedArray")) {
            return;
        }
        MixedArray x = new MixedArray();
        x.getArray1().addAll(Arrays.asList("AAA", "BBB", "CCC"));
        x.setArray2(new UnboundedArray());
        x.getArray2().getItem().addAll(Arrays.asList("aaa", "bbb", "ccc"));
        x.getArray3().addAll(Arrays.asList("DDD", "EEE", "FFF"));
        x.setArray4(new FixedArray());
        x.getArray4().getItem().addAll(Arrays.asList(1, 2, 3));
        x.getArray5().addAll(Arrays.asList("GGG", "HHH", "III"));
        x.setArray6(new MixedArray.Array6());
        x.getArray6().getItem().addAll(Arrays.asList("ggg", "hhh", "iii"));
        x.getArray7().addAll(Arrays.asList("JJJ", "KKK", "LLL"));
        x.setArray8(new MixedArray.Array8());
        x.getArray8().getItem().addAll(Arrays.asList(4, 5, 6));
        Array9 array91 = new MixedArray.Array9();
        Array9 array92 = new MixedArray.Array9();
        Array9 array93 = new MixedArray.Array9();
        array91.setItem("MMM");
        array92.setItem("NNN");
        array93.setItem("OOO");
        x.getArray9().addAll(Arrays.asList(array91, array92, array93));
        Array10 array101 = new MixedArray.Array10();
        Array10 array102 = new MixedArray.Array10();
        Array10 array103 = new MixedArray.Array10();
        array101.setItem("PPP");
        array102.setItem("QQQ");
        array103.setItem("RRR");
        x.getArray10().addAll(Arrays.asList(array101, array102, array103));
        x.getArray11().addAll(Arrays.asList("AAA", "BBB", "CCC"));

        MixedArray yOrig = new MixedArray();
        yOrig.getArray1().addAll(Arrays.asList("XXX", "YYY", "ZZZ"));
        yOrig.setArray2(new UnboundedArray());
        yOrig.getArray2().getItem().addAll(Arrays.asList("xxx", "yyy", "zzz"));
        yOrig.getArray3().addAll(Arrays.asList("DDD", "EEE", "FFF"));
        yOrig.setArray4(new FixedArray());
        yOrig.getArray4().getItem().addAll(Arrays.asList(1, 2, 3));
        yOrig.getArray5().addAll(Arrays.asList("GGG", "HHH", "III"));
        yOrig.setArray6(new MixedArray.Array6());
        yOrig.getArray6().getItem().addAll(Arrays.asList("ggg", "hhh", "iii"));
        yOrig.getArray7().addAll(Arrays.asList("JJJ", "KKK", "LLL"));
        yOrig.setArray8(new MixedArray.Array8());
        yOrig.getArray8().getItem().addAll(Arrays.asList(4, 5, 6));
        array91 = new MixedArray.Array9();
        array92 = new MixedArray.Array9();
        array93 = new MixedArray.Array9();
        array91.setItem("MMM");
        array92.setItem("NNN");
        array93.setItem("OOO");
        yOrig.getArray9().addAll(Arrays.asList(array91, array92, array93));
        array101 = new MixedArray.Array10();
        array102 = new MixedArray.Array10();
        array103 = new MixedArray.Array10();
        array101.setItem("PPP");
        array102.setItem("QQQ");
        array103.setItem("RRR");
        yOrig.getArray10().addAll(Arrays.asList(array101, array102, array103));
        yOrig.getArray11().addAll(Arrays.asList("XXX", "YYY", "ZZZ"));

        Holder<MixedArray> y = new Holder<MixedArray>(yOrig);
        Holder<MixedArray> z = new Holder<MixedArray>();
        MixedArray ret;
        if (testDocLiteral) {
            ret = docClient.testMixedArray(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testMixedArray(x, y, z);
        } else {
            ret = rpcClient.testMixedArray(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testMixedArray(): Incorrect value for inout param", equals(x, y.value));
            assertTrue("testMixedArray(): Incorrect value for out param", equals(yOrig, z.value));
            assertTrue("testMixedArray(): Incorrect return value", equals(x, ret));
        }

        // checkstyle complained otherwise...
        assertEmptyCollectionsHandled(x, yOrig);
    }

    /**
     * @param x
     * @param yOrig
     */
    private void assertEmptyCollectionsHandled(MixedArray x, MixedArray yOrig) {
        Holder<MixedArray> y;
        Holder<MixedArray> z;
        MixedArray ret;
        // empty collections. may be tested only for sequences, i.e., for lists array1, array2, array5, array6,
        // array9 and array11.
        // array3, array4, array7, array8 and array10 must have 3 elements
        // empty them
        x.getArray1().clear();
        x.setArray2(new UnboundedArray());
        x.getArray5().clear();
        x.setArray6(new MixedArray.Array6());
        x.getArray9().clear();
        x.getArray11().clear();

        // empty them
        yOrig.getArray1().clear();
        yOrig.setArray2(new UnboundedArray());
        yOrig.getArray5().clear();
        yOrig.setArray6(new MixedArray.Array6());
        yOrig.getArray9().clear();
        yOrig.getArray11().clear();

        y = new Holder<MixedArray>(yOrig);
        z = new Holder<MixedArray>();
        if (testDocLiteral) {
            ret = docClient.testMixedArray(x, y, z);
        } else if (testXMLBinding) {
            ret = xmlClient.testMixedArray(x, y, z);
        } else {
            ret = rpcClient.testMixedArray(x, y, z);
        }
        if (!perfTestOnly) {
            assertTrue("testMixedArray(): Incorrect value for inout param", equals(x, y.value));
            assertTrue("testMixedArray(): Incorrect value for out param", equals(yOrig, z.value));
            assertTrue("testMixedArray(): Incorrect return value", equals(x, ret));
        }
    }

}
