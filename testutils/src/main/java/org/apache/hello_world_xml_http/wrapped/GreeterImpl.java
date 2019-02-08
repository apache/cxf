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

package org.apache.hello_world_xml_http.wrapped;

import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.hello_world_xml_http.wrapped.types.FaultDetail;

@javax.jws.WebService(serviceName = "XMLService",
                      portName = "XMLPort",
                      endpointInterface = "org.apache.hello_world_xml_http.wrapped.Greeter",
                      targetNamespace = "http://apache.org/hello_world_xml_http/wrapped",
                      wsdlLocation = "testutils/hello_world_xml_wrapped.wsdl")

@javax.xml.ws.BindingType(value = "http://cxf.apache.org/bindings/xformat")

public class GreeterImpl implements Greeter {

    private static final Logger LOG = LogUtils.getL7dLogger(GreeterImpl.class);


    public String greetMe(String me) {
        LOG.info("Executing operation greetMe\n" + me);
        return "Hello " + me;
    }

    public void greetMeOneWay(String me) {
        LOG.info("Executing operation greetMeOneWay\n");
        LOG.info("Hello there " + me);
    }

    public String sayHi() {
        LOG.info("Executing operation sayHi\n");
        return "Bonjour";
    }

    public void pingMe() throws PingMeFault {
        LOG.info("Executing operation pingMe\n");
        FaultDetail faultDetail = new FaultDetail();
        faultDetail.setMajor((short)2);
        faultDetail.setMinor((short)1);
        throw new PingMeFault("PingMeFault raised by server", faultDetail);
    }
}
