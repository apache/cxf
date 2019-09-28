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
package demo.jaxrs.server.simple;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.cxf.common.util.StringUtils;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

public class SparkStreamingOutput implements StreamingOutput {
    private BlockingQueue<String> responseQueue = new LinkedBlockingQueue<>();

    private JavaStreamingContext jssc;
    private volatile boolean sparkBatchCompleted;
    private volatile boolean outputWriteDone;
    private long startAt;
    public SparkStreamingOutput(JavaStreamingContext jssc) {
        this.jssc = jssc;
        this.startAt = System.currentTimeMillis();
    }

    @Override
    public void write(final OutputStream output) throws IOException, WebApplicationException {
        while (!sparkBatchCompleted || !outputWriteDone || !responseQueue.isEmpty()) {
            try {
                String responseEntry = responseQueue.poll(1, TimeUnit.MILLISECONDS);
                if (responseEntry != null) {
                    outputWriteDone = true;
                    output.write(StringUtils.toBytesUTF8(responseEntry));
                    output.flush();
                }
            } catch (InterruptedException e) {
                // continue;
            }
        }

        jssc.stop(false);
        jssc.close();
        System.out.println("Total processing time in millisecs: " + (System.currentTimeMillis() - startAt));
    }


    public void setSparkBatchCompleted() {
        this.sparkBatchCompleted = true;
    }

    public void addResponseEntry(String value) {
        responseQueue.add(value);
    }
}
