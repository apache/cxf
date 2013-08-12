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
package org.apache.cxf.common.xmlschema;

import java.io.InputStream;
import java.io.Reader;

import org.w3c.dom.ls.LSInput;

/**
 * LSInput implementation that can be used in 
 * custom LSResourceResolver implementations for getting
 * schema import and include references resolved.
 */
public class LSInputImpl implements LSInput {

    protected String fPublicId;

    protected String fSystemId;

    protected String fBaseSystemId;

    protected InputStream fByteStream;

    protected Reader fCharStream;

    protected String fData;

    protected String fEncoding;

    protected boolean fCertifiedText;

    public LSInputImpl() {
    }

    public LSInputImpl(String publicId, String systemId, InputStream byteStream) {
        fPublicId = publicId;
        fSystemId = systemId;
        fByteStream = byteStream;
    }

    public InputStream getByteStream() {
        return fByteStream;
    }

    public void setByteStream(InputStream byteStream) {
        fByteStream = byteStream;
    }

    public Reader getCharacterStream() {
        return fCharStream;
    }

    public void setCharacterStream(Reader characterStream) {
        fCharStream = characterStream;
    }

    public String getStringData() {
        return fData;
    }

    public void setStringData(String stringData) {
        fData = stringData;
    }

    public String getEncoding() {
        return fEncoding;
    }

    public void setEncoding(String encoding) {
        fEncoding = encoding;
    }

    public String getPublicId() {
        return fPublicId;
    }

    public void setPublicId(String publicId) {
        fPublicId = publicId;
    }

    public String getSystemId() {
        return fSystemId;
    }

    public void setSystemId(String systemId) {
        fSystemId = systemId;
    }

    public String getBaseURI() {
        return fBaseSystemId;
    }

    public void setBaseURI(String baseURI) {
        fBaseSystemId = baseURI;
    }

    public boolean getCertifiedText() {
        return fCertifiedText;
    }

    public void setCertifiedText(boolean certifiedText) {
        fCertifiedText = certifiedText;
    }

}
