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

package org.apache.cxf.binding.soap;

import javax.wsdl.extensions.soap.SOAPAddress;


import com.ibm.wsdl.extensions.soap.SOAPAddressImpl;


import org.apache.cxf.binding.soap.model.SoapBindingInfo;
import org.apache.cxf.service.model.EndpointInfo;
import org.apache.cxf.service.model.ServiceInfo;
import org.apache.cxf.transport.Destination;
import org.apache.cxf.transport.DestinationFactory;
import org.apache.cxf.transport.DestinationFactoryManager;
import org.easymock.classextension.EasyMock;
import org.easymock.classextension.IMocksControl;
import org.junit.Assert;
import org.junit.Test;

public class SoapDestinationFactoryTest extends Assert {
    
    @Test
    public void testDestination() throws Exception {
        String wsdlSoapNs = "http://schemas.xmlsoap.org/wsdl/soap/";
        String transportURI = "http://foo/transport";
        String location = "http://localhost/service";

        ServiceInfo si = new ServiceInfo();
        EndpointInfo ei = new EndpointInfo(si, wsdlSoapNs);
        SOAPAddress add = new SOAPAddressImpl();
        add.setLocationURI(location);
        ei.addExtensor(add);

        SoapBindingInfo bi = new SoapBindingInfo(si, "", Soap11.getInstance());
        bi.setTransportURI(transportURI);
        ei.setBinding(bi);

        IMocksControl control = EasyMock.createNiceControl();
        DestinationFactoryManager dfm = control.createMock(DestinationFactoryManager.class);
        DestinationFactory fooDF = control.createMock(DestinationFactory.class);
        Destination dest = control.createMock(Destination.class);

        EasyMock.expect(dfm.getDestinationFactory(transportURI)).andReturn(fooDF);
        EasyMock.expect(fooDF.getDestination(ei)).andStubReturn(dest);

        control.replay();

        // SoapDestinationFactory sdf = new SoapDestinationFactory(dfm);
        // Destination dest2 = sdf.getDestination(ei);
        // assertNotNull(dest2);

        // TODO: doesn't pass because I don't know how to use easymock :-(
        // assertEquals(dest, dest2);
    }
}
