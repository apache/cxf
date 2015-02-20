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

package org.apache.cxf.io;

import java.io.BufferedOutputStream;
import java.io.CharArrayReader;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;

import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.common.util.SystemPropertyAction;
import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;

public class CachedWriter extends Writer {
    private static final File DEFAULT_TEMP_DIR;
    private static int defaultThreshold;
    private static long defaultMaxSize;
    private static String defaultCipherTransformation;
    static {
        
        String s = SystemPropertyAction.getPropertyOrNull("org.apache.cxf.io.CachedOutputStream.OutputDirectory");
        if (s == null) {
            // lookup the deprecated property
            s = SystemPropertyAction.getPropertyOrNull("org.apache.cxf.io.CachedWriter.OutputDirectory");
        }
        if (s != null) {
            File f = new File(s);
            if (f.exists() && f.isDirectory()) {
                DEFAULT_TEMP_DIR = f;
            } else {
                DEFAULT_TEMP_DIR = null;
            }
        } else {
            DEFAULT_TEMP_DIR = null;
        }

        setDefaultThreshold(-1);
        setDefaultMaxSize(-1);
        setDefaultCipherTransformation(null);
    }

    protected boolean outputLocked;
    protected Writer currentStream;

    private long threshold = defaultThreshold;
    private long maxSize = defaultMaxSize;

    private long totalLength;

    private boolean inmem;

    private boolean tempFileFailed;
    private File tempFile;
    private File outputDir = DEFAULT_TEMP_DIR;
    private boolean allowDeleteOfFile = true;
    private String cipherTransformation = defaultCipherTransformation;
    private CipherPair ciphers;

    private List<CachedWriterCallback> callbacks;
    
    private List<Object> streamList = new ArrayList<Object>();

    
    static class LoadingCharArrayWriter extends CharArrayWriter {
        public LoadingCharArrayWriter() {
            super(1024);
        }
        public char[] rawCharArray() {
            return super.buf;
        }
    }
    

    public CachedWriter() {
        this(defaultThreshold);

        inmem = true;
    }

    public CachedWriter(long threshold) {
        this.threshold = threshold;
        currentStream = new LoadingCharArrayWriter();
        inmem = true;
        readBusProperties();
    }

    private void readBusProperties() {
        Bus b = BusFactory.getThreadDefaultBus(false);
        if (b != null) {
            String v = getBusProperty(b, "bus.io.CachedOutputStream.Threshold", null);
            if (v != null && threshold == defaultThreshold) {
                threshold = Integer.parseInt(v);
            }
            v = getBusProperty(b, "bus.io.CachedOutputStream.MaxSize", null);
            if (v != null) {
                maxSize = Integer.parseInt(v);
            }
            v = getBusProperty(b, "bus.io.CachedOutputStream.CipherTransformation", null);
            if (v != null) {
                cipherTransformation = v;
            }
        }
    }

    private static String getBusProperty(Bus b, String key, String dflt) {
        String v = (String)b.getProperty(key);
        return v != null ? v : dflt;
    }

    public void holdTempFile() {
        allowDeleteOfFile = false;
    }
    public void releaseTempFileHold() {
        allowDeleteOfFile = true;
    }
    
    public void registerCallback(CachedWriterCallback cb) {
        if (null == callbacks) {
            callbacks = new ArrayList<CachedWriterCallback>();
        }
        callbacks.add(cb);
    }
    
    public void deregisterCallback(CachedWriterCallback cb) {
        if (null != callbacks) {
            callbacks.remove(cb);
        }
    }

    public List<CachedWriterCallback> getCallbacks() {
        return callbacks == null ? null : Collections.unmodifiableList(callbacks);
    }

    /**
     * Perform any actions required on stream flush (freeze headers, reset
     * output stream ... etc.)
     */
    protected void doFlush() throws IOException {
        
    }

    public void flush() throws IOException {
        currentStream.flush();
        if (null != callbacks) {
            for (CachedWriterCallback cb : callbacks) {
                cb.onFlush(this);
            }
        }
        doFlush();
    }

    /**
     * Perform any actions required on stream closure (handle response etc.)
     */
    protected void doClose() throws IOException {
        
    }
    
    /**
     * Perform any actions required after stream closure (close the other related stream etc.)
     */
    protected void postClose() throws IOException {
        
    }

    /**
     * Locks the output stream to prevent additional writes, but maintains
     * a pointer to it so an InputStream can be obtained
     * @throws IOException
     */
    public void lockOutputStream() throws IOException {
        if (outputLocked) {
            return;
        }
        currentStream.flush();
        outputLocked = true;
        if (null != callbacks) {
            for (CachedWriterCallback cb : callbacks) {
                cb.onClose(this);
            }
        }
        doClose();
        streamList.remove(currentStream);
    }
    
