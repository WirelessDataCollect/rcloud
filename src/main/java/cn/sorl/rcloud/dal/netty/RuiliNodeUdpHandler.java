package cn.sorl.rcloud.dal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * Node UDP 处理
 *
 */
public class RuiliNodeUdpHandler extends SimpleChannelInboundHandler<DatagramPacket> {
    private static final Logger logger = LoggerFactory.getLogger(RuiliNodeUdpHandler.class);
    private RuiliNodeUdpDataProcessor processor;
    private NettyPackCountTask countTask ;
    private ScheduledExecutorService testExecutor;
    public RuiliNodeUdpHandler(RuiliNodeUdpDataProcessor udpDataProcessor){
        try{
            this.processor = udpDataProcessor;
            //开启一个5s定时的端口数据包计数器
            testExecutor = Executors.newSingleThreadScheduledExecutor();
            countTask = new NettyPackCountTask("NodeUdp");
            testExecutor.scheduleAtFixedRate(countTask,5, 5, TimeUnit.SECONDS);
        }catch(Exception e){
            logger.error("",e);
        }
    }

    @Override
    protected synchronized void channelRead0(ChannelHandlerContext ctx, DatagramPacket msg){
        try{
            //        RunDeviceServer runDeviceServer = //获取某一个端口的数据信息
//                (RunDeviceServer) App.getApplicationContext().getBean("runDeviceServer");
//        runDeviceServer.incPacksNum();  //每次进入数据接受，都要更新包裹数目
            //如果数字超过了127,则会变成负数为了解决这个问题需要用getUnsignedByte
            ByteBuf temp = msg.content();
//        //是否实时转发给上位机
//        DeviceServerTools.send2Pc(temp);
            //解析数据
            processor.dataProcess(temp);
            countTask.incPacksNum();
        }catch (Exception e){
            logger.error("", e);
        }

    }
    /**
     * 当channel建立的时候回调（不面向连接，也无法返回数据回去），不同于TCP
     *
     * 在UDPbind的时候，服务器也不会进入channelActive。
     *
     * channelActive是自行创建的时候，进入的。
     */
    @Override
    public synchronized void channelActive(ChannelHandlerContext ctx) {
        logger.info("Device UDP channel " + ctx.channel().toString() + " created");
    }
    /**
     * 当Netty由于IO错误或者处理器在处理事件时抛出异常时调用
     */
    @Override
    public synchronized void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("",cause);
        ctx.close();
    }
}
