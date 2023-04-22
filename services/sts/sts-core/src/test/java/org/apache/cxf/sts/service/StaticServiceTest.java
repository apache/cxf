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
package org.apache.cxf.sts.service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.cxf.ws.security.sts.provider.STSException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class StaticServiceTest {

    @org.junit.Test
    public void testIsAddressInEndpoints() {
        StaticService service = new StaticService();
        List<String> endpoints =
                Arrays.asList("https://localhost:12345/sts",
                        "https://localhost:54321/sts2",
                        "https://localhost:55555/sts3");
        service.setEndpoints(endpoints);

        endpoints.forEach(e -> assertTrue(service.isAddressInEndpoints(e)));
    }

    @org.junit.Test
    public void testMaximumAllowableAddress() {
        StaticService service = new StaticService();
        List<String> endpoints =
                Arrays.asList("https://localhost:12345/sts.*");
        service.setEndpoints(endpoints);

        StringBuilder sb = new StringBuilder("https://localhost:12345/sts");
        IntStream.range(0, 1000).forEach(i -> sb.append('1'));

        // This should be allowed
        assertTrue(service.isAddressInEndpoints(sb.toString()));

        IntStream.range(0, 7000).forEach(i -> sb.append('1'));

        // This address is too long
        try {
            service.isAddressInEndpoints(sb.toString());
            fail("Failure expected");
        } catch (STSException ex) {
            // expected
        }
    }

}
