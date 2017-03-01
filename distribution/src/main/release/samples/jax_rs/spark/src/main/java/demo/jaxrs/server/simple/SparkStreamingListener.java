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

import org.apache.spark.streaming.scheduler.StreamingListener;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchCompleted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerBatchSubmitted;
import org.apache.spark.streaming.scheduler.StreamingListenerOutputOperationCompleted;
import org.apache.spark.streaming.scheduler.StreamingListenerOutputOperationStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverError;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStarted;
import org.apache.spark.streaming.scheduler.StreamingListenerReceiverStopped;

public class SparkStreamingListener implements StreamingListener {
    private SparkStreamingOutput streamOutput;
    private boolean batchStarted;
    private long batchStartAt;

    public SparkStreamingListener(SparkStreamingOutput streamOutput) {
        this.streamOutput = streamOutput;
    }

    @Override
    public void onBatchCompleted(StreamingListenerBatchCompleted event) {
        System.out.println("Batch processing time in millisecs: " + (System.currentTimeMillis() - batchStartAt));

        streamOutput.setSparkBatchCompleted();
    }

    @Override
    public synchronized void onBatchStarted(StreamingListenerBatchStarted event) {
        batchStarted = true;
        batchStartAt = System.currentTimeMillis();
        notify();
    }

    @Override
    public void onBatchSubmitted(StreamingListenerBatchSubmitted event) {
    }

    @Override
    public void onOutputOperationCompleted(StreamingListenerOutputOperationCompleted event) {
    }

    @Override
    public void onOutputOperationStarted(StreamingListenerOutputOperationStarted event) {
    }

    @Override
    public void onReceiverError(StreamingListenerReceiverError event) {
    }

    @Override
    public void onReceiverStarted(StreamingListenerReceiverStarted event) {
    }

    @Override
    public void onReceiverStopped(StreamingListenerReceiverStopped arg0) {
    }

    public SparkStreamingOutput getStreamOut() {
        return streamOutput;
    }

    public synchronized void waitForBatchStarted() {
        while (!batchStarted) {
            try {
                this.wait();
            } catch (InterruptedException ex) {
                // continue
            }
        }

    }

}
