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

package org.apache.cxf.systest.xmlbeans;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.cxf.xmlbeans.docLitBare.types.TradePriceData;
import org.apache.cxf.xmlbeans.doc_lit_bare.PutLastTradedPricePortType;
import org.apache.xmlbeans.XmlDecimal;
import org.apache.xmlbeans.XmlString;

/**
 * 
 */
@WebService(targetNamespace = "http://cxf.apache.org/xmlbeans/doc_lit_bare",
            portName = "SoapPort", serviceName = "SOAPService",
            endpointInterface = "org.apache.cxf.xmlbeans.doc_lit_bare.PutLastTradedPricePortType"
)
public class PutLastTradePriceImpl implements PutLastTradedPricePortType {

    /** {@inheritDoc}*/
    public XmlString bareNoParam() {
        return null;
    }

    /** {@inheritDoc}*/
    public XmlString nillableParameter(XmlDecimal theRequest) {
        return null;
    }

    /** {@inheritDoc}*/
    public void putLastTradedPrice(TradePriceData body) {

    }

    /** {@inheritDoc}*/
    public void sayHi(Holder<TradePriceData> body) {

    }

}
