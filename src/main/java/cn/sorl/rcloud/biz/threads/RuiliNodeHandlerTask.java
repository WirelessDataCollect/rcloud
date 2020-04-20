package cn.sorl.rcloud.biz.threads;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.common.util.PropertiesUtil;
import cn.sorl.rcloud.common.util.PropertyLabel;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.netty.*;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 *
 * 运行Node-UDP服务器，用于连接硬件设备
 *
 * @author  neyzoter song
 */
public class RuiliNodeHandlerTask implements Runnable{
    private int listenPort = 5001;
    private String proto;
    private Thread t;
    private String threadName = "Device-Thread";
    public static final String PROTOCAL_UDP = "UDP";
    public static final String PROTOCAL_TCP = "TCP";
    private static final Logger logger = LoggerFactory.getLogger(RuiliNodeHandlerTask.class);

    public RuiliNodeHandlerTask(int port, String name) {
        this.setListenPort(port);
        this.setThreadName(name);
        PropertiesUtil propertiesUtil = new PropertiesUtil(PropertyLabel.PROPERTIES_FILE_ADDR);
        String protocal = propertiesUtil.readValue(PropertyLabel.SERVER_NODE_PORT_PROTOCAL).toUpperCase();
        if (protocal.equals(RuiliNodeHandlerTask.PROTOCAL_UDP)) {
            proto = RuiliNodeHandlerTask.PROTOCAL_UDP;
        } else {
            proto = RuiliNodeHandlerTask.PROTOCAL_TCP;
        }
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
    /**
     * 运行TCP连接设备。
     *
     * @param port 面向设备的端口
     */
    private void runTcp (int port) {

        // 启动计数
        ScheduledExecutorService countService = new ScheduledThreadPoolExecutor(1,
                new BasicThreadFactory.Builder().namingPattern("Tcp-Stream-Counter-%d").daemon(true).build());
        countService.scheduleAtFixedRate(RCloudStreamCountTask.getInstance(), 5, 5, TimeUnit.SECONDS);
        // 用来接收进来的连接，这个函数可以设置多少个线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 用来处理已经被接收的连接
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        Channel ch;
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    // 这里告诉Channel如何接收新的连接
                    .channel(NioServerSocketChannel.class)
                    .childHandler( new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {//起初ch的pipeline会分配一个RunPcServer的出/入站处理器（初始化完成后删除）
                            SimpleMgd dataMgd = BeanContext.context.getBean("dataMgd",SimpleMgd.class);
                            SimpleMgd infoMgd = BeanContext.context.getBean("infoMgd",SimpleMgd.class);
                            SimpleMgd adminMgd = BeanContext.context.getBean("adminMgd",SimpleMgd.class);
                            SimpleMgd generalMgd = BeanContext.context.getBean("generalMgd",SimpleMgd.class);
                            // 自定义处理类
                            ch.pipeline().addLast(new NodeMsgDecoder())//解码器
                                    .addLast(new RCloudNodeDataProcessor(BeanContext.context.getBean("dataMgd", SimpleMgd.class)));//如果需要继续添加与之链接的handler，则再次调用addLast即可
                        }//完成初始化后，删除RunPcServer出/入站处理器
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            // 绑定端口，开始接收进来的连接
            //在bind后，创建一个ServerChannel，并且该ServerChannel管理了多个子Channel
            ChannelFuture cf = b.bind(listenPort).sync();
            // 等待服务器socket关闭
            ch = cf.channel();
            ch.closeFuture().sync();

        } catch (Exception e) {//线程会将中断interrupt作为一个终止请求
            logger.info("",e);
        } finally {
            workerGroup.shutdownGracefully().awaitUninterruptibly();
            bossGroup.shutdownGracefully().awaitUninterruptibly();
        }
    }

    @Override
    public void run() {
        logger.info(String.format("Listen port for nodes: %d ; Protocal : %s", listenPort, proto));
        if (proto.equals(RuiliNodeHandlerTask.PROTOCAL_UDP)) {
            runUdp(listenPort);
        } else if (proto.equals(RuiliNodeHandlerTask.PROTOCAL_TCP)){
            runTcp(listenPort);
        }

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
