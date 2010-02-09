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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class ByteDataSource implements DataSource {
    private String contentType;
    private String name;
    private byte[] data;
    private int offset;
    private int length;

    public ByteDataSource(byte[] dataParam) {
        this(dataParam, 0, dataParam.length);
    }
    public ByteDataSource(byte[] dataParam, String ct) {
        this(dataParam, 0, dataParam.length);
        contentType = ct;
    }

    public ByteDataSource(byte[] dataParam, int offsetParam, int lengthParam) {
        this.data = dataParam;
        this.offset = offsetParam;
        this.length = lengthParam;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] dataParam) {
        this.data = dataParam;
    }

    public void setContentType(String contentTypeParam) {
        this.contentType = contentTypeParam;
    }

    public void setName(String nameParam) {
        this.name = nameParam;
    }

    public String getContentType() {
        return contentType;
    }

    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(data, offset, length);
    }

    public String getName() {
        return name;
    }

    public OutputStream getOutputStream() throws IOException {
        return null;
    }

}
