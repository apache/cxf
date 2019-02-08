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
package org.apache.cxf.management.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.management.persistence.ExchangeData;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class PersistInInterceptor extends AbstractPhaseInterceptor<Message> {

    public PersistInInterceptor() {
        super(Phase.RECEIVE);
    }

    /**
     * Copied from LoggingInInterceptor
     *
     * @param soapMessage
     */
    private void getSoapRequest(Message soapMessage, ExchangeData exchange) {
        InputStream is = soapMessage.getContent(InputStream.class);
        if (is != null) {
            CachedOutputStream bos = new CachedOutputStream();
            try {
                IOUtils.copy(is, bos);

                bos.flush();
                is.close();

                soapMessage.setContent(InputStream.class, bos.getInputStream());

                StringBuilder builder = new StringBuilder();
                bos.writeCacheTo(builder, bos.size());

                bos.close();

                exchange.setRequest(builder.toString());
                exchange.setRequestSize((int)bos.size());

            } catch (IOException e) {
                throw new Fault(e);
            }
        }

    }

    public void handleMessage(Message message) throws Fault {

        ExchangeData soapExchange = new ExchangeData();
        soapExchange.setInDate(new Date());

        message.setContent(ExchangeData.class, soapExchange);

        getSoapRequest(message, soapExchange);

    }

}
