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

package org.apache.cxf.systest.source;

import javax.jws.WebService;
import javax.xml.transform.dom.DOMSource;
import javax.xml.ws.Holder;

import org.apache.cxf.source.doc_lit_bare.PutLastTradedPricePortType;

/**
 *
 */
@WebService(targetNamespace = "http://cxf.apache.org/source/doc_lit_bare",
            portName = "SoapPort", serviceName = "SOAPService",
            endpointInterface = "org.apache.cxf.source.doc_lit_bare.PutLastTradedPricePortType"
)
public class PutLastTradePriceImpl implements PutLastTradedPricePortType {

    public DOMSource nillableParameter(DOMSource theRequest) {
        return null;
    }

    public void sayHi(Holder<DOMSource> body) {

    }

    public void putLastTradedPrice(DOMSource body) {

    }

    public DOMSource bareNoParam() {
        return null;
    }


}
