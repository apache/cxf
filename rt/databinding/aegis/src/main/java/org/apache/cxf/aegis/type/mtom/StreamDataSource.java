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

package org.apache.cxf.aegis.type.mtom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import jakarta.activation.DataSource;

/**
 *
 */
public class StreamDataSource implements DataSource {

    private String contentType;
    private InputStream stream;

    public StreamDataSource(String contentType, InputStream stream) {
        this.contentType = contentType;
        this.stream = stream;
    }

    /** {@inheritDoc}*/
    public String getContentType() {
        return contentType;
    }

    /** {@inheritDoc}*/
    public InputStream getInputStream() throws IOException {
        return stream;
    }

    /** {@inheritDoc}*/
    public String getName() {
        return null;
    }

    /** {@inheritDoc}*/
    public OutputStream getOutputStream() throws IOException {
        return null;
    }

}
