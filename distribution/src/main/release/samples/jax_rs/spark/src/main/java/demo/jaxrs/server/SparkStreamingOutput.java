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

public class SparkStreamingOutput implements StreamingOutput {
    private JavaPairDStream<String, Integer> wordCounts;
    private JavaStreamingContext jssc;
    private boolean operationCompleted;
    public SparkStreamingOutput(JavaStreamingContext jssc, JavaPairDStream<String, Integer> wordCounts) {
        this.jssc = jssc;
        this.wordCounts = wordCounts;
    }

    @Override
    public void write(final OutputStream output) throws IOException, WebApplicationException {
        wordCounts.foreachRDD(new OutputFunction(output));
        jssc.start();
        waitForOperationCompleted();
        jssc.stop(false);
        jssc.close();
    }

    private synchronized void waitForOperationCompleted() {
        while (!operationCompleted) {
            try {
                wait();
            } catch (InterruptedException e) {
                return;
            }
        }
    }
    
    
    public synchronized void setOperationCompleted() {
        this.operationCompleted = true;
        notify();
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
        }
        
    }
    
}
