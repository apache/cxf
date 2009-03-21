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

package org.apache.cxf.xsdvalidation;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;

import org.apache.cxf.common.logging.LogUtils;

/**
 * 
 */
class DOMLSInput implements LSInput {
    private static final Logger LOG = LogUtils.getL7dLogger(DOMLSInput.class);
    private String systemId;
    private String data;
    
    DOMLSInput(Document doc, String systemId) throws TransformerException {
        this.systemId = systemId;
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        DOMSource source = new DOMSource(doc);
        source.setSystemId(systemId);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        transformer.transform(source, result);
        data = writer.toString();
        LOG.fine(systemId + ": " + data);
        
    }

    /** {@inheritDoc}*/
    public String getBaseURI() {
        return null;
    }

    /** {@inheritDoc}*/
    public InputStream getByteStream() {
        return null;
    }

    /** {@inheritDoc}*/
    public boolean getCertifiedText() {
        return false;
    }

    /** {@inheritDoc}*/
    public Reader getCharacterStream() {
        return null;
    }

    /** {@inheritDoc}*/
    public String getEncoding() {
        return "utf-8";
    }

    /** {@inheritDoc}*/
    public String getPublicId() {
        return null;
    }

    /** {@inheritDoc}*/
    public String getStringData() {
        return data;
    }

    /** {@inheritDoc}*/
    public String getSystemId() {
        return systemId;
    }

    /** {@inheritDoc}*/
    public void setBaseURI(String baseURI) {
    }

    /** {@inheritDoc}*/
    public void setByteStream(InputStream byteStream) {
    }

    /** {@inheritDoc}*/
    public void setCertifiedText(boolean certifiedText) {
    }

    /** {@inheritDoc}*/
    public void setCharacterStream(Reader characterStream) {
    }

    /** {@inheritDoc}*/
    public void setEncoding(String encoding) {
    }

    /** {@inheritDoc}*/
    public void setPublicId(String publicId) {
    }

    /** {@inheritDoc}*/
    public void setStringData(String stringData) {
    }

    /** {@inheritDoc}*/
    public void setSystemId(String systemId) {
    }
}
