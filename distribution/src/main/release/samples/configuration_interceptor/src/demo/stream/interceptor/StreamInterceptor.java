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

package demo.stream.interceptor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.cxf.binding.soap.interceptor.SoapPreProtocolOutInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

public class StreamInterceptor extends AbstractPhaseInterceptor<Message> {

    public StreamInterceptor() {
        super(Phase.PRE_STREAM);
        addBefore(SoapPreProtocolOutInterceptor.class.getName());
    }
    
    public void handleMessage(Message message) {
        //TODO

        boolean isOutbound = false;
        isOutbound = message == message.getExchange().getOutMessage()
               || message == message.getExchange().getOutFaultMessage();

        if (isOutbound) {
            OutputStream os = message.getContent(OutputStream.class);
            CachedStream cs = new CachedStream();
            message.setContent(OutputStream.class, cs);
            
            message.getInterceptorChain().doIntercept(message);

            try {
                cs.flush();
                CachedOutputStream csnew = (CachedOutputStream) message
                    .getContent(OutputStream.class);
                GZIPOutputStream zipOutput = new GZIPOutputStream(os);
                CachedOutputStream.copyStream(csnew.getInputStream(), zipOutput, 1024);

                cs.close();
                zipOutput.close();
                os.flush();

                message.setContent(OutputStream.class, os);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        } else {
            try {
                InputStream is = message.getContent(InputStream.class);
                GZIPInputStream zipInput = new GZIPInputStream(is);
                message.setContent(InputStream.class, zipInput);
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }
    }

    public void handleFault(Message message) {
    }
    

    private class CachedStream extends CachedOutputStream {
        public CachedStream() {
            super();
        }
        
        protected void doFlush() throws IOException {
            currentStream.flush();
        }

        protected void doClose() throws IOException {
        }
        
        protected void onWrite() throws IOException {
        }
    }
}
