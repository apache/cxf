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
package org.apache.cxf.systest.swa;

import java.io.IOException;
import java.io.InputStream;

import javax.activation.DataHandler;
import javax.jws.WebService;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.Holder;
import javax.xml.ws.WebServiceException;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.swa_nomime.SwAServiceInterface;
import org.apache.cxf.swa_nomime.types.DataStruct;
import org.apache.cxf.swa_nomime.types.OutputResponseAll;
import org.apache.cxf.swa_nomime.types.VoidRequest;


@WebService(endpointInterface = "org.apache.cxf.swa_nomime.SwAServiceInterface", 
            serviceName = "SwAService", 
            targetNamespace = "http://cxf.apache.org/swa-nomime", 
            portName = "SwAServiceHttpPort")
public class SwANoMimeServiceImpl implements SwAServiceInterface {

    public OutputResponseAll echoAllAttachmentTypes(VoidRequest request, Holder<String> attach1,
                                                    Holder<String> attach2, Holder<String> attach3,
                                                    Holder<byte[]> attach4, Holder<byte[]> attach5) {
        try {
            OutputResponseAll theResponse = new OutputResponseAll();
            theResponse.setResult("ok");
            theResponse.setReason("ok");
            if (attach1 == null || attach1.value == null) {
                System.err.println("attach1.value is null (unexpected)");
                theResponse.setReason("attach1.value is null (unexpected)");
                theResponse.setResult("not ok");
            }
            if (attach2 == null || attach2.value == null) {
                System.err.println("attach2.value is null (unexpected)");
                if (theResponse.getReason().equals("ok")) {
                    theResponse.setReason("attach2.value is null (unexpected)");
                } else {
                    theResponse.setReason(theResponse.getReason() + "\nattach2.value is null (unexpected)");
                }
                theResponse.setResult("not ok");
            }
            if (attach3 == null || attach3.value == null) {
                System.err.println("attach3.value is null (unexpected)");
                if (theResponse.getReason().equals("ok")) {
                    theResponse.setReason("attach3.value is null (unexpected)");
                } else {
                    theResponse.setReason(theResponse.getReason() + "\nattach3.value is null (unexpected)");
                }
                theResponse.setResult("not ok");
            }
            if (attach4 == null || attach4.value == null) {
                System.err.println("attach4.value is null (unexpected)");
                if (theResponse.getReason().equals("ok")) {
                    theResponse.setReason("attach4.value is null (unexpected)");
                } else {
                    theResponse.setReason(theResponse.getReason() + "\nattach4.value is null (unexpected)");
                }
                theResponse.setResult("not ok");
            }
            if (attach5 == null || attach5.value == null) {
                System.err.println("attach5.value is null (unexpected)");
                if (theResponse.getReason().equals("ok")) {
                    theResponse.setReason("attach5.value is null (unexpected)");
                } else {
                    theResponse.setReason(theResponse.getReason() + "\nattach5.value is null (unexpected)");
                }
                theResponse.setResult("not ok");
            }
            return theResponse;
        } catch (Exception e) {
            throw new WebServiceException(e.getMessage());
        }
    }

    public void echoData(Holder<String> text, Holder<byte[]> data) {
        try {
            data.value = ("test" + new String(data.value, 0, 6)).getBytes("UTF-8"); 
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public void echoDataRef(Holder<DataStruct> data) {
        try {
            InputStream bis = null;
            bis = data.value.getDataRef().getDataSource().getInputStream();
            byte b[] = new byte[6];
            bis.read(b, 0, 6);
            String string = IOUtils.newStringFromBytes(b);
            
            ByteArrayDataSource source = 
                new ByteArrayDataSource(("test" + string).getBytes(), "application/octet-stream");
            data.value.setDataRef(new DataHandler(source));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void echoDataWithHeader(Holder<String> text, Holder<byte[]> data, Holder<String> headerText) {
        try {
            data.value = ("test" + new String(data.value, 0, 6)).getBytes("UTF-8"); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
