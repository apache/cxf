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

import javax.jws.WebMethod;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import net.webservicex.GetWeatherByZipCodeDocument;
import net.webservicex.GetWeatherByZipCodeResponseDocument;
import net.webservicex.WeatherForecasts;

@WebService(targetNamespace = "http://www.webservicex.net")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public class WeatherService2 {
    
    @WebMethod(operationName = "GetWeatherByZipCode")
    public GetWeatherByZipCodeResponseDocument.GetWeatherByZipCodeResponse 
    getWeatherByZipCode(GetWeatherByZipCodeDocument.GetWeatherByZipCode body) {
        
        GetWeatherByZipCodeResponseDocument.GetWeatherByZipCodeResponse res 
            = GetWeatherByZipCodeResponseDocument.GetWeatherByZipCodeResponse.Factory
            .newInstance();

        WeatherForecasts weather = res.addNewGetWeatherByZipCodeResult();

        weather.setLatitude(1);
        weather.setLongitude(1);
        weather.setPlaceName("Grand Rapids, MI");

        return res;
    }
    @WebMethod(operationName = "GetForecasts")
    public WeatherForecasts getForecasts() {
        WeatherForecasts weather = WeatherForecasts.Factory.newInstance();

        weather.setLatitude(1);
        weather.setLongitude(1);
        weather.setPlaceName("Grand Rapids, MI");

        return weather;
    }
}
