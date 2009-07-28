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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.cxf.helpers.FileUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.helpers.LoadingByteArrayOutputStream;

public class CachedOutputStream extends OutputStream {
    private static final File DEFAULT_TEMP_DIR;
    private static final int DEFAULT_THRESHOLD;
    static {
        String s = System.getProperty("org.apache.cxf.io.CachedOutputStream.Threshold",
                                      "-1");
        int i = Integer.parseInt(s);
        if (i <= 0) {
            i = 64 * 1024;
        }
        DEFAULT_THRESHOLD = i;
        
        s = System.getProperty("org.apache.cxf.io.CachedOutputStream.OutputDirectory");
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
    }

    protected boolean outputLocked;
    protected OutputStream currentStream;

    private long threshold = DEFAULT_THRESHOLD;

    private int totalLength;

    private boolean inmem;

    private boolean tempFileFailed;
    private File tempFile;
    private File outputDir = DEFAULT_TEMP_DIR;
    private boolean allowDeleteOfFile = true;

    private List<CachedOutputStreamCallback> callbacks;
    
    private List<Object> streamList = new ArrayList<Object>();

    public CachedOutputStream(PipedInputStream stream) throws IOException {
        currentStream = new PipedOutputStream(stream);
        inmem = true;
    }

    public CachedOutputStream() {
        currentStream = new LoadingByteArrayOutputStream(2048);
        inmem = true;
    }

    public CachedOutputStream(long threshold) {
        this.threshold = threshold; 
        currentStream = new LoadingByteArrayOutputStream(2048);
        inmem = true;
    }

    public void holdTempFile() {
        allowDeleteOfFile = false;
    }
    public void releaseTempFileHold() {
        allowDeleteOfFile = true;
    }
    
    public void registerCallback(CachedOutputStreamCallback cb) {
        if (null == callbacks) {
            callbacks = new ArrayList<CachedOutputStreamCallback>();
        }
        callbacks.add(cb);
    }
    
    public void deregisterCallback(CachedOutputStreamCallback cb) {
        if (null != callbacks) {
            callbacks.remove(cb);
        }
    }

    public List<CachedOutputStreamCallback> getCallbacks() {
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
            for (CachedOutputStreamCallback cb : callbacks) {
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
            for (CachedOutputStreamCallback cb : callbacks) {
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
            for (CachedOutputStreamCallback cb : callbacks) {
                cb.onClose(this);
            }
        }
        doClose();
        currentStream.close();
        maybeDeleteTempFile(currentStream);
        postClose();
    }

    public boolean equals(Object obj) {
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
    public void resetOut(OutputStream out, boolean copyOldContent) throws IOException {
        if (out == null) {
            out = new ByteArrayOutputStream();
        }

        if (currentStream instanceof CachedOutputStream) {
            CachedOutputStream ac = (CachedOutputStream) currentStream;
            InputStream in = ac.getInputStream();
            IOUtils.copyAndCloseInput(in, out);
        } else {
            if (inmem) {
                if (currentStream instanceof ByteArrayOutputStream) {
                    ByteArrayOutputStream byteOut = (ByteArrayOutputStream) currentStream;
                    if (copyOldContent && byteOut.size() > 0) {
                        byteOut.writeTo(out);
                    }
                } else if (currentStream instanceof PipedOutputStream) {
                    PipedOutputStream pipeOut = (PipedOutputStream) currentStream;
                    IOUtils.copyAndCloseInput(new PipedInputStream(pipeOut), out);
                } else {
                    throw new IOException("Unknown format of currentStream");
                }
            } else {
                // read the file
                currentStream.close();
                FileInputStream fin = new FileInputStream(tempFile);
                if (copyOldContent) {
                    IOUtils.copyAndCloseInput(fin, out);
                }
                streamList.remove(currentStream);
                tempFile.delete();
                tempFile = null;
                inmem = true;
            }
        }
        currentStream = out;
        outputLocked = false;
    }

    public static void copyStream(InputStream in, OutputStream out, int bufferSize) throws IOException {
        IOUtils.copyAndCloseInput(in, out, bufferSize);
    }

    public int size() {
        return totalLength;
    }

    public byte[] getBytes() throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof ByteArrayOutputStream) {
                return ((ByteArrayOutputStream)currentStream).toByteArray();
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            FileInputStream fin = new FileInputStream(tempFile);
            return IOUtils.readBytesFromStream(fin);
        }
    }

