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
package org.apache.cxf.attachment;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class Rfc5987UtilTest {
    private final String input;
    private final String expected;

    public Rfc5987UtilTest(String input, String expected) {
        this.input = input;
        this.expected = expected;
    }

    @Parameterized.Parameters
    public static List<Object[]> params() throws Exception {
        List<Object[]> params = new ArrayList<>();
        params.add(new Object[] {"foo-ä-€.html", "foo-%c3%a4-%e2%82%ac.html"});
        params.add(new Object[]{"世界ーファイル 2.jpg",
            "%e4%b8%96%e7%95%8c%e3%83%bc%e3%83%95%e3%82%a1%e3%82%a4%e3%83%ab%202.jpg"});
        params.add(new Object[]{"foo.jpg", "foo.jpg"});
        return params;
    }

    @Test
    public void test() throws Exception {
        assertEquals(expected, Rfc5987Util.encode(input, StandardCharsets.UTF_8.name()));

        assertEquals(input, Rfc5987Util.decode(expected, StandardCharsets.UTF_8.name()));
    }
}
