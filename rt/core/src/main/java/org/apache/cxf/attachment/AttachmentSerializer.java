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
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URLDecoder;

import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;


public class AttachmentSerializer {

    private static final String BODY_ATTACHMENT_ID = "root.message@cxf.apache.org";
    private Message message;
    private String bodyBoundary;
    private OutputStream out;
    private String encoding;
    private boolean xop = true;
    
    public AttachmentSerializer(Message messageParam) {
        message = messageParam;
    }

    /**
     * Serialize the beginning of the attachment which includes the MIME 
     * beginning and headers for the root message.
     */
    public void writeProlog() throws IOException {
        // Create boundary for body
        bodyBoundary = AttachmentUtil.getUniqueBoundaryValue();

        String bodyCt = (String) message.get(Message.CONTENT_TYPE);
        bodyCt = bodyCt.replaceAll("\"", "\\\"");
        
        // The bodyCt string is used enclosed within "", so if it contains the character ", it
        // should be adjusted, like in the following case:
        //   application/soap+xml; action="urn:ihe:iti:2007:RetrieveDocumentSet"
        // The attribute action is added in SoapActionOutInterceptor, when SOAP 1.2 is used
        // The string has to be changed in:
        //   application/soap+xml"; action="urn:ihe:iti:2007:RetrieveDocumentSet
        // so when it is enclosed within "", the result must be:
        //   "application/soap+xml"; action="urn:ihe:iti:2007:RetrieveDocumentSet"
        // instead of 
        //   "application/soap+xml; action="urn:ihe:iti:2007:RetrieveDocumentSet""
        // that is wrong because when used it produces:
        //   type="application/soap+xml; action="urn:ihe:iti:2007:RetrieveDocumentSet""
        if ((bodyCt.indexOf('"') != -1) && (bodyCt.indexOf(';') != -1)) {
            int pos = bodyCt.indexOf(';');
            StringBuffer st = new StringBuffer(bodyCt.substring(0 , pos));
            st.append("\"").append(bodyCt.substring(pos, bodyCt.length() - 1));
            bodyCt = st.toString();
        }        
        
        // Set transport mime type
        StringBuilder ct = new StringBuilder();
        ct.append("multipart/related; ");
        if (xop) {
            ct.append("type=\"application/xop+xml\"; ");
        } else {
            ct.append("type=\"").append(bodyCt).append("\"; ");
        }
        
        ct.append("boundary=\"")
            .append(bodyBoundary)
            .append("\"; ")
            .append("start=\"<")
            .append(BODY_ATTACHMENT_ID)
            .append(">\"; ")
            .append("start-info=\"")
            .append(bodyCt)
            .append("\"");
        
        message.put(Message.CONTENT_TYPE, ct.toString());

        
        // 2. write headers
        out = message.getContent(OutputStream.class);
        encoding = (String) message.get(Message.ENCODING);
        if (encoding == null) {
            encoding = "UTF-8";
        }
        StringWriter writer = new StringWriter();
        writer.write("\r\n");
        writer.write("--");
        writer.write(bodyBoundary);
        
        StringBuilder mimeBodyCt = new StringBuilder();
        mimeBodyCt.append("application/xop+xml; charset=")
            .append(encoding)
            .append("; type=\"")
            .append(bodyCt)
            .append("\";");
        
        writeHeaders(mimeBodyCt.toString(), BODY_ATTACHMENT_ID, writer);
        out.write(writer.getBuffer().toString().getBytes(encoding));
    }

    private void writeHeaders(String contentType, String attachmentId, Writer writer) throws IOException {
        writer.write("\r\n");
        writer.write("Content-Type: ");
        writer.write(contentType);
        writer.write("\r\n");

        writer.write("Content-Transfer-Encoding: binary\r\n");

        if (attachmentId != null) {
            writer.write("Content-ID: <");
            if (attachmentId.charAt(0) == '<'
                && attachmentId.charAt(attachmentId.length() - 1) == '>') {
                attachmentId = attachmentId.substring(1, attachmentId.length() - 1);
            }
            writer.write(URLDecoder.decode(attachmentId, "UTF-8"));
            writer.write(">\r\n");
        }
        writer.write("\r\n");
    }

    /**
     * Write the end of the body boundary and any attachments included.
     * @throws IOException
     */
    public void writeAttachments() throws IOException {
        if (message.getAttachments() != null) {
            for (Attachment a : message.getAttachments()) {
                StringWriter writer = new StringWriter();                
                writer.write("\r\n");
                writer.write("--");
                writer.write(bodyBoundary);
                writeHeaders(a.getDataHandler().getContentType(), a.getId(), writer);
                out.write(writer.getBuffer().toString().getBytes(encoding));
                
                a.getDataHandler().writeTo(out);
            }
        }
        StringWriter writer = new StringWriter();                
        writer.write("\r\n");
        writer.write("--");
        writer.write(bodyBoundary);
        writer.write("--");
        out.write(writer.getBuffer().toString().getBytes(encoding));
        out.flush();
    }

    public boolean isXop() {
        return xop;
    }

    public void setXop(boolean xop) {
        this.xop = xop;
    }
    
}
