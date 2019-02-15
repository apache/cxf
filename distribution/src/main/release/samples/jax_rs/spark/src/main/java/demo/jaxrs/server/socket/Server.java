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

package demo.jaxrs.server.socket;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.lifecycle.SingletonResourceProvider;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.StorageLevels;
import org.apache.spark.api.java.function.VoidFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import demo.jaxrs.server.SparkUtils;
import demo.jaxrs.server.simple.SparkStreamingListener;
import demo.jaxrs.server.simple.SparkStreamingOutput;


public class Server {

    protected Server(String[] args) throws Exception {

        ServerSocket sparkServerSocket = new ServerSocket(9999);
        ServerSocket jaxrsResponseServerSocket = new ServerSocket(10000);
        Socket jaxrsResponseClientSocket = new Socket("localhost", 10000);


        SparkConf sparkConf = new SparkConf().setMaster("local[*]")
            .setAppName("JAX-RS Spark Socket Connect");
        JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));

        SparkStreamingOutput streamOut = new SparkStreamingOutput(jssc);
        SparkStreamingListener sparkListener = new SparkStreamingListener(streamOut);
        jssc.addStreamingListener(sparkListener);

        JavaDStream<String> receiverStream = jssc.socketTextStream(
            "localhost", 9999, StorageLevels.MEMORY_ONLY);

        JavaPairDStream<String, Integer> wordCounts = SparkUtils.createOutputDStream(receiverStream, true);
        PrintStream sparkResponseOutputStream = new PrintStream(jaxrsResponseClientSocket.getOutputStream(), true);
        wordCounts.foreachRDD(new SocketOutputFunction(sparkResponseOutputStream));

        jssc.start();

        Socket receiverClientSocket = sparkServerSocket.accept();
        PrintStream sparkOutputStream = new PrintStream(receiverClientSocket.getOutputStream(), true);
        BufferedReader sparkInputStream =
            new BufferedReader(new InputStreamReader(jaxrsResponseServerSocket.accept().getInputStream()));


        JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();

        sf.setResourceClasses(StreamingService.class);
        sf.setResourceProvider(StreamingService.class,
            new SingletonResourceProvider(new StreamingService(sparkInputStream,
                                                                     sparkOutputStream)));
        sf.setAddress("http://localhost:9000/spark");
        sf.create();

        jssc.awaitTermination();
        sparkServerSocket.close();
        jaxrsResponseServerSocket.close();
        jaxrsResponseClientSocket.close();

    }

    public static void main(String[] args) throws Exception {
        new Server(args);
        System.out.println("Server ready...");
        Thread.sleep(60 * 60 * 1000);
        System.out.println("Server exiting");
        System.exit(0);
    }

    private static class SocketOutputFunction implements VoidFunction<JavaPairRDD<String, Integer>> {
        private static final long serialVersionUID = 1L;
        private PrintStream streamOut;
        SocketOutputFunction(PrintStream streamOut) {
            this.streamOut = streamOut;
        }
        @Override
        public void call(JavaPairRDD<String, Integer> rdd) {
            if (!rdd.collectAsMap().isEmpty()) {
                String jobId = null;
                PrintStream printStream = null;
                for (Map.Entry<String, Integer> entry : rdd.collectAsMap().entrySet()) {
                    String value = entry.getKey() + " : " + entry.getValue();
                    if (jobId == null) {
                        int index = value.indexOf(':');
                        jobId = value.substring(0, index);
                        printStream = "oneway".equals(jobId) ? System.out : streamOut;

                    }
                    printStream.println(value);
                }
                printStream.println(jobId + ":" + "<batchEnd>");
            }
        }

    }

}
