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

package org.apache.cxf.staxutils;

import java.io.ByteArrayOutputStream;

import javax.xml.stream.XMLStreamReader;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class DepthRestrictingStreamReaderTest {

    @Test
    public void testReaderOK() throws Exception {
        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("./resources/amazon.xml"));

        DepthRestrictingStreamReader dr = new DepthRestrictingStreamReader(reader,
                                                                           7,
                                                                           4,
                                                                           4);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StaxUtils.copy(dr, bos);
        assertTrue(bos.toString().contains("ItemLookup"));
    }

    @Test
    public void testReaderOKComplex() throws Exception {
        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("./resources/wstrustReqSTRC.xml"));

        DepthRestrictingStreamReader dr = new DepthRestrictingStreamReader(reader,
                                                                           -1,
                                                                           8,
                                                                           3);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StaxUtils.copy(dr, bos);
        assertTrue(bos.toString().contains("RequestSecurityTokenResponse"));
    }

    @Test(expected = DepthExceededStaxException.class)
    public void testElementCountExceeded() throws Exception {
        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("./resources/amazon.xml"));

        DepthRestrictingStreamReader dr = new DepthRestrictingStreamReader(reader,
                                                                           6,
                                                                           4,
                                                                           4);
        StaxUtils.copy(dr, new ByteArrayOutputStream());
    }

    @Test(expected = DepthExceededStaxException.class)
    public void testElementLevelExceeded() throws Exception {
        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("./resources/amazon.xml"));

        DepthRestrictingStreamReader dr = new DepthRestrictingStreamReader(reader,
                                                                           7,
                                                                           3,
                                                                           4);
        StaxUtils.copy(dr, new ByteArrayOutputStream());
    }

    @Test(expected = DepthExceededStaxException.class)
    public void testElementLevelExceededComplex() throws Exception {
        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("./resources/wstrustReqSTRC.xml"));

        DepthRestrictingStreamReader dr = new DepthRestrictingStreamReader(reader,
                                                                           -1,
                                                                           7,
                                                                           3);
        StaxUtils.copy(dr, new ByteArrayOutputStream());
    }

    @Test(expected = DepthExceededStaxException.class)
    public void testInnerElementCountExceeded() throws Exception {
        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("./resources/amazon.xml"));

        DepthRestrictingStreamReader dr = new DepthRestrictingStreamReader(reader,
                                                                           7,
                                                                           4,
                                                                           3);
        StaxUtils.copy(dr, new ByteArrayOutputStream());
    }

    @Test(expected = DepthExceededStaxException.class)
    public void testInnerElementCountExceededComplex() throws Exception {
        XMLStreamReader reader =
            StaxUtils.createXMLStreamReader(getClass().getResourceAsStream("./resources/wstrustReqSTRC.xml"));

        DepthRestrictingStreamReader dr = new DepthRestrictingStreamReader(reader,
                                                                           -1,
                                                                           7,
                                                                           2);
        StaxUtils.copy(dr, new ByteArrayOutputStream());
    }
}