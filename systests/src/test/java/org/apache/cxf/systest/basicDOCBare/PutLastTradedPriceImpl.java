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

package org.apache.cxf.systest.basicDOCBare;
import java.math.BigDecimal;

import javax.xml.ws.Holder;

import org.apache.hello_world_doc_lit_bare.PutLastTradedPricePortType;
import org.apache.hello_world_doc_lit_bare.types.TradePriceData;


@javax.jws.WebService(serviceName = "SOAPService", 
                      portName = "SoapPort",
                      endpointInterface = "org.apache.hello_world_doc_lit_bare.PutLastTradedPricePortType",
                      targetNamespace = "http://apache.org/hello_world_doc_lit_bare",
                      wsdlLocation = "testutils/doc_lit_bare.wsdl")
public class PutLastTradedPriceImpl implements PutLastTradedPricePortType {
   
    public void sayHi(Holder<TradePriceData> inout) {
        inout.value.setTickerPrice(4.5f);
        inout.value.setTickerSymbol("APACHE");
    }   
    public void putLastTradedPrice(TradePriceData body) {
        System.out.println("-----TradePriceData TickerPrice : ----- " + body.getTickerPrice());
        System.out.println("-----TradePriceData TickerSymbol : ----- " + body.getTickerSymbol());

    }
    
    public String bareNoParam() {
        return "testResponse";
    }
    public String nillableParameter(BigDecimal theRequest) {
        return null;
    }
   

}
