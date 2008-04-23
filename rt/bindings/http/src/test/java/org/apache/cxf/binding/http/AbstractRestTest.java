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
package org.apache.cxf.binding.http;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.test.AbstractCXFTest;

public abstract class AbstractRestTest extends AbstractCXFTest {
    boolean debug;

    protected Bus createBus() throws BusException {
        return new SpringBusFactory().createBus();
    }

    protected Document get(String urlStr) throws MalformedURLException, IOException, SAXException,
        ParserConfigurationException {
        return get(urlStr, null);
    }

    protected Document get(String urlStr, Integer resCode) throws MalformedURLException, IOException,
        SAXException, ParserConfigurationException {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection)url.openConnection();

        if (resCode != null) {
            assertEquals(resCode.intValue(), c.getResponseCode());
        }
        InputStream is;
        if (c.getResponseCode() >= 400) {
            is = c.getErrorStream();
        } else {
            is = c.getInputStream();
        }
        return DOMUtils.readXml(is);
    }

    protected Document post(String urlStr, String message) throws MalformedURLException, IOException,
        SAXException, ParserConfigurationException {
        return doMethod(urlStr, message, "POST");
    }

    protected Document put(String urlStr, String message) throws MalformedURLException, IOException,
        SAXException, ParserConfigurationException {
        return doMethod(urlStr, message, "PUT");
    }

    protected Document doMethod(String urlStr, String message, String method) throws MalformedURLException,
        IOException, SAXException, ParserConfigurationException {
        return doMethod(urlStr, message, method, "application/xml");
    }

    protected Document doMethod(String urlStr, String message, String method, String ct)
        throws MalformedURLException, IOException, SAXException, ParserConfigurationException {

        InputStream is = invoke(urlStr, message, method, ct);
        Document res = DOMUtils.readXml(is);

        if (debug) {
            try {
                DOMUtils.writeXml(res, System.out);
            } catch (TransformerException e) {
                throw new RuntimeException(e);
            }
        }
        return res;
    }

    protected byte[] doMethodBytes(String urlStr, String message, String method, String ct)
        throws MalformedURLException, IOException, SAXException, ParserConfigurationException {

        InputStream is = invoke(urlStr, message, method, ct);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        IOUtils.copy(is, out);
        out.close();
        is.close();

        return out.toByteArray();
    }

    private InputStream invoke(String urlStr, String message, String method, String ct)
        throws MalformedURLException, IOException, ProtocolException {
        URL url = new URL(urlStr);
        HttpURLConnection c = (HttpURLConnection)url.openConnection();
        c.setRequestMethod(method);

        if (ct != null) {
            c.setRequestProperty("Content-Type", ct);
        }

        if (message != null) {
            c.setDoOutput(true);
            OutputStream out = c.getOutputStream();
            InputStream msgIs = getResourceAsStream(message);
            assertNotNull(msgIs);

            IOUtils.copy(msgIs, out);
            out.close();
            msgIs.close();
        }

        return c.getInputStream();
    }
}
