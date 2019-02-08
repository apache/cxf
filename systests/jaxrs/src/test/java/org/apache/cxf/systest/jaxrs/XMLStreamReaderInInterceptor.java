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
package org.apache.cxf.systest.jaxrs;

import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.StaxUtils;

public class XMLStreamReaderInInterceptor extends AbstractPhaseInterceptor<Message> {

    public XMLStreamReaderInInterceptor() {
        super(Phase.POST_STREAM);
    }

    public void handleMessage(Message m) throws Fault {
        String method = m.get(Message.HTTP_REQUEST_METHOD).toString();
        if ("POST".equals(method)) {
            XMLStreamReader reader =
                StaxUtils.createXMLStreamReader(m.getContent(InputStream.class));
            m.setContent(XMLStreamReader.class, new CustomXmlStreamReader(reader));
        }
    }

}
