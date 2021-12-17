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

package demo.restful.server;

import java.io.InputStream;

import javax.xml.transform.dom.DOMSource;

import org.w3c.dom.Document;
import org.apache.cxf.message.Message;
import org.apache.cxf.staxutils.StaxUtils;

import jakarta.annotation.Resource;
import jakarta.xml.ws.Provider;
import jakarta.xml.ws.Service;
import jakarta.xml.ws.ServiceMode;
import jakarta.xml.ws.WebServiceContext;
import jakarta.xml.ws.WebServiceProvider;
import jakarta.xml.ws.handler.MessageContext;

@WebServiceProvider()
@ServiceMode(value = Service.Mode.PAYLOAD)
public class RestSourcePayloadProvider implements Provider<DOMSource> {

    @Resource
    protected WebServiceContext wsContext;

    public RestSourcePayloadProvider() {
    }

    public DOMSource invoke(DOMSource request) {
        MessageContext mc = wsContext.getMessageContext();
        String path = (String)mc.get(Message.PATH_INFO);
        String query = (String)mc.get(Message.QUERY_STRING);
        String httpMethod = (String)mc.get(Message.HTTP_REQUEST_METHOD);

        System.out.println("--path--- " + path);
        System.out.println("--query--- " + query);
        System.out.println("--httpMethod--- " + httpMethod);

        if ("POST".equalsIgnoreCase(httpMethod)) {
            // TBD: parse query info from DOMSource
            System.out.println("---Invoking updateCustomer---");
            return updateCustomer(request);
        } else if ("GET".equalsIgnoreCase(httpMethod)) {
            if ("/customerservice/customer".equals(path) && query == null) {
                System.out.println("---Invoking getAllCustomers---");
                return getAllCustomers();
            } else if ("/customerservice/customer".equals(path) && query != null) {
                System.out.println("---Invoking getCustomer---");
                return getCustomer(query);
            }
        }

        return null;
    }

    private DOMSource getAllCustomers() {
        return createDOMSource("/CustomerAllResp.xml");
    }

    private DOMSource getCustomer(String customerID) {
        return createDOMSource("/CustomerJohnResp.xml");
    }

    private DOMSource updateCustomer(DOMSource request) {
        // TBD: returned update customer info
        return createDOMSource("/CustomerJohnResp.xml");
    }

    private DOMSource createDOMSource(String fileName) {
        DOMSource response = null;

        try (InputStream greetMeResponse = getClass().getResourceAsStream(fileName)) {
            Document document = StaxUtils.read(greetMeResponse);
            response = new DOMSource(document);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return response;
    }
}
