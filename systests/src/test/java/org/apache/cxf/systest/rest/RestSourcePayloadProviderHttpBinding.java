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

package org.apache.cxf.systest.rest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;
import javax.xml.ws.handler.MessageContext;

import org.w3c.dom.Document;

import org.apache.cxf.helpers.CastUtils;

@WebServiceProvider()
@ServiceMode(value = Service.Mode.PAYLOAD)
@BindingType("http://cxf.apache.org/bindings/xformat")
public class RestSourcePayloadProviderHttpBinding implements Provider<DOMSource> {

    @Resource
    protected WebServiceContext wsContext;

    public RestSourcePayloadProviderHttpBinding() {
    }

    public DOMSource invoke(DOMSource request) {
        MessageContext mc = wsContext.getMessageContext();
        String path = (String)mc.get(MessageContext.PATH_INFO);
        String query = (String)mc.get(MessageContext.QUERY_STRING);
        String httpMethod = (String)mc.get(MessageContext.HTTP_REQUEST_METHOD);
        
        Map<String, List<String>> responseHeader =
            CastUtils.cast((Map)mc.get(MessageContext.HTTP_RESPONSE_HEADERS));
        if (responseHeader == null) {
            responseHeader = new HashMap<String, List<String>>();
            mc.put(MessageContext.HTTP_RESPONSE_HEADERS, responseHeader);
        }

        List<String> values = new ArrayList<String>();
        values.add("hello1");
        values.add("hello2");
        responseHeader.put("REST", values);
//        System.out.println("--path--- " + path);
//        System.out.println("--query--- " + query);
//        System.out.println("--httpMethod--- " + httpMethod);
        
        if (httpMethod.equalsIgnoreCase("POST")) {
            // TBD: parse query info from DOMSource
            // System.out.println("--POST: getAllCustomers--- ");
            return getCustomer(null);
        } else if (httpMethod.equalsIgnoreCase("GET")) {
            if ("/XMLService/RestProviderPort/Customer".equals(path) && query == null) {
                // System.out.println("--GET:getAllCustomers--- ");
                return getAllCustomers();
            } else if ("/XMLService/RestProviderPort/Customer".equals(path) && query != null) {
                // System.out.println("--GET:getCustomer--- ");
                return getCustomer(query);
            }
        }

        return null;
    }

    private DOMSource getAllCustomers() {
        return createDOMSource("resources/CustomerAllResp.xml");
    }

    private DOMSource getCustomer(String customerID) {
        return createDOMSource("resources/CustomerJohnResp.xml");
    }

    private DOMSource createDOMSource(String fileName) {
        DocumentBuilderFactory factory;
        DocumentBuilder builder;
        Document document = null;
        DOMSource response = null;

        try {
            factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            builder = factory.newDocumentBuilder();
            InputStream greetMeResponse = getClass().getResourceAsStream(fileName);

            document = builder.parse(greetMeResponse);
            response = new DOMSource(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }       
}
