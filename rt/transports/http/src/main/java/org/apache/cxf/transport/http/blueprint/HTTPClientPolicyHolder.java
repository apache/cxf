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
package org.apache.cxf.transport.http.blueprint;

import java.io.ByteArrayInputStream;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;

import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;


public class HTTPClientPolicyHolder extends HTTPClientPolicy {
    
    private String parsedElement;
    private HTTPClientPolicy clientPolicy;

    private JAXBContext jaxbContext;
    private Set<Class<?>> jaxbClasses;

    public HTTPClientPolicyHolder() {
    }

    public void init() {
        try {

            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);

            Element element = docFactory.newDocumentBuilder()
                .parse(new ByteArrayInputStream(parsedElement.getBytes())).getDocumentElement();

            clientPolicy = (HTTPClientPolicy)HolderUtils.getJaxbObject(element, HTTPClientPolicy.class,
                                                           jaxbContext, jaxbClasses, getClass().getClassLoader());
            
            this.setAccept(clientPolicy.getAccept());
            this.setAcceptEncoding(clientPolicy.getAcceptEncoding());
            this.setAcceptLanguage(clientPolicy.getAcceptLanguage());
            this.setAllowChunking(clientPolicy.isAllowChunking());
            this.setAsyncExecuteTimeout(clientPolicy.getAsyncExecuteTimeout());
            this.setAsyncExecuteTimeoutRejection(clientPolicy.isAsyncExecuteTimeoutRejection());
            this.setAutoRedirect(clientPolicy.isAutoRedirect());
            this.setBrowserType(clientPolicy.getBrowserType());
            this.setCacheControl(clientPolicy.getCacheControl());
            this.setChunkingThreshold(clientPolicy.getChunkingThreshold());
            this.setConnection(clientPolicy.getConnection());
            this.setConnectionTimeout(clientPolicy.getConnectionTimeout());
            this.setContentType(clientPolicy.getContentType());
            this.setCookie(clientPolicy.getCookie());
            this.setDecoupledEndpoint(clientPolicy.getDecoupledEndpoint());
            this.setHost(clientPolicy.getHost());
            this.setMaxRetransmits(clientPolicy.getMaxRetransmits());
            this.setNonProxyHosts(clientPolicy.getNonProxyHosts());
            this.setProxyServer(clientPolicy.getProxyServer());
            // need to check if the property is set to avoid NPE
            if (clientPolicy.isSetProxyServerPort()) {
                this.setProxyServerPort(clientPolicy.getProxyServerPort());
            }
            this.setProxyServerType(clientPolicy.getProxyServerType());
            this.setReceiveTimeout(clientPolicy.getReceiveTimeout());
            this.setReferer(clientPolicy.getReferer());
            
        } catch (Exception e) {
            throw new RuntimeException("Could not process configuration.", e);
        }
    }

    public void destroy() {
        
    }

    public String getParsedElement() {
        return parsedElement;
    }

    public void setParsedElement(String parsedElement) {
        this.parsedElement = parsedElement;
    }

        
}
