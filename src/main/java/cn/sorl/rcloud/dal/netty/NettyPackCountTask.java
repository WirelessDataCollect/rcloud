package cn.sorl.rcloud.dal.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * Netty网络端口数据包计数工具
 *
 * @author  neyzoter
 */
public class NettyPackCountTask implements Runnable{
    private Thread t;
    private int packsNum;
    private String threadName;
    private final static Logger logger = LoggerFactory.getLogger(NettyPackCountTask.class);
    public NettyPackCountTask(String threadName){
        this.threadName = threadName;
        packsNum = 0;
    }
    @Override
    public void run() {
        try {
            logger.info(String.format("%s's Periodic Packs : %d",this.threadName,this.packsNum));
            this.zeroPacksNum();
        }catch (Exception e) {
            logger.error(this.threadName,e);
        }
    }
    /**
     * 开始线程
     */
    public void start () {
        logger.info(String.format("Starting %s thread",this.threadName));
        if (t == null) {
            t = new Thread (this, this.threadName);
            t.start ();
        }
    }
    /**
     * 关闭NettyPackCountTask对象的线程
     */
    public void stop () {
        logger.info(String.format("Starting %s thread",this.threadName));
        t.interrupt();
    }
    /**
     * 计数器增加
     */
    public int incPacksNum(){
        this.packsNum += 1;
        return this.packsNum;
    }
    /**
     * 计数器获取
     */
    public int getPacksNum(){
        return this.packsNum;
    }
    /**
     * 计数器归零
     */
    public void zeroPacksNum(){
        this.packsNum = 0;
    }
}
