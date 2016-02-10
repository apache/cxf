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

import java.io.InputStream;
import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.apache.spark.SparkConf;
import org.apache.spark.SparkException;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;

import scala.Tuple2;


@Path("/")
public class StreamingService {
    private SparkConf sparkConf;
    public StreamingService(SparkConf sparkConf) {
        this.sparkConf = sparkConf;
    }
    
    @POST
    @Path("/stream")
    @Consumes("text/plain")
    @Produces("text/plain")
    public StreamingOutput getStream(InputStream is) {
        try {
            JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, Durations.seconds(1));
            JavaReceiverInputDStream<String> receiverStream = 
                jssc.receiverStream(new InputStreamReceiver(is));
            return new SparkStreamingOutput(jssc, 
                                            createOutputDStream(receiverStream));
        } catch (Exception ex) {
            if (ex instanceof SparkException) {
                // org.apache.spark.SparkException: Only one SparkContext may be running in this JVM (see SPARK-2243).
                // To ignore this error, set spark.driver.allowMultipleContexts = true
                throw new WebApplicationException(Response.status(503).header("Retry-After", "60").build());
            } else {
                throw new WebApplicationException(ex);
            }
        }
    }

    @SuppressWarnings("serial")
    private static JavaPairDStream<String, Integer> createOutputDStream(JavaReceiverInputDStream<String> receiverStream) {
        final JavaDStream<String> words = receiverStream.flatMap(
            new FlatMapFunction<String, String>() {
                @Override 
                public Iterable<String> call(String x) {
                    return Arrays.asList(x.split(" "));
                }
            });
        final JavaPairDStream<String, Integer> pairs = words.mapToPair(
            new PairFunction<String, String, Integer>() {
            
                @Override 
                public Tuple2<String, Integer> call(String s) {
                    return new Tuple2<String, Integer>(s, 1);
                }
            });
        return pairs.reduceByKey(
            new Function2<Integer, Integer, Integer>() {
             
                @Override 
                public Integer call(Integer i1, Integer i2) {
                    return i1 + i2;
                }
            });
    }
   
    //new MyReceiverInputDStream(jssc.ssc(), 
    //                           scala.reflect.ClassTag$.MODULE$.apply(String.class));
//    public static class MyReceiverInputDStream extends ReceiverInputDStream<String> {
//
//        public MyReceiverInputDStream(StreamingContext ssc_, ClassTag<String> evidence$1) {
//            super(ssc_, evidence$1);
//        }
//
//        @Override
//        public Receiver<String> getReceiver() {
//            return new InputStreamReceiver(is);
//        }
//        
//    }
}
