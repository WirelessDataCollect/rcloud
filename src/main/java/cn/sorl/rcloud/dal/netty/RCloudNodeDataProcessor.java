package cn.sorl.rcloud.dal.netty;

import cn.sorl.rcloud.common.time.TimeUtils;
import cn.sorl.rcloud.common.util.PropertiesUtil;
import cn.sorl.rcloud.common.util.PropertyLabel;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliDatadbSegment;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.async.SingleResultCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import lombok.Getter;
import lombok.Setter;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 * 数据处理器，用于处理设备50发送到服务器的ByteBuf数据
 * @apiNote  数据格式：[0:PACKAGE_TIME_IO_LENGTH-1]：[时间:ADC数据长度:IO:ID:校验码]；
 * [PACKAGE_TIME_IO_LENGTH-1:PACKAGE_TIME_IO_LENGTH+MAX_TEST_NAME-1]：[时间:ADC数据长度:IO:ID:校验码]；
     * 00 00 10 10 48 10 01 00 10 00 00 00 01 01 00 48 74 65 73 74 31 5f 32 30 31 39 30 33 30 31 5f 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 02 00 05 01 03 00 06 01 02 00 05 01 03 00 05
 * @author  Charles Song
 * @date    2020-4-13
 * @version 0.0.1
 */
public class RCloudNodeDataProcessor  extends ChannelInboundHandlerAdapter {
    /**
     * 已连接设备
     */
    @Getter
    @Setter
    private static Map<String, RCloudNodeChannelAttr> nodeChMap = new ConcurrentHashMap<>();

    private SimpleMgd dataMgd;

    /**
     * 最大通道通道数目
     */
    private int MAX_CHANNEL_NUM;

    private static final Logger logger = LoggerFactory.getLogger(RCloudNodeDataProcessor.class);

    public RCloudNodeDataProcessor (SimpleMgd dataMgd) {
        try {
            this.dataMgd = dataMgd;
            PropertiesUtil propertiesUtil = new PropertiesUtil(PropertyLabel.PROPERTIES_FILE_ADDR);
            MAX_CHANNEL_NUM = Integer.parseInt(propertiesUtil.readValue(PropertyLabel.SERVER_NODE_CONNECT_NUM));
        } catch (Exception e) {
            logger.error("",e);
        }
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        NodeMsg nodeMsg = (NodeMsg) msg;
        logger.info("NodeMsg : " + nodeMsg.toString());
        short io1 = (short) (nodeMsg.getIo() & 0x01);
        short io2 = (short) (nodeMsg.getIo() & 0x02);
        Document doc = new Document(RuiliDatadbSegment.MONGODB_KEY_NODE_ID,nodeMsg.getId())//该包的节点
                .append(RuiliDatadbSegment.MONGODB_KEY_YYYYMMDD, nodeMsg.getYymd())//改包的年月日
                .append(RuiliDatadbSegment.MONGODB_KEY_HEADTIME,nodeMsg.getTimer())//改包的起始时间
                .append(RuiliDatadbSegment.MONGODB_KEY_ISO_DATE,nodeMsg.getIsotime())//iso时间，从testName中获取
                .append(RuiliDatadbSegment.MONGODB_KEY_INSERT_ISO_DATE, TimeUtils.getStrIsoSTime())//插入文档的时间
                .append(RuiliDatadbSegment.MONGODB_KEY_IO1,io1)//数字通道1
                .append(RuiliDatadbSegment.MONGODB_KEY_IO2,io2)//数字通道2
                .append(RuiliDatadbSegment.MONGODB_KEY_DATA_COUNT,nodeMsg.getCount())//数据个数
                .append(RuiliDatadbSegment.MONGODB_KEY_DATA_TYPE, nodeMsg.getType())//数据类型
                .append(RuiliDatadbSegment.MONGODB_KEY_TESTNAME,nodeMsg.getTstName())//测试名称
                .append(RuiliDatadbSegment.MONGODB_KEY_RAW_DATA,nodeMsg.getByteRawData() );//原始数据
        /*doc存入数据库*/
        //mongodb.insertOne已加锁
        try{
            dataMgd.insertOne(doc, new SingleResultCallback<Void>() {
                @Override
                public void onResult(final Void result, final Throwable t) {
                    ///用于指示DOC是否成功插入
			    	logger.info("Document inserted!");
                }});
        }catch(Exception e) {
            logger.error("", e);
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.info(String.format("Node %s connected!",ctx.channel().remoteAddress()));

        // 连接的节点过多
        if (nodeChMap.size() > this.MAX_CHANNEL_NUM) {
            channelInactive(ctx);
            return;
        }
        nodeChMap.put(ctx.channel().remoteAddress().toString(), new RCloudNodeChannelAttr(ctx));
        logger.info(String.format("Node Channel Num : %d", RCloudNodeDataProcessor.getNodeChMap().size()));
        ctx.fireChannelActive();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.delCh(ctx);
        logger.info(String.format("Node %s disconnected!",ctx.channel().remoteAddress().toString()));
        logger.info(String.format("Channel Num : %d", RCloudNodeDataProcessor.nodeChMap.size()));
        ctx.fireChannelInactive();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当出现异常
        logger.error("",cause);
        delCh(ctx);
    }

    /**
     * 删除通道
     * @param ctx ChannelHandlerContext
     */
    private synchronized void delCh(ChannelHandlerContext ctx){
        try {
            //从通道的map中删除掉这个通道
            RCloudNodeDataProcessor.getNodeChMap().remove(ctx.channel().remoteAddress().toString());
            //关闭该通道,并等待future完毕
            this.ctxCloseFuture(ctx);
        }catch(Exception e) {
            logger.error("",e);
        }

    }

    /**
     * 关闭ctx
     * @param ctx
     */
    public void ctxCloseFuture(ChannelHandlerContext ctx) {
        try {
            ChannelFuture future = ctx.close();
            logger.info("Got In Close : "+ctx.pipeline().channel().toString());
            future.addListener(new ChannelFutureListener(){
                @Override
                public void operationComplete(ChannelFuture f) {
                    if(!f.isSuccess()) {
                        logger.error("",f.cause());
                    }
                }
            });
        }catch(Exception e){
            logger.error("",e);
        }

    }

    /**
     * 设置MongoDB的操作对象
     * @param simpleMgd mongodb操作对象
     */
    public void setMongodb(SimpleMgd simpleMgd) {
        this.dataMgd = simpleMgd;
        //指向当前的数据集合
        this.dataMgd.resetCol(TimeUtils.getStrIsoMTime());
    }

    /**
     * 获取MongoDB的操作对象
     * @return {@link SimpleMgd} mongodb操作对象
     */
    public SimpleMgd getMongodb() {
        return this.dataMgd;
    }

}
