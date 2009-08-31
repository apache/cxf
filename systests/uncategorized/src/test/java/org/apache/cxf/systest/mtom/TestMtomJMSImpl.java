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

package org.apache.cxf.systest.mtom;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.xml.ws.Holder;

import org.apache.cxf.mime.TestMtom;
import org.apache.cxf.mime.types.XopStringType;


@WebService(serviceName = "TestMtomJMSService", 
        portName = "TestMtomJMSPort", 
        targetNamespace = "http://cxf.apache.org/mime", 
        endpointInterface = "org.apache.cxf.mime.TestMtom",
        wsdlLocation = "testutils/mtom_xop.wsdl")
        
public class TestMtomJMSImpl implements TestMtom {

    public void testXop(Holder<String> name, Holder<DataHandler> attachinfo) {
        // TODO Auto-generated method stub
        name.value = "return detail + " + name.value;
    }

    public XopStringType testXopString(XopStringType data) {
        XopStringType d2 = new XopStringType();
        d2.setAttachinfo("This is the cereal shot from guns" + data.getAttachinfo());
        return d2;
    }


}
