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
package org.apache.cxf.systests.forked.dispatch;


import java.io.IOException;
import java.io.InputStream;

import org.apache.cxf.ext.logging.LoggingInInterceptor;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class HugeResponseInterceptor extends AbstractPhaseInterceptor<Message> {
    private ResponseInterceptorType type;

    public HugeResponseInterceptor(ResponseInterceptorType type) {
        super(Phase.RECEIVE);
        addAfter(LoggingInInterceptor.class.getName());
        this.type = type;
    }

    public void handleMessage(Message message) throws Fault {
        if (type.equals(ResponseInterceptorType.overflow)) {
            throw new StackOverflowError();
        } else if (type.equals(ResponseInterceptorType.ElementLevelThreshold)) {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                CachedOutputStream bos = new CachedOutputStream();
                try {
                    is = getClass().getClassLoader().getResourceAsStream(
                        "org/apache/cxf/systests/forked/dispatch/GreetMeDocLiteralRespBreakThreshold.xml");
                    IOUtils.copy(is, bos);
                    bos.flush();
                    is.close();
                    message.setContent(InputStream.class, bos.getInputStream());
                    bos.close();
                    message.setContent(InputStream.class, bos.getInputStream());
                } catch (IOException e) {
                    throw new Fault(e);
                }
            }
        } else if (type.equals(ResponseInterceptorType.ElementCountThreshold)) {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                CachedOutputStream bos = new CachedOutputStream();
                try {
                    is = getClass().getClassLoader().getResourceAsStream(
                        "org/apache/cxf/systests/forked/dispatch/"
                        + "GreetMeDocLiteralRespBreakElementCountThreshold.xml");
                    IOUtils.copy(is, bos);
                    bos.flush();
                    is.close();
                    message.setContent(InputStream.class, bos.getInputStream());
                    bos.close();
                    message.setContent(InputStream.class, bos.getInputStream());
                } catch (IOException e) {
                    throw new Fault(e);
                }
            }
        }
    }

}

