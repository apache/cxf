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
package org.apache.cxf.xmlbeans.rpc;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import javax.xml.namespace.QName;

import org.w3c.dom.Node;

import net.webservicex.WeatherData;

import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.wsdl.WSDLConstants;
import org.apache.cxf.xmlbeans.AbstractXmlBeansTest;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:dan@envoisolutions.com">Dan Diephouse</a>
 */
public class WeatherServiceRPCLitTest extends AbstractXmlBeansTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        //xsf.setStyle(SoapConstants.STYLE_RPC);
        createService(RPCWeatherService.class, new RPCWeatherService(),
                      "WeatherService", new QName("http://www.webservicex.net", "WeatherService"));
    }

    @Test
    public void testInvoke() throws Exception {
        Node response = invoke("WeatherService", "SetWeatherData.xml");

        addNamespace("w", "http://www.webservicex.net");
        assertValid("//w:setWeatherDataResponse", response);

        response = invoke("WeatherService", "GetWeatherData.xml");
        assertValid("//w:getWeatherDataResponse/return", response);
        assertValid("//w:getWeatherDataResponse/return/w:MaxTemperatureC[text()='1']", response);
        assertValid("//w:getWeatherDataResponse/return/w:MaxTemperatureF[text()='1']", response);

    }

    @Test
    public void testWSDL() throws Exception {
        Node wsdl = getWSDLDocument("WeatherService");

        addNamespace("w", WSDLConstants.NS_WSDL11);
        addNamespace("xsd", SOAPConstants.XSD);

        assertValid("//w:message[@name='getWeatherDataResponse']/w:part[@type='tns:WeatherData']", wsdl);
    }

    @SOAPBinding(style = SOAPBinding.Style.RPC)
    @WebService(targetNamespace = "http://www.webservicex.net")
    public static class RPCWeatherService {
        @WebMethod
        public WeatherData getWeatherData() {
            WeatherData data = WeatherData.Factory.newInstance();
            data.setMaxTemperatureC("1");
            data.setMaxTemperatureF("1");

            return data;
        }

        @WebMethod
        public void setWeatherData(@WebParam(name = "data") WeatherData data) {

        }
    }
}
