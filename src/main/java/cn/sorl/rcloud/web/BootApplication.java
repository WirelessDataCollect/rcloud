package cn.sorl.rcloud.web;

import cn.sorl.rcloud.biz.threads.RuiliNodeHandlerTask;
import cn.sorl.rcloud.biz.threads.RuiliPcHandlerTask;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ComponentScan({"cn.sorl.rcloud.biz.threads","cn.sorl.rcloud.biz.pool"})
@EnableScheduling
@EnableAsync
/**
 * 应用类
 * @author neyzoter song
 */
public class BootApplication {
    public static void main(String[] args){
        SpringApplication.run(BootApplication.class, args);
        RuiliNodeHandlerTask ruiliNodeHandlerTask = new RuiliNodeHandlerTask(5001, "Node-Thread");
        RuiliPcHandlerTask ruiliPcHandlerTask =new RuiliPcHandlerTask(8089, "Pc-Thread", "\t");
        ruiliNodeHandlerTask.start();ruiliPcHandlerTask.start();
    }
}