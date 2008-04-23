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

package org.apache.cxf.xmlbeans.basic;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;

import org.apache.cxf.databinding.xmlbeans.test.Address;


/**
 * 
 */
public class TestService {
    @WebMethod
    @WebResult(name = "return")
    public Address echoAddress(@WebParam(name = "ad")Address ad) {
        
        Address ret = Address.Factory.newInstance();
        ret.setAddressLine1(ad.getAddressLine1());
        ret.setAddressLine2(ad.getAddressLine2());
        ret.setCity(ad.getCity());
        ret.setCountry(ad.getCountry());
        ret.setZIPPostalCode(ad.getZIPPostalCode());
        ret.setStateProvinceRegion(ad.getStateProvinceRegion());
        return ret;
    }

}
