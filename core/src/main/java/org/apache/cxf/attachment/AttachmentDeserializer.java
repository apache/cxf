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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.activation.DataSource;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.helpers.HttpHeaderHelper;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Attachment;
import org.apache.cxf.message.Message;

public class AttachmentDeserializer {
    public static final String ATTACHMENT_PART_HEADERS = AttachmentDeserializer.class.getName() + ".headers";
    public static final String ATTACHMENT_DIRECTORY = "attachment-directory";

    public static final String ATTACHMENT_MEMORY_THRESHOLD = "attachment-memory-threshold";

    public static final String ATTACHMENT_MAX_SIZE = "attachment-max-size";

    public static final int THRESHOLD = 1024 * 100; //100K (byte unit)

    private static final Pattern CONTENT_TYPE_BOUNDARY_PATTERN = Pattern.compile("boundary=\"?([^\";]*)");

    // TODO: Is there a better way to detect boundaries in the message content?
    // It seems constricting to assume the boundary will start with ----=_Part_
    private static final Pattern INPUT_STREAM_BOUNDARY_PATTERN =
            Pattern.compile("^--(\\S*)$", Pattern.MULTILINE);

    private boolean lazyLoading = true;

    private int pbAmount = 2048;
    private PushbackInputStream stream;
    private int createCount; 
    private int closedCount;
    private boolean closed;

    private byte boundary[];

    private String contentType;

    private LazyAttachmentCollection attachments;

    private Message message;

    private InputStream body;
    
    private Set<DelegatingInputStream> loaded = new HashSet<DelegatingInputStream>();
    private List<String> supportedTypes;

    public AttachmentDeserializer(Message message) {
        this(message, Collections.singletonList("multipart/related"));
    }

    public AttachmentDeserializer(Message message, List<String> supportedTypes) {
        this.message = message;
        this.supportedTypes = supportedTypes;
    }
    
    public void initializeAttachments() throws IOException {
        initializeRootMessage();

        attachments = new LazyAttachmentCollection(this);
        message.setAttachments(attachments);
    }

    protected void initializeRootMessage() throws IOException {
        contentType = (String) message.get(Message.CONTENT_TYPE);

        if (contentType == null) {
            throw new IllegalStateException("Content-Type can not be empty!");
        }

        if (message.getContent(InputStream.class) == null) {
            throw new IllegalStateException("An InputStream must be provided!");
        }

        if (AttachmentUtil.isTypeSupported(contentType.toLowerCase(), supportedTypes)) {
            String boundaryString = findBoundaryFromContentType(contentType);
            if (null == boundaryString) {                
                boundaryString = findBoundaryFromInputStream();
            }
            // If a boundary still wasn't found, throw an exception
            if (null == boundaryString) {
                throw new IOException("Couldn't determine the boundary from the message!");
            }
            boundary = boundaryString.getBytes("utf-8");

            stream = new PushbackInputStream(message.getContent(InputStream.class),
                                             pbAmount);
            if (!readTillFirstBoundary(stream, boundary)) {
                throw new IOException("Couldn't find MIME boundary: " + boundaryString);
            }

            Map<String, List<String>> ih = loadPartHeaders(stream);
            message.put(ATTACHMENT_PART_HEADERS, ih);
            String val = AttachmentUtil.getHeader(ih, "Content-Type", "; ");
            if (!StringUtils.isEmpty(val)) {
                String cs = HttpHeaderHelper.findCharset(val);
                if (!StringUtils.isEmpty(cs)) {
                    message.put(Message.ENCODING, HttpHeaderHelper.mapCharset(cs));
                }
            }

            body = new DelegatingInputStream(new MimeBodyPartInputStream(stream, boundary, pbAmount),
                                             this);
            createCount++;
            message.setContent(InputStream.class, body);
        }
    }

    private String findBoundaryFromContentType(String ct) throws IOException {
        // Use regex to get the boundary and return null if it's not found
        Matcher m = CONTENT_TYPE_BOUNDARY_PATTERN.matcher(ct);
        return m.find() ? "--" + m.group(1) : null;
    }

