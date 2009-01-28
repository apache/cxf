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

package org.apache.cxf.jaxrs.ext.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.activation.DataSource;

public class InputStreamDataSource implements DataSource {

    private InputStream is;
    private String contentType;
    private String name;
    
    public InputStreamDataSource(InputStream is, String contentType) {
        this.is = is;
        this.contentType = contentType;
    }
    
    public InputStreamDataSource(InputStream is, String contentType, String name) {
        this.is = is;
        this.contentType = contentType;
        this.name = name;
    }
    
    public String getContentType() {
        return contentType;
    }

    public InputStream getInputStream() throws IOException {
        return is;
    }

    public String getName() {
        return name;
    }

    public OutputStream getOutputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

}