    public void writeCacheTo(OutputStream out) throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof ByteArrayOutputStream) {
                ((ByteArrayOutputStream)currentStream).writeTo(out);
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            FileInputStream fin = new FileInputStream(tempFile);
            IOUtils.copyAndCloseInput(fin, out);
        }
    }
    public void writeCacheTo(StringBuilder out, int limit) throws IOException {
        flush();
        if (totalLength < limit
            || limit == -1) {
            writeCacheTo(out);
            return;
        }

        int count = 0;
        if (inmem) {
            if (currentStream instanceof ByteArrayOutputStream) {
                byte bytes[] = ((ByteArrayOutputStream)currentStream).toByteArray();
                out.append(IOUtils.newStringFromBytes(bytes, 0, limit));
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            FileInputStream fin = new FileInputStream(tempFile);
            byte bytes[] = new byte[1024];
            int x = fin.read(bytes);
            while (x != -1) {
                if ((count + x) > limit) {
                    x = limit - count;
                }
                out.append(IOUtils.newStringFromBytes(bytes, 0, x));
                count += x;

                if (count >= limit) {
                    x = -1;
                } else {
                    x = fin.read(bytes);
                }
            }
            fin.close();
        }
    }
    public void writeCacheTo(StringBuilder out) throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof ByteArrayOutputStream) {
                byte[] bytes = ((ByteArrayOutputStream)currentStream).toByteArray();
                out.append(IOUtils.newStringFromBytes(bytes));
            } else {
                throw new IOException("Unknown format of currentStream");
            }
        } else {
            // read the file
            FileInputStream fin = new FileInputStream(tempFile);
            byte bytes[] = new byte[1024];
            int x = fin.read(bytes);
            while (x != -1) {
                out.append(IOUtils.newStringFromBytes(bytes, 0, x));
                x = fin.read(bytes);
            }
            fin.close();
        }
    }


    /**
     * @return the underlying output stream
     */
    public OutputStream getOut() {
        return currentStream;
    }

    public int hashCode() {
        return currentStream.hashCode();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder().append("[")
            .append(CachedOutputStream.class.getName())
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

    public void write(byte[] b, int off, int len) throws IOException {
        if (!outputLocked) {
            onWrite();
            this.totalLength += len;
            if (inmem && totalLength > threshold && currentStream instanceof ByteArrayOutputStream) {
                createFileOutputStream();
            }
            currentStream.write(b, off, len);
        }
    }

    public void write(byte[] b) throws IOException {
        if (!outputLocked) {
            onWrite();
            this.totalLength += b.length;
            if (inmem && totalLength > threshold && currentStream instanceof ByteArrayOutputStream) {
                createFileOutputStream();
            }
            currentStream.write(b);
        }
    }

    public void write(int b) throws IOException {
        if (!outputLocked) {
            onWrite();
            this.totalLength++;
            if (inmem && totalLength > threshold && currentStream instanceof ByteArrayOutputStream) {
                createFileOutputStream();
            }
            currentStream.write(b);
        }
    }

    private void createFileOutputStream() throws IOException {
        if (tempFileFailed) {
            return;
        }
        ByteArrayOutputStream bout = (ByteArrayOutputStream)currentStream;
        try {
            if (outputDir == null) {
                tempFile = FileUtils.createTempFile("cos", "tmp");
            } else {
                tempFile = FileUtils.createTempFile("cos", "tmp", outputDir, false);
            }
            
            currentStream = new BufferedOutputStream(new FileOutputStream(tempFile));
            bout.writeTo(currentStream);
            inmem = false;
            streamList.add(currentStream);
        } catch (Exception ex) {
            //Could be IOException or SecurityException or other issues.
            //Don't care what, just keep it in memory.
            tempFileFailed = true;
            tempFile = null;
            inmem = true;
            currentStream = bout;
        }
    }

    public File getTempFile() {
        return tempFile != null && tempFile.exists() ? tempFile : null;
    }

    public InputStream getInputStream() throws IOException {
        flush();
        if (inmem) {
            if (currentStream instanceof LoadingByteArrayOutputStream) {
                return ((LoadingByteArrayOutputStream) currentStream).createInputStream();
            } else if (currentStream instanceof ByteArrayOutputStream) {
                return new ByteArrayInputStream(((ByteArrayOutputStream) currentStream).toByteArray());
            } else if (currentStream instanceof PipedOutputStream) {
                return new PipedInputStream((PipedOutputStream) currentStream);
            } else {
                return null;
            }
        } else {
            try {
                FileInputStream fileInputStream = new FileInputStream(tempFile) {
                    public void close() throws IOException {
                        super.close();
                        maybeDeleteTempFile(this);
                    }
                };
                streamList.add(fileInputStream);
                return fileInputStream;
            } catch (FileNotFoundException e) {
                throw new IOException("Cached file was deleted, " + e.toString());
            }
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
            tempFile.delete();
            tempFile = null;
            currentStream = new LoadingByteArrayOutputStream(1024);
            inmem = true;
        }
    }

    public void setOutputDir(File outputDir) throws IOException {
        this.outputDir = outputDir;
    }
    public void setThreshold(long threshold) {
        this.threshold = threshold;
    }
}
