package cn.sorl.rcloud.dal.netty;

import cn.sorl.rcloud.common.security.SimpleSaltMd5;
import cn.sorl.rcloud.common.time.TimeUtils;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliAdmindbSegment;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliDatadbSegment;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliInfodbSegment;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.FindIterable;
import com.mongodb.client.result.DeleteResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import org.bson.Document;
import org.bson.types.Binary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端处理类
 */
public class RuiliPcTcpHandler extends ChannelInboundHandlerAdapter {

    //ch_map 存储PC连接的通道<PC[num],channel>
    private static Map<String, RuiliPcChannelAttr> pcChMap = new ConcurrentHashMap<>();
    //配置文件数据库
    private SimpleMgd infoMgd;
    //数据存储数据库
    private SimpleMgd dataMgd;
    //用户信息数据库
    private SimpleMgd adminMgd;
    //用户信息数据库
    private SimpleMgd generalMgd;
    //限制连接
    private final static int MAX_CHANNEL_NUM = 50;

    private static final Logger logger = LoggerFactory.getLogger(RuiliPcTcpHandler.class);

    /**
     * 构造类
     *
     * @param data_mgd data mongodb
     * @param info_mgd info mongodb
     * @param admin_mgd admin mongodb
     * @param general_mgd general mongodb
     */
    public RuiliPcTcpHandler(SimpleMgd data_mgd, SimpleMgd info_mgd, SimpleMgd admin_mgd, SimpleMgd general_mgd){
        this.infoMgd = info_mgd;
        this.dataMgd = data_mgd;
        this.adminMgd = admin_mgd;
        this.generalMgd = general_mgd;

    }
    /**
     * 转发设备信息至PC端上位机
     * @param temp
     */
    public static void send2Pc(ByteBuf temp) {   //这里需要是静态的，非静态依赖对象
        //NEED TEST
        String testName = RuiliNodeUdpDataProcessor.getFrameHeadTestName(temp);
        synchronized(RuiliPcTcpHandler.getPcChMap()) {
            for(Iterator<Map.Entry<String,RuiliPcChannelAttr>> item = RuiliPcTcpHandler.getPcChMap().entrySet().iterator(); item.hasNext();) {
                Map.Entry<String,RuiliPcChannelAttr> entry = item.next();
                //判断是否为实时获取数据的状态,且和测试名称对应
                if((entry.getValue().getStatus()==RuiliPcChannelAttr.DATA_GET_STA) && ((entry.getValue().getTestName().equals(testName)) || (entry.getValue().getTestName().equals("all")))) {
                    //发送数据
                    if(entry.getValue().getContext().channel().isWritable()){
                        entry.getValue().getContext().writeAndFlush(Unpooled.wrappedBuffer(temp));
                    }
                }
            }
        }
    }
    /**
     * 获取保存同服务器连接的PC的通道
     * @return {@link Map}
     */
    public static synchronized Map<String,RuiliPcChannelAttr> getPcChMap(){
        return RuiliPcTcpHandler.pcChMap;
    }
    /**
     * 返回连接服务器的PC个数
     * @return int
     */
    public synchronized int getPcChNum(){
        return pcChMap.size();
    }
    /**
     * 设置配置信息数据库
     * @param db 存储用户信息的db
     */
    public void setinfoMgd(SimpleMgd db) {
        this.infoMgd = db;
    }
    /**
     * 获取配置信息数据库
     */
    public SimpleMgd getinfoMgd() {
        return this.infoMgd;
    }
    /**
     * 删除某个channel所有信息
     *
     * @param ctx context
     *
     */
    private synchronized void delCh(ChannelHandlerContext ctx){
        try {
            //从通道的map中删除掉这个通道
            RuiliPcTcpHandler.getPcChMap().remove(ctx.channel().remoteAddress().toString());
            //关闭该通道,并等待future完毕
            this.ctxCloseFuture(ctx);
        }catch(Exception e) {
            logger.error("",e);
        }

    }
    /**
     * 登录管理员Md5加密
     *
     * 验证通过则将该上位机放到新人驱，如果没有通过则直接断开。
     * @apiNote
     * |---------;---------|
     *    user     keyHash
     * @param ctx context
     * @param info infomation
     */
    private void loginMd5(ChannelHandlerContext ctx, String info) {
        //获取该通道盐值
        String salt = RuiliPcTcpHandler.getPcChMap().get(ctx.channel().remoteAddress().toString()).getSalt();

        String[] splitInfo = info.split(RuiliPcCmdAttr.SEG_INFO1_INFON);
        String userStr = splitInfo[0];
        String keyHashStr = splitInfo[1];//md5(md5(key)+salt)
        //解析加密数值
        try {
            BasicDBObject projections = new BasicDBObject();
            projections.append(RuiliAdmindbSegment.MONGODB_USER_KEY_KEY, 1).append("_id", 0);
            //BasicDBObject时Bson的实现
            BasicDBObject filter = new BasicDBObject();
            filter.put(RuiliAdmindbSegment.MONGODB_USER_NAME_KEY, userStr);
            FindIterable<Document> docIter = this.adminMgd.collection.find(filter).projection(projections) ;
            //forEach：异步操作
            docIter.forEach(new Block<Document>() {
                @Override
                public void apply(final Document document) {//每个doc所做的操作

                    //先获取db中user的key
                    String key = (String) document.get(RuiliAdmindbSegment.MONGODB_USER_KEY_KEY);
                    //查看查询到的密码明文
                    logger.debug("Key in DB : "+key);
                    //计算得到keyHash
                    String keyHashStrLocal = SimpleSaltMd5.getKeySaltHash(key, salt);
                    //打印出收到的keyHash和本地计算出来的keyHash
                    logger.debug("Key Hash Remot : "+keyHashStr);
                    logger.debug("Key Hash Local : "+keyHashStrLocal);
                    if(keyHashStrLocal.toUpperCase().equals(keyHashStr.toUpperCase())){
                        logger.info(ctx.channel().remoteAddress().toString()+"'s Key Correct!");
                        //如果当前状态是请求连接状态，才可以进行下一步
                        if(pcChMap.get(ctx.channel().remoteAddress().toString()).getStatus() == RuiliPcChannelAttr.REQUEST_CONNECT_STA) {
                            //设置该通道为信任
                            pcChMap.get(ctx.channel().remoteAddress().toString()).setStatus(RuiliPcChannelAttr.LOGINED_STA);
                        }else {                	//如果当前已经登录了
                            //do nothing!
                        }
                    }else {
                        logger.info(ctx.channel().remoteAddress().toString()+"'s Key Incorrect!");
                    }
                }}, new SingleResultCallback<Void>() {//所有操作完成后的工作
                @Override
                public void onResult(final Void result, final Throwable t) {
                    //查询操作结束后，查看是否登录成功
                    if(pcChMap.get(ctx.channel().remoteAddress().toString()).getStatus() == RuiliPcChannelAttr.LOGINED_STA) {//登录成功
                        //返回登录信息
                        writeFlushFuture(ctx,RuiliPcCmdAttr.PC_WANT_LOGIN+RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OK);
//	                    	ctx.writeAndFlush(Unpooled.copiedBuffer("Login"+TCP_ServerHandler4PC.SEG_CMD_DONE_SIGNAL+TCP_ServerHandler4PC.DONE_SIGNAL_OK,CharsetUtil.UTF_8));
                    }else {//登录失败，已经断开了
                        writeFlushFuture(ctx,RuiliPcCmdAttr.PC_WANT_LOGIN+RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_ERROR);
                        //删除这个通道
                        delCh(ctx);
                    }
                }
            });
        }catch(Exception e) {
            logger.error("",e);
        }

    }
    /**
     * 发送信息，会受到成功的信息，进而处理（不会阻塞等待）
     *
     * 消息末尾加上一个'\n'结束符
     * @param ctx 通道ctx
     * @param msg 要发送的String信息
     */
    public void writeFlushFuture(ChannelHandlerContext ctx,String msg) {
        //如果还没有断开
        if(ctx.channel().isActive()) {
            //如果这个channel没有到达水位的话，还可以写入
            //水位在active时设置
            if(ctx.channel().isWritable()) {
                ChannelFuture future = ctx.writeAndFlush(Unpooled.copiedBuffer(msg + RuiliPcCmdAttr.SEG_TOW_PACK, CharsetUtil.UTF_8));
                //等待发送完毕
                future.addListener(new ChannelFutureListener(){
                    @Override
                    public void operationComplete(ChannelFuture f) {
                        if(!f.isSuccess()) {
                            logger.error("",f.cause());
                        }
                    }
                });
            }
        }else {
            this.delCh(ctx);
        }
    }

