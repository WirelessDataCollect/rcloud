package cn.sorl.rcloud.biz.threads;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.netty.RuiliNodeUdpDataProcessor;
import cn.sorl.rcloud.dal.netty.RuiliNodeUdpHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 * 运行Node-UDP服务器，用于连接硬件设备
 *
 * @author  neyzoter song
 */
public class RuiliNodeHandlerTask implements Runnable{
    private int listenPort = 5001;
    private Thread t;
    private String threadName = "Device-Thread";
    private static final Logger logger = LoggerFactory.getLogger(RuiliNodeHandlerTask.class);

    public RuiliNodeHandlerTask(int port, String name) {
        this.setListenPort(port);
        this.setThreadName(name);
    }
    /**
     * 设置连接设备的端口。bean的set方法，bean会自动调用
     *
     * @param port
     */
    public void setListenPort(int port) {
        this.listenPort = port;
    }
    /**
     * 设置线程名称。bean的set方法，bean会自动调用
     *
     * @param name
     */
    public void setThreadName(String name) {
        this.threadName = name;
    }
    /**
     * 运行UDP连接设备。
     *
     * @param port 面向设备的端口
     */
    private void runUdp(int port){
        EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
        try {

            Bootstrap bootstrap = new Bootstrap();//引导启动
            bootstrap.group(eventLoopGroup)
                    .channel(NioDatagramChannel.class)
                    .option(ChannelOption.SO_BROADCAST, true)
                    .handler(new ChannelInitializer<DatagramChannel>() {
                        @Override
                        public void initChannel(DatagramChannel ch) throws Exception {
                            ch.pipeline().addLast(new RuiliNodeUdpHandler(new RuiliNodeUdpDataProcessor(BeanContext.context.getBean("dataMgd", SimpleMgd.class))));
                        }
                    });//业务处理类,其中包含一些回调函数
            ChannelFuture cf= bootstrap.bind(port).sync();
            cf.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("",e);
        } finally {
            eventLoopGroup.shutdownGracefully();//最后一定要释放掉所有资源,并关闭channle
        }
    }
    @Override
    public void run() {
        logger.info(String.format("Listen port for nodes: %d", listenPort));
        runUdp(listenPort);
    }
    public void start () {
        logger.info("Starting " +  this.threadName);
        if (t == null) {
            t = new Thread (this, this.threadName);
            t.start ();
        }
    }
    /**
     * 关闭RunDeviceServer对象的线程
     */
    public void stop () {
        logger.info("Stopping " +  threadName);
        t.interrupt();
    }
}
