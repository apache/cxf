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

package org.apache.cxf.systest.ws.addr_wsdl.jaxwsmm;

import java.io.StringWriter;
import java.util.logging.Logger;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.common.logging.LogUtils;

@WebServiceProvider(
    targetNamespace = "http://messaging/",
    serviceName = "AsyncMessagingService",
    portName = "AsyncMessagingImplPort")
@ServiceMode(value = Service.Mode.MESSAGE)
//FIXME: When using "PAYLOAD" mode, it works; but when using "MESSAGE" mode, it breaks
//@ServiceMode(value = Service.Mode.PAYLOAD)
public class MessageProviderWithAddressingPolicy implements Provider<Source> {

    private static final Logger LOG = LogUtils.getLogger(WSDLAddrPolicyAttachmentJaxwsMMProviderTest.class);

    public MessageProviderWithAddressingPolicy() {
        LOG.info("Creating provider object");
    }

    public Source invoke(Source request) {
        TransformerFactory tfactory = TransformerFactory.newInstance();
        try {
            /*
            tfactory.setAttribute("indent-number", "2");
             */
            Transformer serializer = tfactory.newTransformer();
            // Setup indenting to "pretty print"
            serializer.setOutputProperty(OutputKeys.INDENT, "yes");
            serializer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StringWriter swriter = new StringWriter();
            serializer.transform(request, new StreamResult(swriter));
            swriter.flush();
            LOG.info("Provider received a request\n" + swriter.toString());

        } catch (TransformerException e) {
            e.printStackTrace();
        }
        return null;
    }
}