    public void close() throws IOException {
        currentStream.flush();
        outputLocked = true;
        if (null != callbacks) {
            for (CachedWriterCallback cb : callbacks) {
                cb.onClose(this);
            }
        }
        doClose();
        currentStream.close();
        maybeDeleteTempFile(currentStream);
        postClose();
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof CachedWriter) {
            return currentStream.equals(((CachedWriter)obj).currentStream);
        }
        return currentStream.equals(obj);
    }

    /**
     * Replace the original stream with the new one, optionally copying the content of the old one
     * into the new one.
     * When with Attachment, needs to replace the xml writer stream with the stream used by
     * AttachmentSerializer or copy the cached output stream to the "real"
     * output stream, i.e. onto the wire.
     * 
     * @param out the new output stream
     * @param copyOldContent flag indicating if the old content should be copied
     * @throws IOException
     */
    public void resetOut(Writer out, boolean copyOldContent) throws IOException {
        if (out == null) {
            out = new LoadingCharArrayWriter();
        }

        if (currentStream instanceof CachedWriter) {
            CachedWriter ac = (CachedWriter) currentStream;
            Reader in = ac.getReader();
            IOUtils.copyAndCloseInput(in, out);
        } else {
            if (inmem) {
                if (currentStream instanceof LoadingCharArrayWriter) {
                    LoadingCharArrayWriter byteOut = (LoadingCharArrayWriter) currentStream;
                    if (copyOldContent && byteOut.size() > 0) {
                        byteOut.writeTo(out);
                    }
                } else {
                    throw new IOException("Unknown format of currentStream");
                }
            } else {
                // read the file
                currentStream.close();
                if (copyOldContent) {
                    InputStreamReader fin = createInputStreamReader(tempFile);
                    IOUtils.copyAndCloseInput(fin, out);
                }
                streamList.remove(currentStream);
                deleteTempFile();
                inmem = true;
            }
        }
        currentStream = out;
        outputLocked = false;
    }


    public long size() {
        return totalLength;
    }

    public char[] getChars() throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof LoadingCharArrayWriter) {
                return ((LoadingCharArrayWriter)currentStream).toCharArray();
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            try (Reader fin = createInputStreamReader(tempFile)) {
                CharArrayWriter out = new CharArrayWriter((int)tempFile.length());
                char bytes[] = new char[1024];
                int x = fin.read(bytes);
                while (x != -1) {
                    out.write(bytes, 0, x);
                    x = fin.read(bytes);
                }
                return out.toCharArray();
            }
        }
    }

    public void writeCacheTo(Writer out) throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof LoadingCharArrayWriter) {
                ((LoadingCharArrayWriter)currentStream).writeTo(out);
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            try (Reader fin = createInputStreamReader(tempFile)) {
                char bytes[] = new char[1024];
                int x = fin.read(bytes);
                while (x != -1) {
                    out.write(bytes, 0, x);
                    x = fin.read(bytes);
                }
            }
        }
    }
    
    public void writeCacheTo(StringBuilder out, long limit) throws IOException {
        flush();
        if (totalLength < limit
            || limit == -1) {
            writeCacheTo(out);
            return;
        }

        long count = 0;
        if (inmem) {
            if (currentStream instanceof LoadingCharArrayWriter) {
                LoadingCharArrayWriter s = (LoadingCharArrayWriter)currentStream;
                out.append(s.rawCharArray(), 0, (int)limit);
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            try (Reader fin = createInputStreamReader(tempFile)) {
                char bytes[] = new char[1024];
                long x = fin.read(bytes);
                while (x != -1) {
                    if ((count + x) > limit) {
                        x = limit - count;
                    }
                    out.append(bytes, 0, (int)x);
                    count += x;

                    if (count >= limit) {
                        x = -1;
                    } else {
                        x = fin.read(bytes);
                    }
                }
            }
        }
    }
    
    public void writeCacheTo(StringBuilder out) throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof LoadingCharArrayWriter) {
                LoadingCharArrayWriter lcaw = (LoadingCharArrayWriter)currentStream;
                out.append(lcaw.rawCharArray(), 0, lcaw.size());
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            try (Reader r = createInputStreamReader(tempFile)) {
                char chars[] = new char[1024];
                int x = r.read(chars);
                while (x != -1) {
                    out.append(chars, 0, x);
                    x = r.read(chars);
                }
            }
        }
    }


    /**
     * @return the underlying output stream
     */
    public Writer getOut() {
        return currentStream;
    }

    public int hashCode() {
        return currentStream.hashCode();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder().append("[")
            .append(CachedWriter.class.getName())
            .append(" Content: ");
        try {
            writeCacheTo(builder);
        } catch (IOException e) {
            //ignore
        }
        return builder.append("]").toString();
    }

    protected void onWrite() throws IOException {

    }

    private  void enforceLimits() throws IOException {
        if (maxSize > 0 && totalLength > maxSize) {
            throw new CacheSizeExceededException();
        }
        if (inmem && totalLength > threshold && currentStream instanceof LoadingCharArrayWriter) {
            createFileOutputStream();
        }       
    }

    
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (!outputLocked) {
            onWrite();
            this.totalLength += len;
            enforceLimits();
            currentStream.write(cbuf, off, len);
        }
    }

    private void createFileOutputStream() throws IOException {
        if (tempFileFailed) {
            return;
        }
        LoadingCharArrayWriter bout = (LoadingCharArrayWriter)currentStream;
        try {
            if (outputDir == null) {
                tempFile = FileUtils.createTempFile("cos", "tmp");
            } else {
                tempFile = FileUtils.createTempFile("cos", "tmp", outputDir, false);
            }
            currentStream = createOutputStreamWriter(tempFile);
            bout.writeTo(currentStream);
            inmem = false;
            streamList.add(currentStream);
        } catch (Exception ex) {
            //Could be IOException or SecurityException or other issues.
            //Don't care what, just keep it in memory.
            tempFileFailed = true;
            if (currentStream != bout) {
                currentStream.close();
            }
            deleteTempFile();
            inmem = true;
            currentStream = bout;
        }
    }

    public File getTempFile() {
        return tempFile != null && tempFile.exists() ? tempFile : null;
    }

    public Reader getReader() throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof LoadingCharArrayWriter) {
                LoadingCharArrayWriter lcaw = (LoadingCharArrayWriter)currentStream;
                return new CharArrayReader(lcaw.rawCharArray(), 0, lcaw.size());
            } else {
                return null;
            }
        } else {
            try {
                InputStream fileInputStream = new FileInputStream(tempFile) {
                    boolean closed;
                    public void close() throws IOException {
                        if (!closed) {
                            super.close();
                            maybeDeleteTempFile(this);
                        }
                        closed = true;
                    }
                };
                streamList.add(fileInputStream);
                if (cipherTransformation != null) {
                    fileInputStream = new CipherInputStream(fileInputStream, ciphers.getDecryptor()) {
                        boolean closed;
                        public void close() throws IOException {
                            if (!closed) {
                                super.close();
                                closed = true;
                            }
                        }
                    };
                }
                return new InputStreamReader(fileInputStream, "UTF-8");
            } catch (FileNotFoundException e) {
                throw new IOException("Cached file was deleted, " + e.toString());
            }
        }
    }
    
    private synchronized void deleteTempFile() {
        if (tempFile != null) {
            File file = tempFile;
            tempFile = null;
            FileUtils.delete(file);
        }
    }
    private void maybeDeleteTempFile(Object stream) {
        streamList.remove(stream);
        if (!inmem && tempFile != null && streamList.isEmpty() && allowDeleteOfFile) {
            if (currentStream != null) {
                try {
                    currentStream.close();
                    postClose();
                } catch (Exception e) {
                    //ignore
                }
            }
            deleteTempFile();
            currentStream = new LoadingCharArrayWriter();
            inmem = true;
        }
    }

    public void setOutputDir(File outputDir) throws IOException {
        this.outputDir = outputDir;
    }
    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }

    public void setMaxSize(long maxSize) {
        this.maxSize = maxSize;
    }

    public void setCipherTransformation(String cipherTransformation) {
        this.cipherTransformation = cipherTransformation;
    }
    
    public static void setDefaultMaxSize(long l) {
        if (l == -1) {
            String s = System.getProperty("org.apache.cxf.io.CachedOutputStream.MaxSize");
            if (s == null) {
                // lookup the deprecated property
                s = System.getProperty("org.apache.cxf.io.CachedWriter.MaxSize", "-1");
            }
            l = Long.parseLong(s);
        }
        defaultMaxSize = l;
    }
    public static void setDefaultThreshold(int i) {
        if (i == -1) {
            String s = SystemPropertyAction.getProperty("org.apache.cxf.io.CachedOutputStream.Threshold");
            if (s == null) {
                // lookup the deprecated property
                s = SystemPropertyAction.getProperty("org.apache.cxf.io.CachedWriter.Threshold", "-1");
            }
            i = Integer.parseInt(s);
            if (i <= 0) {
                i = 64 * 1024;
            }
        }
        defaultThreshold = i;
        
    }

    public static void setDefaultCipherTransformation(String n) {
        if (n == null) {
            n = SystemPropertyAction.getPropertyOrNull("org.apache.cxf.io.CachedOutputStream.CipherTransformation");
        }
        defaultCipherTransformation = n;
    }

    private OutputStreamWriter createOutputStreamWriter(File file) throws IOException {
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file));
        if (cipherTransformation != null) {
            try {
                if (ciphers == null) {
                    ciphers = new CipherPair(cipherTransformation);
                }
            } catch (GeneralSecurityException e) {
                throw new IOException(e.getMessage(), e);
            }
            out = new CipherOutputStream(out, ciphers.getEncryptor()) {
                boolean closed;
                public void close() throws IOException {
                    if (!closed) {
                        super.close();
                        closed = true;
                    }
                }
            };
        }
        return new OutputStreamWriter(out, "utf-8");
    }

    private InputStreamReader createInputStreamReader(File file) throws IOException {
        InputStream in = new FileInputStream(file);
        if (cipherTransformation != null) {
            in = new CipherInputStream(in, ciphers.getDecryptor()) {
                boolean closed;
                public void close() throws IOException {
                    if (!closed) {
                        super.close();
                        closed = true;
                    }
                }
            };
        }
        return new InputStreamReader(in, "utf-8");
    }

}
