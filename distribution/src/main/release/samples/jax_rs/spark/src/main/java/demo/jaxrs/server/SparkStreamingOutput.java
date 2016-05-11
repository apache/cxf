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
package demo.jaxrs.server;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.StreamingOutput;

import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.scheduler.StreamingListener;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchCompleted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchSubmitted;
import org.apache.spark.streaming.scheduler.StreamingListenerOutputOperationCompleted;
import org.apache.spark.streaming.scheduler.StreamingListenerOutputOperationStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverError;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStopped;

public class SparkStreamingOutput implements StreamingOutput {
    private JavaPairDStream<String, Integer> wordCounts;
    private JavaStreamingContext jssc;
    private boolean sparkDone;
    private boolean batchCompleted;
    public SparkStreamingOutput(JavaStreamingContext jssc, JavaPairDStream<String, Integer> wordCounts) {
        this.jssc = jssc;
        this.wordCounts = wordCounts;
    }

    @Override
    public void write(final OutputStream output) throws IOException, WebApplicationException {
        wordCounts.foreachRDD(new OutputFunction(output));
        jssc.addStreamingListener(new SparkStreamingListener());
        jssc.start();
        awaitTermination();
        jssc.stop(false);
        jssc.close();
    }

    private synchronized void awaitTermination() {
        while (!sparkDone) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
    }
    private synchronized void releaseStreamingContext() {
        if (batchCompleted) {
            sparkDone = true;
            notify();
        }
    }
    
    private synchronized void setBatchCompleted() {
        batchCompleted = true;
    }
    
    
    // This dedicated class was introduced to validate that when Spark is running it does not
    // fail the processing due to OutputStream being one of the fields in the serializable class,
    private class OutputFunction implements VoidFunction<JavaPairRDD<String, Integer>> {
        private static final long serialVersionUID = 1L;
        private OutputStream os;
        OutputFunction(OutputStream os) {
            this.os = os;
        }
        @Override
        public void call(JavaPairRDD<String, Integer> rdd) {
            for (Map.Entry<String, Integer> entry : rdd.collectAsMap().entrySet()) {
                String value = entry.getKey() + " : " + entry.getValue() + "\r\n";
                try {
                    os.write(value.getBytes());
                    os.flush();
                } catch (IOException ex) {
                    throw new WebApplicationException(); 
                }
            }
            // Right now we assume by the time we call it the batch the whole InputStream has been
            // processed
            releaseStreamingContext();
        }
        
    }
    private class SparkStreamingListener implements StreamingListener {

        @Override
        public void onBatchCompleted(StreamingListenerBatchCompleted event) {
            // as soon as the batch is finished we let the streaming context go
            // but this may need to be revisited if a given InputStream happens to be processed in
            // multiple batches ?
            setBatchCompleted();
        }

        @Override
        public void onBatchStarted(StreamingListenerBatchStarted arg0) {
        }

        @Override
        public void onBatchSubmitted(StreamingListenerBatchSubmitted arg0) {
        }

        @Override
        public void onOutputOperationCompleted(StreamingListenerOutputOperationCompleted arg0) {
        }

        @Override
        public void onOutputOperationStarted(StreamingListenerOutputOperationStarted arg0) {
        }

        @Override
        public void onReceiverError(StreamingListenerReceiverError arg0) {
        }

        @Override
        public void onReceiverStarted(StreamingListenerReceiverStarted arg0) {
        }

        @Override
        public void onReceiverStopped(StreamingListenerReceiverStopped arg0) {
        }
        
    }
}
