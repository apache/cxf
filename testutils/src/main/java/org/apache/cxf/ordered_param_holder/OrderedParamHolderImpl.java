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

package org.apache.cxf.ordered_param_holder;

import javax.jws.WebService;
import javax.xml.ws.Holder;

@WebService(serviceName = "ordered_param_holder", 
        portName = "ordered_param_holderSOAP", 
        targetNamespace = "http://cxf.apache.org/ordered_param_holder/", 
        endpointInterface = "org.apache.cxf.ordered_param_holder.OrderedParamHolder",
        wsdlLocation = "testutils/ordered_param_holder.wsdl")
     
public class OrderedParamHolderImpl implements OrderedParamHolder {

    public void orderedParamHolder(Holder<ComplexStruct> part3, Holder<Integer> part2, Holder<String> part1) {
        // TODO Auto-generated method stub
        part2.value = new Integer(part2.value.intValue() + 1);
        part1.value = "return " + part1.value;
        part3.value.elem1 = "return " + part3.value.elem1;
        part3.value.elem2 = "return " + part3.value.elem2;
        part3.value.elem3++;
    }

}
