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

package org.apache.hello_world_soap_http;

import java.math.BigDecimal;

import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.hello_world_doc_lit_bare.PutLastTradedPricePortType;
import org.apache.hello_world_doc_lit_bare.types.TradePriceData;

@WebService
public class DocLitBareImpl implements PutLastTradedPricePortType {
    int sayHiCount;
    int putLastTradedPriceCount;
    int bareNoParamCallCount;
    
    public void sayHi(Holder<TradePriceData> inout) {
        ++sayHiCount;
        inout.value.setTickerPrice(4.5f);
        inout.value.setTickerSymbol("APACHE");
    }
    
    public void putLastTradedPrice(TradePriceData body) {
        ++putLastTradedPriceCount;
    }
    
    public int getSayHiInvocationCount() {
        return sayHiCount; 
    }
    
    public int getPutLastTradedPriceCount() {
        return putLastTradedPriceCount; 
    }
    
    public String bareNoParam() {
        bareNoParamCallCount++;
        return "testSuccess";
    }
    
    public int getBareNoParamCount() {
        return bareNoParamCallCount;
    }

    public String nillableParameter(BigDecimal theRequest) {
        return null;
    }
}
