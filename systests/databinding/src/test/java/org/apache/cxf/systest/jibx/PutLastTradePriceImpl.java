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

package org.apache.cxf.systest.jibx;

import javax.jws.WebService;

import org.apache.cxf.jibx.doc_lit_bare.PutLastTradedPricePortType;
import org.apache.cxf.jibx.doclitbare.types.In;
import org.apache.cxf.jibx.doclitbare.types.InDecimal;
import org.apache.cxf.jibx.doclitbare.types.OutString;
import org.apache.cxf.jibx.doclitbare.types.StringRespType;

/**
 * 
 */
@WebService(targetNamespace = "http://cxf.apache.org/jibx/doc_lit_bare",
            portName = "SoapPort", serviceName = "SOAPService",
            endpointInterface = "org.apache.cxf.jibx.doc_lit_bare.PutLastTradedPricePortType"
)
public class PutLastTradePriceImpl implements PutLastTradedPricePortType {
    
    public StringRespType bareNoParam() {
        StringRespType st = new StringRespType();
        st.setStringRespType("Get the request!");
        return st;
    }

    public void sayHi(javax.xml.ws.Holder<org.apache.cxf.jibx.doclitbare.types.Inout> body) {
        body.value.setTickerSymbol("BAK");
    }
    

    public void putLastTradedPrice(In body) {
        // TODO Auto-generated method stub
        
    }

    public OutString nillableParameter(InDecimal theRequest) {
        OutString st = new OutString();
        st.setOutString("Get the request " + theRequest.getInDecimal().toString());
        return st;
    }
    

}
