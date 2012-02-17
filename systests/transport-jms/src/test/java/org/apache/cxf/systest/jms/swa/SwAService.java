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

package org.apache.cxf.systest.jms.swa;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

/**
 * 
 */

@WebService(targetNamespace = "http://cxf.apache.org/swa", name = "SwAServiceInterface")
//@javax.xml.bind.annotation.XmlSeeAlso({ org.apache.cxf.swa.types.ObjectFactory.class })
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface SwAService {

    @WebMethod
    void echoData(
        @WebParam(partName = "text", mode = WebParam.Mode.INOUT, name = "text",
            targetNamespace = "http://cxf.apache.org/swa/types")
        javax.xml.ws.Holder<java.lang.String> text,
        @WebParam(partName = "data", mode = WebParam.Mode.INOUT, name = "data", targetNamespace = "")
        javax.xml.ws.Holder<javax.activation.DataHandler> data
    );
}