    /**
     * 发送信息并等待成功
     * @param ctx 通道ctx
     * @param msg 要发送的ByteBuf信息
     */
    public void writeFlushFuture(ChannelHandlerContext ctx, ByteBuf msg) {
        //如果这个channel没有到达水位的话，还可以写入
        //水位在active时设置
        if(ctx.channel().isActive()&&ctx.channel().isWritable()) {
            ChannelFuture future = ctx.channel().writeAndFlush(msg);
            //发送完毕会返回一个信息
            future.addListener(new ChannelFutureListener(){
                @Override
                public void operationComplete(ChannelFuture f) {
                    if(!f.isSuccess()) {
                        delCh(ctx);
                        logger.error("",f.cause());
                    }
                }
            });
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
     * 获取ctx的远程地址字符串形式
     * @param ctx
     * @return
     */
    public static String getCtxRmAddrStr(ChannelHandlerContext ctx) {
        return ctx.channel().remoteAddress().toString();
    }
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        try {
            BasicDBObject projections = null;
            //转化为string
            String message = ((ByteBuf)msg).toString(CharsetUtil.UTF_8);
            String[] splitMsg = message.split(RuiliPcCmdAttr.SEG_CMD_INFO,2);//将CMD和info分成两段
            String cmd = splitMsg[0];
            logger.info("Got Cmd : "+ message);
            logger.debug("Msg Len: "+String.valueOf(splitMsg.length));
            //判断当前上位机状态（未登录、已登录等）
            if(RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).getStatus().equals(RuiliPcChannelAttr.DATA_GET_STA)) {//实时接收数据的时候不能进行其他操作
                switch(cmd) {
                    case RuiliPcCmdAttr.PC_STOP_GET_RTDATA://降级为登录状态
                        this.writeFlushFuture(ctx, RuiliPcCmdAttr.PC_STOP_GET_RTDATA+
                                RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OK);//发送完毕收到一个通知
                        RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).setTestName("");
                        RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).setStatus(RuiliPcChannelAttr.LOGINED_STA);
                        break;
                    default:
                        break;
                }
            }else if(RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).getStatus().equals(RuiliPcChannelAttr.REQUEST_CONNECT_STA)) {//已经登录REQUEST_CONNECT_STA) {LOGINED_STA
                //TODO 将REQUEST_CONNECT_STA改回来
                //判断cmd类型
                switch(cmd) {
                    case RuiliPcCmdAttr.PC_START_ONE_TEST:
                        BasicDBObject filterName = new BasicDBObject();
                        if(splitMsg.length>1) {//也就是除了cmd还有其他信息（filter信息）
                            //将信息划分为多个filters，实验名称;配置文件长度;配置文件
                            String[] infoStr = splitMsg[1].split(RuiliPcCmdAttr.SEG_INFO1_INFON,3);
                            if(infoStr.length < 3) {
                                break;
                            }
                            try {
                                String testName = infoStr[0];
                                String isoDate  = (testName.split("/", 2))[1];
                                int configFileLen = Integer.parseInt(infoStr[1]);
                                String testConfigFile = infoStr[2];
                                //长度不正确
                                if(configFileLen != testConfigFile.length()) {
                                    logger.warn(String.format("Test(\"%s\")'s Config File Len Error: Abandoned", testName));
                                    this.writeFlushFuture(ctx, RuiliPcCmdAttr.PC_START_ONE_TEST+
                                            RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_ERROR);
                                    break;
                                }else {
                                    filterName.put(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME, testName);
                                    //删掉重复的（之前已经有的）
                                    this.infoMgd.collection.deleteMany(filterName, new SingleResultCallback<DeleteResult>() {
                                        @Override
                                        public void onResult(final DeleteResult result, final Throwable t) {
                                            logger.info(String.format("Find Existed Test(\"%s\")'s Config File : Deleted\r\n", testName));
                                        }
                                    });
                                    //给该PC设置测试名称
                                    RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).setTestName(testName);

                                    Document doc = new Document(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME,testName)
                                            .append(RuiliInfodbSegment.TESTINFOMONGODB_KEY_ISODATE, isoDate)
                                            //当前插入的系统时间
                                            .append(RuiliInfodbSegment.TESTINFOMONGODB_KEY_INSERT_ISO_DATE, TimeUtils.getStrIsoSTime())
                                            //当前插入的月份，指向一个数据存储集合
                                            .append(RuiliInfodbSegment.TESTINFOMONGODB_KEY_COL_INDEX_OF_DATA, TimeUtils.getStrIsoMTime())
                                            .append(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTCONF, testConfigFile);
                                    this.infoMgd.insertOne(doc, new SingleResultCallback<Void>() {
                                        @Override
                                        public void onResult(final Void result, final Throwable t) {
                                            logger.info(String.format("Test(\"%s\") Config File Saved", testName));
                                            logger.info(String.format("-Config File Detail : \r\n  %s", testConfigFile));
                                            writeFlushFuture(ctx, RuiliPcCmdAttr.PC_START_ONE_TEST+
                                                    RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OK);
                                        }});
                                }
                            }catch(Exception e) {
                                logger.error("",e);
                            }
                        }
                        break;
                    case RuiliPcCmdAttr.MONGODB_FIND_DOCS_NAMES://获取所有的doc的test名称
                        //filter
                        BasicDBObject filters = new BasicDBObject();
                        if(splitMsg.length>1) {//也就是除了cmd还有其他信息（filter信息）
                            //splitMsg[1]格式    |key:info;key:info;......|
                            String[] filtersStr = splitMsg[1].split(RuiliPcCmdAttr.SEG_INFO1_INFON);//将信息划分为多个filters
                            //缓存filter的上下界
                            String lowerBound;String upperBound;
                            for(String filterStr:filtersStr) {//将过滤信息都put到filter中
                                String[] oneFilter = filterStr.split(RuiliPcCmdAttr.SEG_KEY_VALUE,2);//eg.{test:test1_201901251324}，没有参数也可以
                                switch(oneFilter[0]) {
                                    case RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME://过滤测试名称，test:xxxxx
                                        filters.put(oneFilter[0], oneFilter[1]);
                                        break;
                                    //过滤年月日,yyyy-MM-ddTHH:mm:ss
                                    case RuiliInfodbSegment.TESTINFOMONGODB_KEY_INSERT_ISO_DATE://配置文件插入的时间（以服务器时间为准）
                                    case RuiliInfodbSegment.TESTINFOMONGODB_KEY_ISODATE://配置文件插入的时间（以上位机时间为准，从测试名称中提取得到）
                                        String[] lowerUpperData = oneFilter[1].split(RuiliPcCmdAttr.SEG_LOWER_UPPER_BOUND);
                                        if(lowerUpperData.length > 1) {
                                            lowerBound = (oneFilter[1].split(RuiliPcCmdAttr.SEG_LOWER_UPPER_BOUND,2))[0];//小的日期
                                            upperBound = (oneFilter[1].split(RuiliPcCmdAttr.SEG_LOWER_UPPER_BOUND,2))[1];//大的日期
                                            filters.put(oneFilter[0], new BasicDBObject("$gte",lowerBound).append("$lte", upperBound));//>=和<=
                                        }else{
                                            //nothing to do
                                        }
                                        break;
                                }//end of case
                            }//end of for
                        }
                        //根据过滤信息来获取实验名称
                        projections = new BasicDBObject();
                        projections.append(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME, 1).append("_id", 0);
                        FindIterable<Document> findIter = this.infoMgd.collection.find(filters).projection(projections);
                        findIter.forEach(doc-> {
                            try {
                                //没有超过水位
                                if(!ctx.channel().isWritable()){
                                    ctx.flush();
                                }
                                ctx.write(Unpooled.copiedBuffer(RuiliPcCmdAttr.MONGODB_FIND_DOCS_NAMES+
                                        RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+
                                        (String)doc.get(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME)+
                                        RuiliPcCmdAttr.SEG_TOW_PACK, CharsetUtil.UTF_8));//发给上位机原始数据
                            }catch(Exception e) {
                                logger.error("",e);
                            }
                        },  (result, t) -> {
                                ctx.write(Unpooled.copiedBuffer(RuiliPcCmdAttr.MONGODB_FIND_DOCS_NAMES+
                                        RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+
                                        RuiliPcCmdAttr.DONE_SIGNAL_OVER+
                                        RuiliPcCmdAttr.SEG_TOW_PACK,CharsetUtil.UTF_8));
                                ctx.flush();
                                logger.debug(RuiliPcCmdAttr.MONGODB_FIND_DOCS_NAMES+RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OVER);
                        });

                        break;
                    //获取MongoDB中的文档信息，可以使用filter
                    //!!!!!只支持查询一次测试
                    case RuiliPcCmdAttr.MONGODB_FIND_DOCS:
                        RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).allowSendDocs();
                        //filte
                        BasicDBObject filterDocs = new BasicDBObject();
                        //TODO  to test
                        if(splitMsg.length >= 2) {//也就是除了cmd还有其他信息（filter信息）
                            //splitMsg[1]格式    |key:info;key:info;......|
                            String[] filtersStr = splitMsg[1].split(RuiliPcCmdAttr.SEG_INFO1_INFON);//将信息划分为多个filters
                            if(filtersStr.length >= 1) {
                                String[] oneFilter = filtersStr[0].split(RuiliPcCmdAttr.SEG_KEY_VALUE,2);
                                if(oneFilter.length >= 2) {
                                    filterDocs.put(oneFilter[0], oneFilter[1]);
                                }
                            }
                        }
                        //获取数据所存在的集合
                        this.infoMgd.collection.find(filterDocs).first(new SingleResultCallback<Document>() {//所有操作完成后的工作
                            @Override
                            public void onResult(final Document result, final Throwable t) {
                                try {
                                    String[] yyyy_MM = new String[2];
                                    yyyy_MM[0] = (String)result.get(RuiliInfodbSegment.TESTINFOMONGODB_KEY_COL_INDEX_OF_DATA);
                                    if(yyyy_MM[0] == null) {
                                        logger.warn("Cannot get colName");
                                        return;
                                    }
                                    //获取得到下一个月String
                                    SimpleDateFormat sdf= new SimpleDateFormat("yyyy-MM");
                                    Date date =sdf.parse(yyyy_MM[0]);
                                    Calendar calendar = Calendar.getInstance();
                                    calendar.setTime(date);
                                    calendar.add(Calendar.MONTH, 1);
                                    yyyy_MM[1] = sdf.format(calendar.getTime());
                                    if(yyyy_MM[1] == null) {
                                        logger.warn("Cannot get colName");
                                        return;
                                    }
                                    //查询集合和下一个月的集合
                                    //最多查询两个月，时间跨度不能太大
//                                    for(String colName : yyyy_MM) {
                                        //指向相应的集合
                                        String colName = yyyy_MM[0];
                                        generalMgd.resetCol(colName);
                                        BasicDBObject projections = new BasicDBObject();
                                        projections.append(RuiliDatadbSegment.MONGODB_KEY_RAW_DATA, 1).append("_id", 0);
                                        FindIterable<Document> docIter = dataMgd.collection.find(filterDocs).projection(projections);
                                        docIter.forEach(document-> {
                                            if(!RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).isAllowSendDocs()){
                                                return;
                                            }
                                            try {
                                                //没有超过水位
                                                if(!ctx.channel().isWritable()){
                                                    ctx.flush();
                                                }
                                                ctx.write(Unpooled.copiedBuffer(RuiliPcCmdAttr.MONGODB_FIND_DOCS+RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL,CharsetUtil.UTF_8));//加入抬头
                                                Binary rawDataBin = (Binary)document.get(RuiliDatadbSegment.MONGODB_KEY_RAW_DATA);
                                                byte[] rawDataByte = rawDataBin.getData();
                                                ctx.write(Unpooled.wrappedBuffer(rawDataByte));//发给上位机原始数据
                                            }catch(Exception e) {
                                                logger.error("",e);
                                        }}, (result4SingleResultCallback, thread4SingleResultCallback) -> {//所有操作完成后的工作
//                                                if(colName.equals(yyyy_MM[1])){
                                                ctx.write(Unpooled.copiedBuffer(RuiliPcCmdAttr.MONGODB_FIND_DOCS+
                                                        RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OVER,CharsetUtil.UTF_8));
                                                ctx.flush();
                                                logger.debug(RuiliPcCmdAttr.MONGODB_FIND_DOCS+RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OVER);
                                                RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).allowSendDocs();
//                                                }
                                        });
//                                    }
                                } catch (ParseException e) {
                                    logger.info("",e);
                                }
                            }
                        });
                        break;
                    case RuiliPcCmdAttr.PC_WANT_GET_TEST_CONFIG:
                        try {
                            String testName = splitMsg[1].trim();
                            BasicDBObject filter = new BasicDBObject();
                            filter.put(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME, testName);
                            //查找第一个满足testname满足要求的配置文件
                            this.infoMgd.collection.find(filter).first(new SingleResultCallback<Document>() {
                                @Override
                                public void onResult(final Document doc, final Throwable t) {
                                    if(doc == null) {
                                        return;
                                    }
                                    writeFlushFuture(ctx,RuiliPcCmdAttr.PC_WANT_GET_TEST_CONFIG+
                                            RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+doc.get(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTCONF));
                                    writeFlushFuture(ctx,RuiliPcCmdAttr.PC_WANT_GET_TEST_CONFIG+
                                            RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OVER);
                                }
                            });
                        }catch(Exception e) {
                            logger.error("",e);
                        }
                        break;
                    case RuiliPcCmdAttr.MONGODB_CREATE_COL://创建collection
                        //TODO
                        break;
                    case RuiliPcCmdAttr.PC_WANT_STOP_GET_DOCS://创建collection
                        RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).stopSendDocs();
                        this.writeFlushFuture(ctx,RuiliPcCmdAttr.PC_WANT_STOP_GET_DOCS+
                                RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OK);
                        break;
                    case RuiliPcCmdAttr.PC_WANT_GET_RTDATA://修改位GetRtData的状态
                        try {
                            String testName = splitMsg[1].trim();
                            RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).setTestName(testName);
                            RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).setStatus(RuiliPcChannelAttr.DATA_GET_STA);
                            this.writeFlushFuture(ctx,RuiliPcCmdAttr.PC_WANT_GET_RTDATA+
                                    RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OK);
                        }catch(Exception e){
                            logger.error("",e);
                        }

                        break;
                    default:
                        break;
                }
            }else if(RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).getStatus().equals(RuiliPcChannelAttr.REQUEST_CONNECT_STA)) {//连接但还未登录
                switch(cmd) {
                    case RuiliPcCmdAttr.PC_WANT_LOGIN://PC想要登录
                        String info = splitMsg[1];
                        logger.debug("Info : "+info);
                        //当前状态时请求连接状态而且用户名和密码匹配成功
                        loginMd5(ctx,info);
                        break;
                    default://不认识的命令，强制断开
                        this.delCh(ctx);
                        break;
                }
            }//end of if elif
            //不管登录与否，都要处理的命令
            switch(cmd) {
                case RuiliPcCmdAttr.HEART_BEAT_SIGNAL://心跳包
                    //TODO 每次更新心跳包的时间，过一段时间检查是否超过时间
                    this.writeFlushFuture(ctx,RuiliPcCmdAttr.HEART_BEAT_SIGNAL+
                            RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OK);
                    break;
                case RuiliPcCmdAttr.PC_WANT_DISCONNECT://上位机想要断开连接
                    this.writeFlushFuture(ctx,RuiliPcCmdAttr.PC_WANT_DISCONNECT+
                            RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+RuiliPcCmdAttr.DONE_SIGNAL_OK);
                    this.delCh(ctx);
                    break;
                default:
                    break;
            }
        }
        catch(Exception e) {
            logger.error("",e);
        }
        finally {
            // 抛弃收到的数据
            ReferenceCountUtil.release(msg);//如果不是继承的SimpleChannel...则需要自行释放msg
        }
    }//end of channelRead
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //设置消息队列流量水位，不能太多，否则会造成积压
        ctx.channel().config().setWriteBufferHighWaterMark(50*1024*1024);//50MB
        logger.info(String.format("PC %s connected!",ctx.channel().remoteAddress()));
        //通道数过多
        if(RuiliPcTcpHandler.pcChMap.size() > RuiliPcTcpHandler.MAX_CHANNEL_NUM) {
            channelInactive(ctx);//关闭通道
            return;
        }
        //加入该通道
        RuiliPcTcpHandler.pcChMap.put(ctx.channel().remoteAddress().toString(), new RuiliPcChannelAttr(ctx));
        logger.info(String.format("Channel Num : %d", RuiliPcTcpHandler.getPcChMap().size()));
        String salt = RuiliPcTcpHandler.pcChMap.get(ctx.channel().remoteAddress().toString()).getSalt();
        this.writeFlushFuture(ctx,"RandStr"+RuiliPcCmdAttr.SEG_CMD_DONE_SIGNAL+salt);
        logger.debug("Salt : "+salt);
        ctx.fireChannelActive();
    }//end of channelActive
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        this.delCh(ctx);
        logger.info(String.format("PC %s disconnected!",ctx.channel().remoteAddress().toString()));
        logger.info(String.format("Channel Num : %d", RuiliPcTcpHandler.pcChMap.size()));
        ctx.fireChannelInactive();
    }
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 当出现异常就关闭连接
        logger.error("",cause);
        this.delCh(ctx);
    }
}
