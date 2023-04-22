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

import java.io.IOException;
import java.io.InputStream;

import javax.xml.stream.XMLStreamReader;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MultivaluedMap;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;

public class XmlStreamReaderProvider implements ContainerRequestFilter {

    @Context
    private MessageContext context;

    public void filter(ContainerRequestContext c) throws IOException {
        String method = context.get(Message.HTTP_REQUEST_METHOD).toString();

        if ("PUT".equals(method)) {
            MultivaluedMap<String, String> map = context.getUriInfo().getPathParameters();
            if (!"123".equals(map.getFirst("id"))) {
                throw new RuntimeException();
            }
            Message m = JAXRSUtils.getCurrentMessage();
            XMLStreamReader reader =
                StaxUtils.createXMLStreamReader(m.getContent(InputStream.class));
            m.setContent(XMLStreamReader.class,
                                                      new CustomXmlStreamReader(reader));
        }
    }

}
