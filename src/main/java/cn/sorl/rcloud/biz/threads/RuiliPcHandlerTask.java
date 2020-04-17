package cn.sorl.rcloud.biz.threads;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;

import cn.sorl.rcloud.dal.netty.RuiliPcTcpHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.traffic.ChannelTrafficShapingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * PC客户端连接处理
 *
 * @author  neyzoter song
 */
public class RuiliPcHandlerTask implements Runnable{
    private Thread t;
    private String threadName;
    private int listenPort;
    public Channel ch;
    //分割报文
    private String MsgSPL;
    private static final Logger logger = LoggerFactory.getLogger(RuiliPcHandlerTask.class);

    public RuiliPcHandlerTask(int port, String threadName, String spl){
        this.threadName = threadName;
        this.listenPort = port;
        this.MsgSPL = spl;
    }
    /**
     * 设置线程名称。bean的set方法，bean会自动调用
     *
     * @param port
     */
    public void setListenPort(int port) {
        this.listenPort = port;
        logger.info(String.format("Listen port for PC: %d", listenPort));
    }
    /**
     * 设置线程名称。bean的set方法，bean会自动调用。
     *
     * @param name
     */
    public void setThreadName(String name) {
        this.threadName = name;
    }

    /**
     * 启动TCP监听
     */
    private void runTcp(){
        // 用来接收进来的连接，这个函数可以设置多少个线程
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        // 用来处理已经被接收的连接
        EventLoopGroup workerGroup = new NioEventLoopGroup();

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
                            ch.pipeline().addLast(new DelimiterBasedFrameDecoder(64 * 1024, Unpooled.copiedBuffer(MsgSPL.getBytes())))//换行解码器
                                    .addLast(new RuiliPcTcpHandler(dataMgd, infoMgd, adminMgd, generalMgd));//如果需要继续添加与之链接的handler，则再次调用addLast即可
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
        logger.info(String.format("Listen port for pcs: %d", this.listenPort));
        runTcp();
    }
    /**
     * 开始RunPcServer对象的线程
     * @return none
     */
    public void start () {
        logger.info("Starting " +  this.threadName);
        if (t == null) {
            //运行(this)这个runnable接口下的对象
            t = new Thread (this, this.threadName);
            t.start ();
        }
    }
    /**
     * 关闭RunPcServer对象的线程(使用interrup关闭，thread.stop不安全)
     * @return none
     */
    public void stop () {
        logger.info("Stopping " +  threadName);
        //当在一个被阻塞的线程(调用sleep或者wait)上调用interrupt时，阻塞调用将会被InterruptedException异常中断
        t.interrupt();
    }
}
