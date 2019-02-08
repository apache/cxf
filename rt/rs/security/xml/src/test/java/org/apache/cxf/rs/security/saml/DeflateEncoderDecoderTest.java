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
package org.apache.cxf.rs.security.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import org.apache.cxf.common.util.Base64Utility;
import org.apache.cxf.helpers.IOUtils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class DeflateEncoderDecoderTest {

    @Test(expected = DataFormatException.class)
    public void testInvalidContent() throws Exception {
        DeflateEncoderDecoder inflater = new DeflateEncoderDecoder();
        inflater.inflateToken("invalid_grant".getBytes());
    }

    @Test(expected = DataFormatException.class)
    public void testInvalidContentAfterBase64() throws Exception {
        DeflateEncoderDecoder inflater = new DeflateEncoderDecoder();
        byte[] base64decoded = Base64Utility.decode("invalid_grant");
        inflater.inflateToken(base64decoded);
    }

    @Test
    public void testInflateDeflate() throws Exception {
        DeflateEncoderDecoder inflater = new DeflateEncoderDecoder();
        byte[] deflated = inflater.deflateToken("valid_grant".getBytes());
        InputStream is = inflater.inflateToken(deflated);
        assertNotNull(is);
        assertEquals("valid_grant", IOUtils.readStringFromStream(is));
    }

    @Test
    public void testInflateDeflateBase64() throws Exception {
        DeflateEncoderDecoder inflater = new DeflateEncoderDecoder();
        byte[] deflated = inflater.deflateToken("valid_grant".getBytes());
        String base64String = Base64Utility.encode(deflated);
        byte[] base64decoded = Base64Utility.decode(base64String);
        InputStream is = inflater.inflateToken(base64decoded);
        assertNotNull(is);
        assertEquals("valid_grant", IOUtils.readStringFromStream(is));
    }
    @Test
    public void testInflateDeflateWithTokenDuplication() throws Exception {
        String token = "valid_grant valid_grant valid_grant valid_grant valid_grant valid_grant";

        DeflateEncoderDecoder deflateEncoderDecoder = new DeflateEncoderDecoder();
        byte[] deflatedToken = deflateEncoderDecoder.deflateToken(token.getBytes());

        String cxfInflatedToken = IOUtils
                .toString(deflateEncoderDecoder.inflateToken(deflatedToken));

        String streamInflatedToken = IOUtils.toString(
                new InflaterInputStream(new ByteArrayInputStream(deflatedToken),
                        new Inflater(true)));

        assertEquals(streamInflatedToken, token);
        assertEquals(cxfInflatedToken, token);
    }
}