    private String findBoundaryFromInputStream() throws IOException {
        InputStream is = message.getContent(InputStream.class);
        //boundary should definitely be in the first 2K;
        PushbackInputStream in = new PushbackInputStream(is, 4096);
        byte buf[] = new byte[2048];
        int i = in.read(buf);
        String msg = IOUtils.newStringFromBytes(buf, 0, i);
        in.unread(buf, 0, i);
        
        // Reset the input stream since we'll need it again later
        message.setContent(InputStream.class, in);

        // Use regex to get the boundary and return null if it's not found
        Matcher m = INPUT_STREAM_BOUNDARY_PATTERN.matcher(msg);
        return m.find() ? "--" + m.group(1) : null;
    }
    
    public AttachmentImpl readNext() throws IOException {
        // Cache any mime parts that are currently being streamed
        cacheStreamedAttachments();
        if (closed) {
            return null;
        }

        int v = stream.read();
        if (v == -1) {
            return null;
        }
        stream.unread(v);

        Map<String, List<String>> headers = loadPartHeaders(stream);
        return (AttachmentImpl)createAttachment(headers);
    }

    private void cacheStreamedAttachments() throws IOException {
        if (body instanceof DelegatingInputStream
            && !((DelegatingInputStream) body).isClosed()) {

            cache((DelegatingInputStream) body);
        }

        List<Attachment> atts = new ArrayList<Attachment>(attachments.getLoadedAttachments());
        for (Attachment a : atts) {
            DataSource s = a.getDataHandler().getDataSource();
            if (s instanceof AttachmentDataSource) {
                AttachmentDataSource ads = (AttachmentDataSource)s;
                if (!ads.isCached()) {
                    ads.cache(message);
                }
            } else if (s.getInputStream() instanceof DelegatingInputStream) {
                cache((DelegatingInputStream) s.getInputStream());
            } else {
                //assume a normal stream that is already cached
            }
        }
    }

