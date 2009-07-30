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

package org.apache.cxf.binding.soap.tcp.frames;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;

import org.apache.cxf.binding.soap.tcp.DataCodingUtils;

public class SoapTcpFrameContentDescription {
    private int contentId;
    private Map<Integer, String> parameters;
    
    public int getContentId() {
        return contentId;
    }
    
    public void setContentId(final int contentId) {
        this.contentId = contentId;
    }
    
    public Map<Integer, String> getParameters() {
        return parameters;
    }
    
    public void setParameters(final Map<Integer, String> parameters) {
        this.parameters = parameters;
    }
    
    public void write(final OutputStream output) throws IOException {
        DataCodingUtils.writeInts4(output, contentId, parameters.size());
        final Iterator<Integer> keys = parameters.keySet().iterator();
        while (keys.hasNext()) {
            final Integer paramId = keys.next();
            final String paramValue = parameters.get(paramId);
            final byte[] paramValueBytes = paramValue.getBytes("UTF-8");
            DataCodingUtils.writeInts4(output, paramId.intValue(), paramValueBytes.length);
            output.write(paramValueBytes);
        }
    }
}
