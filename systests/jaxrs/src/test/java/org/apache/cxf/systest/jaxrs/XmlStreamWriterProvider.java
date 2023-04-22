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
import java.io.OutputStream;

import javax.xml.stream.XMLStreamWriter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;

public class XmlStreamWriterProvider implements ContainerResponseFilter {

    public void filter(ContainerRequestContext reqC, ContainerResponseContext respC) throws IOException {
        Message m = JAXRSUtils.getCurrentMessage();
        OperationResourceInfo ori = m.getExchange().get(OperationResourceInfo.class);
        String method = ori.getHttpMethod();
        if ("PUT".equals(method)) {
            XMLStreamWriter writer =
                StaxUtils.createXMLStreamWriter(m.getContent(OutputStream.class));
            m.setContent(XMLStreamWriter.class, new CustomXmlStreamWriter(writer));
        }
    }

}
