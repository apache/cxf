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

package org.apache.cxf.systest.mtom_schema_validation;

import java.io.StringReader;

import javax.jws.soap.SOAPBinding;
import javax.xml.transform.sax.SAXSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceProvider;

import org.xml.sax.InputSource;

import org.apache.cxf.annotations.EndpointProperties;
import org.apache.cxf.annotations.EndpointProperty;

@BindingType(value = "http://schemas.xmlsoap.org/wsdl/soap/http")
@ServiceMode(value = javax.xml.ws.Service.Mode.PAYLOAD)
@WebServiceProvider(targetNamespace = "http://cxf.apache.org/", serviceName = "HelloWS", portName = "hello")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@EndpointProperties(value = {
                             @EndpointProperty(key = "schema-validation-enabled", value = "true"),
                             @EndpointProperty(key = "mtom-enabled", value = "true")
                    })
public class TestProvider implements Provider<SAXSource> {

    private String successRsp = "<ns2:helloResponse xmlns:ns2=\"http://cxf.apache.org/\">"
                                + "<return>Hello CXF</return>" + "</ns2:helloResponse>";

    public SAXSource invoke(SAXSource request) {
        return new SAXSource(new InputSource(new StringReader(successRsp)));
    }
}
