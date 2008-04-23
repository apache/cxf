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
package org.apache.cxf.xmlbeans;


import org.w3c.dom.Node;

import org.apache.cxf.common.util.SOAPConstants;
import org.apache.cxf.wsdl.WSDLConstants;
import org.junit.Before;
import org.junit.Test;

public class WeatherService2Test extends AbstractXmlBeansTest {

    @Before
    public void setUp() throws Exception {
        super.setUp();

        createService(WeatherService2.class,
                      new WeatherService2(),
                      "WeatherService", null);
    }

    @Test
    public void testInvoke() throws Exception {
        Node response = invoke("WeatherService", "GetWeatherByZip.xml");

        addNamespace("w", "http://www.webservicex.net");
        assertValid("//w:GetWeatherByZipCodeResponse", response);

        response = invoke("WeatherService", "GetForecasts.xml");

        addNamespace("u", "http://www.webservicex.net");
        assertValid("//u:GetForecastsResponse", response);
        assertValid("//u:GetForecastsResponse/w:Latitude", response);
        assertValid("//u:GetForecastsResponse/w:Longitude", response);
    }

    @Test
    public void testWSDL() throws Exception {
        Node wsdl = getWSDLDocument("WeatherService");

        addNamespace("xsd", SOAPConstants.XSD);
        addNamespace("w", WSDLConstants.NS_WSDL11);

        assertValid("//w:message[@name='GetForecastsResponse']/w:part[@element='tns:GetForecastsResponse']",
                    wsdl);
    }
}
