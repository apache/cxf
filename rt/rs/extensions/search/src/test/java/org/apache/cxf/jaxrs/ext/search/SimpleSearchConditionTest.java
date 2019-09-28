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
package org.apache.cxf.jaxrs.ext.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SimpleSearchConditionTest {

    private static SearchCondition<SingleAttr> cEq;
    private static SearchCondition<SingleAttr> cGt;
    private static SearchCondition<SingleAttr> cGeq;
    private static SearchCondition<SingleAttr> cLt;
    private static SearchCondition<SingleAttr> cLeq;

    private static SingleAttr attr = new SingleAttr("bbb");
    private static SingleAttr attrGreater = new SingleAttr("ccc");
    private static SingleAttr attrLesser = new SingleAttr("aaa");

    // TODO 1. comparison with multiple values
    // TODO 2. comparison when getter returns null/throws exception
    private static DoubleAttr attr2Vals = new DoubleAttr("bbb", "ccc");
    private static DoubleAttr attr2ValsGreater = new DoubleAttr("ccc", "ddd");
    private static DoubleAttr attr2ValsLesser = new DoubleAttr("aaa", "bbb");

    private static DoubleAttr attr1Val = new DoubleAttr("bbb", null);
    private static DoubleAttr attr1ValGreater = new DoubleAttr("ccc", "ingored");
    private static DoubleAttr attr1ValLesser = new DoubleAttr("aaa", "ingored");

    private static SearchCondition<DoubleAttr> dc1Eq;
    private static SearchCondition<DoubleAttr> dc1Gt;
    private static SearchCondition<DoubleAttr> dc1Geq;
    private static SearchCondition<DoubleAttr> dc1Lt;
    private static SearchCondition<DoubleAttr> dc1Leq;

    private static SearchCondition<DoubleAttr> dc2Eq;
    private static SearchCondition<DoubleAttr> dc2Gt;
    private static SearchCondition<DoubleAttr> dc2Geq;
    private static SearchCondition<DoubleAttr> dc2Lt;
    private static SearchCondition<DoubleAttr> dc2Leq;

    private static List<ConditionType> supported = Arrays.asList(ConditionType.EQUALS,
                                                                 ConditionType.NOT_EQUALS,
                                                                 ConditionType.GREATER_OR_EQUALS,
                                                                 ConditionType.GREATER_THAN,
                                                                 ConditionType.LESS_OR_EQUALS,
                                                                 ConditionType.LESS_THAN);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        cEq = new SimpleSearchCondition<>(ConditionType.EQUALS, attr);
        cGt = new SimpleSearchCondition<>(ConditionType.GREATER_THAN, attr);
        cGeq = new SimpleSearchCondition<>(ConditionType.GREATER_OR_EQUALS, attr);
        cLt = new SimpleSearchCondition<>(ConditionType.LESS_THAN, attr);
        cLeq = new SimpleSearchCondition<>(ConditionType.LESS_OR_EQUALS, attr);

        dc1Eq = new SimpleSearchCondition<>(ConditionType.EQUALS, attr1Val);
        dc1Gt = new SimpleSearchCondition<>(ConditionType.GREATER_THAN, attr1Val);
        dc1Geq = new SimpleSearchCondition<>(ConditionType.GREATER_OR_EQUALS, attr1Val);
        dc1Lt = new SimpleSearchCondition<>(ConditionType.LESS_THAN, attr1Val);
        dc1Leq = new SimpleSearchCondition<>(ConditionType.LESS_OR_EQUALS, attr1Val);

        dc2Eq = new SimpleSearchCondition<>(ConditionType.EQUALS, attr2Vals);
        dc2Gt = new SimpleSearchCondition<>(ConditionType.GREATER_THAN, attr2Vals);
        dc2Geq = new SimpleSearchCondition<>(ConditionType.GREATER_OR_EQUALS, attr2Vals);
        dc2Lt = new SimpleSearchCondition<>(ConditionType.LESS_THAN, attr2Vals);
        dc2Leq = new SimpleSearchCondition<>(ConditionType.LESS_OR_EQUALS, attr2Vals);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtorNull1() {
        new SimpleSearchCondition<SingleAttr>((ConditionType)null, attr);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtorNull2() {
        new SimpleSearchCondition<SingleAttr>(ConditionType.LESS_THAN, null);
    }

    @Test
    public void testCtorCondSupported() {
        for (ConditionType ct : ConditionType.values()) {
            try {
                new SimpleSearchCondition<SingleAttr>(ct, attr);
                if (!supported.contains(ct)) {
                    fail(String.format("Not supported type %s should throw exception", ct.name()));
                }
            } catch (IllegalArgumentException e) {
                if (supported.contains(ct)) {
                    fail(String.format("Supported type %s should not throw exception", ct.name()));
                }
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCtorMapNull() {
        new SimpleSearchCondition<SingleAttr>((Map<String, ConditionType>)null, attr);
    }

    @Test
    public void testCtorMapCondSupported() {
        for (ConditionType ct : ConditionType.values()) {
            try {
                Map<String, ConditionType> map = new HashMap<>();
                map.put("foo", ct);
                new SimpleSearchCondition<SingleAttr>(map, attr);
                if (!supported.contains(ct)) {
                    fail(String.format("Not supported type %s should throw exception", ct.name()));
                }
            } catch (IllegalArgumentException e) {
                if (supported.contains(ct)) {
                    fail(String.format("Supported type %s should not throw exception", ct.name()));
                }
            }
        }
    }

    @Test
    public void testGetCondition() {
        assertEquals(cLeq.getCondition(), attr);
    }

    @Test
    public void testGetConditionType() {
        assertEquals(cEq.getConditionType(), ConditionType.EQUALS);
        assertEquals(cLt.getConditionType(), ConditionType.LESS_THAN);
    }

    @Test
    public void testGetConditions() {
        assertEquals(cGt.getSearchConditions(), null);
    }

    @Test
    public void testIsMetEq() {
        assertTrue(cEq.isMet(attr));
        assertFalse(cEq.isMet(attrGreater));
    }

    @Test
    public void testIsMetGt() {
        assertTrue(cGt.isMet(attrGreater));
        assertFalse(cGt.isMet(attr));
        assertFalse(cGt.isMet(attrLesser));
    }

    @Test
    public void testIsMetGeq() {
        assertTrue(cGeq.isMet(attrGreater));
        assertTrue(cGeq.isMet(attr));
        assertFalse(cGeq.isMet(attrLesser));
    }

    @Test
    public void testIsMetLt() {
        assertFalse(cLt.isMet(attrGreater));
        assertFalse(cLt.isMet(attr));
        assertTrue(cLt.isMet(attrLesser));
    }

    @Test
    public void testIsMetLeq() {
        assertFalse(cLeq.isMet(attrGreater));
        assertTrue(cLeq.isMet(attr));
        assertTrue(cLeq.isMet(attrLesser));
    }

    @Test
    public void testIsMetEqPrimitive() {
        assertTrue(new SimpleSearchCondition<String>(ConditionType.EQUALS, "foo").isMet("foo"));
    }

    @Test
    public void testIsMetGtPrimitive() {
        assertTrue(new SimpleSearchCondition<Float>(ConditionType.GREATER_THAN, 1.5f).isMet(2.5f));
    }

    @Test
    public void testIsMetLtPrimitive() {
        assertTrue(new SimpleSearchCondition<Integer>(ConditionType.LESS_THAN, 10).isMet(5));
    }

    @Test
    public void testFindAll() {
        List<SingleAttr> inputs = Arrays.asList(attr, attrGreater, attrLesser);
        List<SingleAttr> found = Arrays.asList(attr, attrGreater);
        assertEquals(found, cGeq.findAll(inputs));
    }

    @Test
    public void testIsMetEqDouble1Val() {
        assertFalse(dc1Eq.isMet(attr1ValGreater));
        assertTrue(dc1Eq.isMet(attr1Val));
        assertFalse(dc1Eq.isMet(attr1ValLesser));
    }

    @Test
    public void testIsMetGtDouble1Val() {
        assertTrue(dc1Gt.isMet(attr1ValGreater));
        assertFalse(dc1Gt.isMet(attr1Val));
        assertFalse(dc1Gt.isMet(attr1ValLesser));
    }

    @Test
    public void testIsMetGeqDouble1Val() {
        assertTrue(dc1Geq.isMet(attr1ValGreater));
        assertTrue(dc1Geq.isMet(attr1Val));
        assertFalse(dc1Geq.isMet(attr1ValLesser));
    }

    @Test
    public void testIsMetLtDouble1Val() {
        assertFalse(dc1Lt.isMet(attr1ValGreater));
        assertFalse(dc1Lt.isMet(attr1Val));
        assertTrue(dc1Lt.isMet(attr1ValLesser));
    }

    @Test
    public void testIsMetLeqDouble1Val() {
        assertFalse(dc1Leq.isMet(attr1ValGreater));
        assertTrue(dc1Leq.isMet(attr1Val));
        assertTrue(dc1Leq.isMet(attr1ValLesser));
    }

    @Test
    public void testIsMetEqDouble2Vals() {
        assertFalse(dc2Eq.isMet(attr2ValsGreater));
        assertTrue(dc2Eq.isMet(attr2Vals));
        assertFalse(dc2Eq.isMet(attr2ValsLesser));
    }

    @Test
    public void testIsMetGtDouble2Vals() {
        assertTrue(dc2Gt.isMet(attr2ValsGreater));
        assertFalse(dc2Gt.isMet(attr2Vals));
        assertFalse(dc2Gt.isMet(attr2ValsLesser));
    }

    @Test
    public void testIsMetGeqDouble2Vals() {
        assertTrue(dc2Geq.isMet(attr2ValsGreater));
        assertTrue(dc2Geq.isMet(attr2Vals));
        assertFalse(dc2Geq.isMet(attr2ValsLesser));
    }

    @Test
    public void testIsMetLtDouble2Vals() {
        assertFalse(dc2Lt.isMet(attr2ValsGreater));
        assertFalse(dc2Lt.isMet(attr2Vals));
        assertTrue(dc2Lt.isMet(attr2ValsLesser));
    }

    @Test
    public void testIsMetLeqDouble2Vals() {
        assertFalse(dc2Leq.isMet(attr2ValsGreater));
        assertTrue(dc2Leq.isMet(attr2Vals));
        assertTrue(dc2Leq.isMet(attr2ValsLesser));
    }

    @Test
    public void testIsMetMappedOperators() {
        Map<String, ConditionType> map = new HashMap<>();
        map.put("foo", ConditionType.LESS_THAN);
        map.put("bar", ConditionType.GREATER_THAN);

        // expression "template.getFoo() < pojo.getFoo() & template.getBar() > pojo.getBar()"
        assertTrue(new SimpleSearchCondition<DoubleAttr>(map, new DoubleAttr("bbb", "ccc"))
            .isMet(new DoubleAttr("aaa", "ddd")));

        // expression "template.getBar() > pojo.getBar()"
        assertTrue(new SimpleSearchCondition<DoubleAttr>(map, new DoubleAttr(null, "ccc"))
            .isMet(new DoubleAttr("!not-interpreted!", "ddd")));
    }

    @Test
    public void testIsMetWildcardEnds() {
        SimpleSearchCondition<String> ssc = new SimpleSearchCondition<>(ConditionType.EQUALS, "bar*");
        assertTrue(ssc.isMet("bar"));
        assertTrue(ssc.isMet("barbaz"));
        assertFalse(ssc.isMet("foobar"));
    }

    @Test
    public void testIsMetWildcardStarts() {
        SimpleSearchCondition<String> ssc = new SimpleSearchCondition<>(ConditionType.EQUALS, "*bar");
        assertTrue(ssc.isMet("bar"));
        assertFalse(ssc.isMet("barbaz"));
        assertTrue(ssc.isMet("foobar"));
    }

    @Test
    public void testIsMetWildcardStartsEnds() {
        SimpleSearchCondition<String> ssc = new SimpleSearchCondition<>(ConditionType.EQUALS, "*bar*");
        assertTrue(ssc.isMet("bar"));
        assertTrue(ssc.isMet("barbaz"));
        assertTrue(ssc.isMet("foobar"));
    }

    @Test
    public void testIsMetWildcardMultiAsterisk() {
        SimpleSearchCondition<String> ssc = new SimpleSearchCondition<>(ConditionType.EQUALS, "*ba*r*");
        assertFalse(ssc.isMet("bar"));
        assertTrue(ssc.isMet("ba*r"));
        assertTrue(ssc.isMet("fooba*r"));
        assertTrue(ssc.isMet("fooba*rbaz"));
        assertFalse(ssc.isMet("foobarbaz"));
    }

    static class SingleAttr {
        private String foo;

        SingleAttr(String foo) {
            this.foo = foo;
        }

        public String getFoo() {
            return foo;
        }

        // this should not be used by "isMet" (is not public)
        @SuppressWarnings("unused")
        private String getBar() {
            return "it's private!";
        }
    }

    static class DoubleAttr {
        private String foo;
        private String bar;

        DoubleAttr(String foo, String bar) {
            this.foo = foo;
            this.bar = bar;
        }

        public String getFoo() {
            return foo;
        }

        public String getBar() {
            return bar;
        }
    }
}
