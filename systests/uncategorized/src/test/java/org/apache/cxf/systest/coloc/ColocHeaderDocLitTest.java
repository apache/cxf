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

package org.apache.cxf.systest.coloc;

import org.junit.Test;

public class ColocHeaderDocLitTest extends AbstractHeaderServiceDocLitTest {
    static final String TRANSPORT_URI = "http://localhost:" + PORT + "/headers";

    protected Object getServiceImpl() {
        return new HttpServiceImpl();
    }

    protected String getTransportURI() {
        return TRANSPORT_URI;
    }

    @javax.jws.WebService(portName = "SoapPort9000", serviceName = "SOAPHeaderService",
            targetNamespace = "http://apache.org/headers/doc_lit",
            endpointInterface = "org.apache.headers.doc_lit.HeaderTester")
    class HttpServiceImpl extends BaseHeaderTesterDocLitImpl { }

    @Test
    public void dummy() {
    }

}
