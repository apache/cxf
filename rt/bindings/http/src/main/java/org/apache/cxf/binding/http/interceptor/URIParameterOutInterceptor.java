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
package org.apache.cxf.binding.http.interceptor;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.apache.cxf.binding.http.URIMapper;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.service.model.BindingOperationInfo;

/**
 * Sets up the destination URI for a client invocation.
 */
public class URIParameterOutInterceptor extends AbstractPhaseInterceptor<Message> {
    
    public URIParameterOutInterceptor() {
        super(Phase.PREPARE_SEND);
        addBefore(MessageSenderInterceptor.class.getName());
    }

    public void handleMessage(Message message) throws Fault {
        Endpoint ep = message.getExchange().get(Endpoint.class);
        URIMapper mapper = (URIMapper)ep.getService().get(URIMapper.class.getName());
        BindingOperationInfo bop = message.getExchange().get(BindingOperationInfo.class);

        String address = ep.getEndpointInfo().getAddress();
        String location = mapper.getLocation(bop);

        StringBuilder uri = new StringBuilder();
        uri.append(address);

        boolean addressSlash = address.charAt(address.length() - 1) == '/';
        boolean locationSlash = location.charAt(0) == '/';
        if (!addressSlash && !locationSlash) {
            uri.append('/');
            uri.append(location);
        } else if (addressSlash && locationSlash) {
            uri.append(location.substring(1));
        } else {
            uri.append(location);
        }

        Document d = (Document) message.getContent(Node.class);
        String encodedUri = encodeIri(uri.toString(), d);

        message.put(Message.ENDPOINT_ADDRESS, encodedUri);
    }

    public static String encodeIri(String uri, Document doc) {
        StringBuilder builder = new StringBuilder();
        String locPath = uri;
        Element root = doc.getDocumentElement();

        int start = 0;
        char c;
        for (int idx1 = 0; idx1 < locPath.length(); idx1++) {
            c = locPath.charAt(idx1);
            if (c == '{') {
                if (locPath.charAt(idx1 + 1) == '{') {
                    idx1++;
                } else {
                    builder.append(locPath.substring(start, idx1));

                    int locEnd = locPath.indexOf('}', idx1);
                    String name = locPath.substring(idx1 + 1, locEnd);
                    idx1 = locEnd;
                    
                    Node node = root.getFirstChild();
                    while (node != null) {
                        if (node.getNodeType() == Node.ELEMENT_NODE && name.equals(node.getLocalName())) {
                            builder.append(DOMUtils.getRawContent(node));
                            break;
                        }   
                        node = node.getNextSibling();
                    }

                    start = locEnd + 1;
                }
            }
        }

        if (start == 0) {
            return uri;
        }

        return builder.toString();
    }

}