    private void cache(DelegatingInputStream input) throws IOException {
        if (loaded.contains(input)) {
            return;
        }
        loaded.add(input);
        CachedOutputStream out = null;
        InputStream origIn = input.getInputStream();
        try {
            out = new CachedOutputStream();
            AttachmentUtil.setStreamedAttachmentProperties(message, out);
            IOUtils.copy(input, out);
            input.setInputStream(out.getInputStream());
            origIn.close();
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }

    /**
     * Move the read pointer to the begining of the first part read till the end
     * of first boundary
     *
     * @param pushbackInStream
     * @param boundary
     * @throws MessagingException
     */
    private static boolean readTillFirstBoundary(PushbackInputStream pbs, byte[] bp) throws IOException {

        // work around a bug in PushBackInputStream where the buffer isn't
        // initialized
        // and available always returns 0.
        int value = pbs.read();
        pbs.unread(value);
        while (value != -1) {
            value = pbs.read();
            if ((byte) value == bp[0]) {
                int boundaryIndex = 0;
                while (value != -1 && (boundaryIndex < bp.length) && ((byte) value == bp[boundaryIndex])) {

                    value = pbs.read();
                    if (value == -1) {
                        throw new IOException("Unexpected End while searching for first Mime Boundary");
                    }
                    boundaryIndex++;
                }
                if (boundaryIndex == bp.length) {
                    // boundary found, read the newline
                    if (value == 13) {
                        pbs.read();
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Create an Attachment from the MIME stream. If there is a previous attachment
     * that is not read, cache that attachment.
     *
     * @return
     * @throws IOException
     */
    private Attachment createAttachment(Map<String, List<String>> headers) throws IOException {
        InputStream partStream = 
            new DelegatingInputStream(new MimeBodyPartInputStream(stream, boundary, pbAmount),
                                      this);
        createCount++;
        return AttachmentUtil.createAttachment(partStream, headers);
    }

    public boolean isLazyLoading() {
        return lazyLoading;
    }

    public void setLazyLoading(boolean lazyLoading) {
        this.lazyLoading = lazyLoading;
    }

    public void markClosed(DelegatingInputStream delegatingInputStream) throws IOException {
        closedCount++;
        if (closedCount == createCount && !attachments.hasNext(false)) {
            int x = stream.read();
            while (x != -1) {
                x = stream.read();
            }
            stream.close();
            closed = true;
        }
    }
    /**
     *  Check for more attachment.
     *
     * @return whether there is more attachment or not.  It will not deserialize the next attachment.
     * @throws IOException
     */
    public boolean hasNext() throws IOException {
        cacheStreamedAttachments();
        if (closed) {
            return false;
        }

        int v = stream.read();
        if (v == -1) {
            return false;
        }
        stream.unread(v);
        return true;
    }
    
    

    private Map<String, List<String>> loadPartHeaders(InputStream in) throws IOException {
        List<String> headerLines = new ArrayList<String>(10);
        StringBuilder buffer = new StringBuilder(128);
        String line;
        // loop until we hit the end or a null line
        while ((line = readLine(in)) != null) {
            // lines beginning with white space get special handling
            if (line.startsWith(" ") || line.startsWith("\t")) {
                // this gets handled using the logic defined by
                // the addHeaderLine method.  If this line is a continuation, but
                // there's nothing before it, just call addHeaderLine to add it
                // to the last header in the headers list
                if (buffer.length() == 0) {
                    addHeaderLine(headerLines, line);
                } else {
                    // preserve the line break and append the continuation
                    buffer.append("\r\n");
                    buffer.append(line);
                }
            } else {
                // if we have a line pending in the buffer, flush it
                if (buffer.length() > 0) {
                    addHeaderLine(headerLines, buffer.toString());
                    buffer.setLength(0);
                }
                // add this to the accumulator
                buffer.append(line);
            }
        }

        // if we have a line pending in the buffer, flush it
        if (buffer.length() > 0) {
            addHeaderLine(headerLines, buffer.toString());
        }
        Map<String, List<String>> heads = new TreeMap<String, List<String>>(String.CASE_INSENSITIVE_ORDER);
        for (String h: headerLines) {
            int separator = h.indexOf(':');
            String name = null;
            String value = "";
            if (separator == -1) {
                name = h.trim();
            } else {
                name = h.substring(0, separator);
                // step past the separator.  Now we need to remove any leading white space characters.
                separator++;

                while (separator < h.length()) {
                    char ch = h.charAt(separator);
                    if (ch != ' ' && ch != '\t' && ch != '\r' && ch != '\n') {
                        break;
                    }
                    separator++;
                }
                value = h.substring(separator);
            }
            List<String> v = heads.get(name);
            if (v == null) {
                v = new ArrayList<String>(1);
                heads.put(name, v);
            }
            v.add(value);
        }
        return heads;
    }

    private String readLine(InputStream in) throws IOException {
        StringBuilder buffer = new StringBuilder(128);

        int c;

        while ((c = in.read()) != -1) {
            // a linefeed is a terminator, always.
            if (c == '\n') {
                break;
            } else if (c == '\r') {
                //just ignore the CR.  The next character SHOULD be an NL.  If not, we're
                //just going to discard this
                continue;
            } else {
                // just add to the buffer
                buffer.append((char)c);
            }
        }

        // no characters found...this was either an eof or a null line.
        if (buffer.length() == 0) {
            return null;
        }

        return buffer.toString();
    }    
    private void addHeaderLine(List<String> headers, String line) {
        // null lines are a nop
        if (line.length() == 0) {
            return;
        }

        // we need to test the first character to see if this is a continuation whitespace
        char ch = line.charAt(0);

        // tabs and spaces are special.  This is a continuation of the last header in the list.
        if (ch == ' ' || ch == '\t') {
            int size = headers.size();
            // it's possible that we have a leading blank line.
            if (size > 0) {
                line = headers.remove(size - 1) + line;
                headers.add(line);
            }
        } else {
            // this just gets appended to the end, preserving the addition order.
            headers.add(line);
        }
    }

}
