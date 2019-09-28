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

package org.apache.cxf.systest.handlers;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.test.AbstractCXFSpringTest;

/**
 *
 */
public abstract class AbstractSpringConfiguredAutoRewriteSoapAddressTest extends AbstractCXFSpringTest {

    private Document retrieveWsdlDocument(String hostname, String port) throws Exception {
        URL wsdlUrlLocalhost = new URL("http://" + hostname + ":" + port + "/SpringEndpoint?wsdl");
        URLConnection urlConnection = wsdlUrlLocalhost.openConnection();
        try (InputStream input = urlConnection.getInputStream()) {
            return StaxUtils.read(input);
        }
    }

    protected List<String> findAllServiceUrlsFromWsdl(String hostname, String port) throws Exception {
        Document wsdlDocument = retrieveWsdlDocument(hostname, port);
        List<String> serviceUrls = new LinkedList<>();
        List<Element> serviceList = DOMUtils.findAllElementsByTagNameNS(wsdlDocument.getDocumentElement(),
                                                          "http://schemas.xmlsoap.org/wsdl/",
                                                          "service");
        for (Element serviceEl : serviceList) {
            List<Element> portList = DOMUtils.findAllElementsByTagNameNS(serviceEl,
                                                          "http://schemas.xmlsoap.org/wsdl/",
                                                          "port");
            for (Element portEl : portList) {
                List<Element> addressList = DOMUtils.findAllElementsByTagNameNS(portEl,
                                                          "http://schemas.xmlsoap.org/wsdl/soap/",
                                                          "address");
                for (Element addressEl : addressList) {
                    serviceUrls.add(addressEl.getAttribute("location"));
                }
            }
        }
        return serviceUrls;
    }

}
