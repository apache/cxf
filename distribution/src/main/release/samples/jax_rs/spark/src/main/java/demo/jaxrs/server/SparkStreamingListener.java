package demo.jaxrs.server;

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
    private SparkStreamingOutput sparkStreamingOutput;

    public SparkStreamingListener(SparkStreamingOutput sparkStreamingOutput) {
        this.sparkStreamingOutput = sparkStreamingOutput;
    }

    @Override
    public void onBatchCompleted(StreamingListenerBatchCompleted event) {
    }

    @Override
    public void onBatchStarted(StreamingListenerBatchStarted event) {
    }

    @Override
    public void onBatchSubmitted(StreamingListenerBatchSubmitted event) {
    }

    @Override
    public void onOutputOperationCompleted(StreamingListenerOutputOperationCompleted event) {
        sparkStreamingOutput.setOperationCompleted();
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
    
}
