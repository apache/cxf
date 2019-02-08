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

package org.apache.cxf.jaxrs.impl;

import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

import org.apache.cxf.message.Message;
import org.apache.cxf.transport.http.AbstractHTTPDestination;

public class ServletOutputStreamFilter extends ServletOutputStream {

    private Message m;
    private ServletOutputStream os;

    public ServletOutputStreamFilter(ServletOutputStream os, Message m) {
        this.os = os;
        this.m = m;
    }

    @Override
    public void write(int b) throws IOException {
        setComittedStatus();
        os.write(b);
    }

    @Override
    public void write(byte[] b) throws IOException {
        setComittedStatus();
        os.write(b);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        setComittedStatus();
        os.write(b, off, len);
    }

    private void setComittedStatus() {
        m.getExchange().put(AbstractHTTPDestination.RESPONSE_COMMITED, Boolean.TRUE);
    }

    //Servlet 3.1 additions
    public boolean isReady() {
        return os.isReady();
    }
    public void setWriteListener(WriteListener writeListener) {
        os.setWriteListener(writeListener);
    }
}
