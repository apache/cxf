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

package org.apache.cxf.ws.rm;

import java.io.InputStream;
import java.util.logging.Logger;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.Phase;

/**
 * 
 */
public class RMCaptureInInterceptor extends AbstractRMInterceptor<Message> {
    private static final Logger LOG = LogUtils.getLogger(RMCaptureInInterceptor.class);
    
    public RMCaptureInInterceptor() {
        super(Phase.PRE_STREAM);
    }

    protected void handle(Message message) throws SequenceFault, RMException {
        LOG.entering(getClass().getName(), "handleMessage");
        // This message capturing mechanism will need to be changed at some point.
        // Until then, we keep this interceptor here and utilize the robust
        // option to avoid the unnecessary message capturing/caching.
        if (!MessageUtils.isTrue(message.getContextualProperty(Message.ROBUST_ONEWAY))) {
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                CachedOutputStream saved = new CachedOutputStream();
                try {
                    IOUtils.copy(is, saved);

                    saved.flush();
                    is.close();

                    message.setContent(InputStream.class, saved.getInputStream());
                    LOG.fine("Capturing the original RM message");
                    message.put(RMMessageConstants.SAVED_CONTENT, saved);
                } catch (Exception e) {
                    throw new Fault(e);
                }
            }
        }
    }
}
