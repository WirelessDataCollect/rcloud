package cn.sorl.rcloud.biz.pool;

import cn.sorl.rcloud.biz.threads.RuiliNodeHandlerTask;
import cn.sorl.rcloud.biz.threads.RuiliPcHandlerTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AsyncPoolImpl implements AsyncPoolIf{
    private static final Logger logger = LoggerFactory.getLogger(AsyncPoolImpl.class);

    @Override
    @Async("asyncExecutor")
    public void executeAsync() {
        logger.info("Start Async Pool");
        RuiliNodeHandlerTask ruiliNodeHandlerTask = new RuiliNodeHandlerTask(5001, "Node-Thread");
        RuiliPcHandlerTask ruiliPcHandlerTask =new RuiliPcHandlerTask(8089, "Pc-Thread", "\t");
        ruiliNodeHandlerTask.start();ruiliPcHandlerTask.start();
        logger.info("Stop Async Pool");
    }
}
