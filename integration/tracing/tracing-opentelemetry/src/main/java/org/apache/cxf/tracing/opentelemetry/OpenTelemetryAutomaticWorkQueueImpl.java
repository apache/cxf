package org.apache.cxf.tracing.opentelemetry;

import org.apache.cxf.tracing.opentelemetry.internal.CurrentContextThreadPoolExecutor;
import org.apache.cxf.workqueue.AutomaticWorkQueueImpl;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OpenTelemetryAutomaticWorkQueueImpl extends AutomaticWorkQueueImpl {

    @Override
    protected ThreadPoolExecutor createThreadPoolExecutor(
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            ThreadFactory threadFactory,
            AutomaticWorkQueueImpl.WatchDog watchDog
    ) {
        return new CurrentContextThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory) {

            @Override
            protected void terminated() {
                ThreadFactory f = this.getThreadFactory();
                if (f instanceof AWQThreadFactory awqThreadFactory) {
                    awqThreadFactory.shutdown();
                }
                if (watchDog != null) {
                    watchDog.shutdown();
                }
            }
        };
    }
}
