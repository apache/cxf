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
package org.apache.cxf.xmlbeans;

import org.w3c.dom.Node;
import org.junit.Before;
import org.junit.Test;



/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class IDRefServiceTest extends AbstractXmlBeansTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();
        createService(IDRefService.class, new IDRefService(), "IDRefService", null);
    }

    @Test
    public void testInvoke() throws Exception {
        Node response = getWSDLDocument("IDRefService");
        assertNotNull(response);

        /*
         * 
         *  SampleElementDocument doc =
         * SampleElementDocument.Factory.newInstance(); SampleElement
         * sampleElement = doc.addNewSampleElement(); SampleUserInformation
         * information = sampleElement.addNewSampleUserInformation();
         * addNamespace("t", "urn:TestService"); addNamespace("x",
         * "http://cxf.apache.org/xmlbeans");
         * assertValid("//t:mixedRequestResponse/x:response/x:form", response);
         */
    }
}
