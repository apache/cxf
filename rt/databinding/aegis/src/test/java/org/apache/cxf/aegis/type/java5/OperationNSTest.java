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
package org.apache.cxf.aegis.type.java5;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.jws.Oneway;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.xml.xpath.XPathConstants;

import org.w3c.dom.Document;

import org.apache.cxf.aegis.AbstractAegisTest;
import org.apache.cxf.aegis.databinding.AegisDatabinding;
import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.helpers.XPathUtils;
import org.apache.cxf.jaxws.JaxWsServerFactoryBean;
import org.junit.Before;
import org.junit.Test;


public class OperationNSTest extends AbstractAegisTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        JaxWsServerFactoryBean sf = new JaxWsServerFactoryBean();
        sf.setServiceClass(NotificationLogImpl.class);
        sf.setAddress("local://NotificationLogImpl");
        sf.getServiceFactory().setDataBinding(new AegisDatabinding());

        sf.create();
    }

    @Test
    public void testWSDL() throws Exception {
        Collection<Document> wsdls = getWSDLDocuments("NotificationService");

        addNamespace("xsd", SOAPConstants.XSD);
        //assertValid("//xsd:element[@name='Notify']", wsdl);
        assertTrue(isExist(wsdls, "//xsd:element[@name='Notify']", getNamespaces()));
    }

    private boolean isExist(Collection<Document> docs, String xpath, Map<String, String> ns) {
        XPathUtils xpather = new XPathUtils(ns);
        for (Document doc : docs) {
            if (xpather.isExist(xpath, doc, XPathConstants.NODE)) {
                return true;
            }
        }
        return false;
    }

    @WebService(name = "NotificationLog", targetNamespace = "http://www.sics.se/NotificationLog")
    public static interface NotificationLog {

        @WebMethod(operationName = "Notify", action = "")
        @Oneway
        void notify(@WebParam(name = "Notify",
                               targetNamespace = "http://docs.oasis-open.org/wsn/b-2")
                               Document notify);

        @WebMethod(operationName = "query", action = "")
        @WebResult(name = "queryResponseDocs",
                   targetNamespace = "http://www.sics.se/NotificationLog")
        List<Document> query(@WebParam(name = "xpath",
                                       targetNamespace = "http://www.sics.se/NotificationLog")
                             String xpath);

        @WebMethod(operationName = "Notify2", action = "")
        @Oneway
        void notify2(@WebParam(name = "Notify",
             targetNamespace = "http://docs.oasis-open.org/wsn/2004/"
                 + "06/wsn-WS-BaseNotification-1.2-draft-01.xsd")
             Document notify);
    }

    @WebService(endpointInterface = "org.apache.cxf.aegis.type.java5.OperationNSTest$NotificationLog",
                serviceName = "NotificationService")
    public static class NotificationLogImpl implements NotificationLog {

        public void notify(Document notify) {
        }

        public void notify2(Document notify) {
        }

        public List<Document> query(String xpath) {
            return null;
        }
    }
}
