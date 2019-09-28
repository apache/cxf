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

package org.apache.cxf.feature.transform;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.logging.Logger;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.io.CachedWriter;
import org.apache.cxf.staxutils.StaxSource;
import org.apache.cxf.staxutils.StaxUtils;

public final class XSLTUtils {
    private static final Logger LOG = LogUtils.getL7dLogger(XSLTUtils.class);

    private XSLTUtils() {

    }

    public static InputStream transform(Templates xsltTemplate, InputStream in) {
        try (InputStream inputStream = in; CachedOutputStream out = new CachedOutputStream()) {
            Source beforeSource = new StaxSource(StaxUtils.createXMLStreamReader(inputStream));

            Transformer trans = xsltTemplate.newTransformer();
            trans.transform(beforeSource, new StreamResult(out));

            return out.getInputStream();
        } catch (IOException e) {
            throw new Fault("GET_CACHED_INPUT_STREAM", LOG, e, e.getMessage());
        } catch (TransformerException e) {
            throw new Fault("XML_TRANSFORM", LOG, e, e.getMessage());
        }
    }

    public static Reader transform(Templates xsltTemplate, Reader inReader) {
        try (Reader reader = inReader; CachedWriter outWriter = new CachedWriter()) {
            Source beforeSource = new StaxSource(StaxUtils.createXMLStreamReader(reader));

            Transformer trans = xsltTemplate.newTransformer();
            trans.transform(beforeSource, new StreamResult(outWriter));

            return outWriter.getReader();
        } catch (IOException e) {
            throw new Fault("GET_CACHED_INPUT_STREAM", LOG, e, e.getMessage());
        } catch (TransformerException e) {
            throw new Fault("XML_TRANSFORM", LOG, e, e.getMessage());
        }
    }

    public static Document transform(Templates xsltTemplate, Document in) {
        try {
            DOMSource beforeSource = new DOMSource(in);

            Document out = DOMUtils.createDocument();

            Transformer trans = xsltTemplate.newTransformer();
            trans.transform(beforeSource, new DOMResult(out));

            return out;
        } catch (TransformerException e) {
            throw new Fault("XML_TRANSFORM", LOG, e, e.getMessage());
        }
    }
}
