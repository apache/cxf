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

package org.apache.cxf.attachment;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;

public class AttachmentBoundaryDeserializer {
    private static final Pattern INPUT_STREAM_BOUNDARY_PATTERN = Pattern.compile("^--(\\S*)$", Pattern.MULTILINE);
    private static final int PUSHBACK_AMOUNT = 2048;
    
    private final int maxHeaderLength;
    private final Message message;

    public AttachmentBoundaryDeserializer(Message message) {
        this.message = message;
        this.maxHeaderLength = MessageUtils.getContextualInteger(message, 
            AttachmentDeserializer.ATTACHMENT_MAX_HEADER_SIZE, AttachmentDeserializer.DEFAULT_MAX_HEADER_SIZE);
    }

    public Attachment read(InputStream body) throws IOException {
        final PushbackInputStream stream = new PushbackInputStream(body, PUSHBACK_AMOUNT);
        final String boundaryString = findBoundaryFromInputStream(stream);

        // If a boundary still wasn't found, throw an exception
        if (null == boundaryString) {
            throw new IOException("Couldn't determine the boundary from the message!");
        }

        final byte[] boundary = boundaryString.getBytes(StandardCharsets.UTF_8);
        if (!AttachmentDeserializerUtil.readTillFirstBoundary(stream, boundary)) {
            throw new IOException("Couldn't find MIME boundary: " + boundaryString);
        }

        Map<String, List<String>> ih = AttachmentDeserializerUtil.loadPartHeaders(stream, maxHeaderLength);
        String val = AttachmentUtil.getHeader(ih, "Content-Transfer-Encoding");

        MimeBodyPartInputStream mmps = new MimeBodyPartInputStream(stream, boundary, PUSHBACK_AMOUNT);
        InputStream ins = AttachmentUtil.decode(mmps, val);
        if (ins != mmps) {
            ih.remove("Content-Transfer-Encoding");
        }

        return AttachmentUtil.createAttachment(new DelegatingInputStream(ins, is -> stream.close()), ih, message);
    }

    private String findBoundaryFromInputStream(PushbackInputStream stream) throws IOException {
        //boundary should definitely be in the first 2K;
        byte[] buf = new byte[2048];
        int i = stream.read(buf);
        int len = i;
        while (i > 0 && len < buf.length) {
            i = stream.read(buf, len, buf.length - len);
            if (i > 0) {
                len += i;
            }
        }
        String msg = IOUtils.newStringFromBytes(buf, 0, len);
        stream.unread(buf, 0, len);

        // Use regex to get the boundary and return null if it's not found
        Matcher m = INPUT_STREAM_BOUNDARY_PATTERN.matcher(msg);
        return m.find() ? "--" + m.group(1) : null;
    }
}
