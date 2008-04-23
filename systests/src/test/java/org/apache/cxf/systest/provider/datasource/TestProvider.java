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

package org.apache.cxf.systest.provider.datasource;



import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Logger;

import javax.activation.DataSource;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.xml.ws.BindingType;
import javax.xml.ws.Provider;
import javax.xml.ws.Service;
import javax.xml.ws.ServiceMode;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.WebServiceProvider;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;




@WebServiceProvider(serviceName = "ModelProvider")
@BindingType(value = "http://cxf.apache.org/bindings/xformat")
@ServiceMode(value = Service.Mode.MESSAGE)
public class TestProvider extends AbstractProvider<DataSource> implements Provider<DataSource> {

    static final Logger LOG = LogUtils.getLogger(TestProvider.class);
    
    @javax.annotation.Resource
    public void setWebServiceContext(WebServiceContext wsc) {
        super.setWebServiceContext(wsc);
    }

    @Override
    public DataSource invoke(DataSource req) {
        return super.invoke(req); 
    }
    
    protected DataSource post(DataSource req) {
        String msg;
        try {
            LOG.info("content type: " + req.getContentType());

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            IOUtils.copy(req.getInputStream(), baos);
            LOG.info("body [" + new String(baos.toByteArray())  + "]");
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            msg = "<ok/>";

            MimeMultipart multipart = DataSourceProviderTest.readAttachmentParts(req.getContentType(), bais);
            LOG.info("found " + multipart.getCount() + " parts");
            return new ByteArrayDataSource(baos.toByteArray(), req.getContentType());
        } catch (Exception e) {
            e.printStackTrace();
            msg = "<fail/>";
        }
        return new ByteArrayDataSource(msg.getBytes(), "text/xml");
    }

    
    @Override
    protected DataSource get(DataSource req) {
        String msg = "<doc><response>Hello</response></doc>";
        return new ByteArrayDataSource(msg.getBytes(), "application/octet-stream");
    }
    
    
}
