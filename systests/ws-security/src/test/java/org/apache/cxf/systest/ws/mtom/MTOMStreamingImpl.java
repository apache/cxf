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
package org.apache.cxf.systest.ws.mtom;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.activation.DataHandler;
import jakarta.activation.DataSource;
import jakarta.jws.WebService;
import org.example.contract.doubleit.DoubleItFault;
import org.example.contract.doubleit.DoubleItStreamingMtomPortType;

@WebService(targetNamespace = "http://www.example.org/contract/DoubleIt",
            serviceName = "DoubleItService",
            endpointInterface = "org.example.contract.doubleit.DoubleItStreamingMtomPortType")
public class MTOMStreamingImpl implements DoubleItStreamingMtomPortType {

    static final long CHUNK_DELAY_MS = 500L;
    static final int CHUNK_COUNT = 5;
    static final int CHUNK_SIZE_BYTES = 1024;

    static final AtomicReference<Instant> STREAMING_FINISHED = new AtomicReference<>();

    public static void resetStreamingFinished() {
        STREAMING_FINISHED.set(null);
    }

    public static Instant getStreamingFinished() {
        return STREAMING_FINISHED.get();
    }

    @Override
    public DataHandler doubleIt5(int numberToDouble) throws DoubleItFault {
        if (numberToDouble == 0) {
            throw new DoubleItFault("0 can't be doubled!");
        }
        try {
            PipedInputStream pipedIn = new PipedInputStream(CHUNK_SIZE_BYTES * 2);
            PipedOutputStream pipedOut = new PipedOutputStream(pipedIn);
            Thread writerThread = new Thread(() -> {
                byte[] chunk = new byte[CHUNK_SIZE_BYTES];
                Arrays.fill(chunk, (byte) 'A');
                try {
                    for (int i = 0; i < CHUNK_COUNT; i++) {
                        pipedOut.write(chunk);
                        pipedOut.flush();
                        Thread.sleep(CHUNK_DELAY_MS);
                    }
                    pipedOut.close();
                    STREAMING_FINISHED.set(Instant.now());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (IOException e) {
                    // pipe may be closed early by consumer; no further action needed
                }
            });
            writerThread.setDaemon(true);
            writerThread.start();
            DataSource dataSource = new DataSource() {
                @Override
                public InputStream getInputStream() {
                    return pipedIn;
                }
                @Override
                public OutputStream getOutputStream() {
                    throw new UnsupportedOperationException();
                }
                @Override
                public String getContentType() {
                    return "application/octet-stream";
                }
                @Override
                public String getName() {
                    return "streaming-data";
                }
            };
            return new DataHandler(dataSource);
        } catch (IOException e) {
            throw new DoubleItFault("Error creating streaming response: " + e.getMessage());
        }
    }
}
