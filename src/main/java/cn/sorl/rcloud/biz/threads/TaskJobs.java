package cn.sorl.rcloud.biz.threads;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.common.time.TimeUtils;
import cn.sorl.rcloud.common.util.PropertiesUtil;
import cn.sorl.rcloud.common.util.PropertyLabel;
import cn.sorl.rcloud.common.util.ShellCallUtil;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliDatadbSegment;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliInfodbSegment;
import cn.sorl.rcloud.dal.netty.RuiliPcChannelAttr;
import cn.sorl.rcloud.dal.netty.RuiliPcCmdAttr;
import cn.sorl.rcloud.dal.netty.RuiliPcTcpHandler;
import cn.sorl.rcloud.dal.network.EmailSender;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.ClientSession;
import com.mongodb.async.client.FindIterable;
import com.mongodb.async.client.MongoIterable;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.result.DeleteResult;
import io.netty.buffer.Unpooled;
import io.netty.buffer.UnpooledUnsafeDirectByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import jdk.nashorn.tools.Shell;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;

/**
 *
 * 部分周期性执行任务
 *
 * @author  neyzoter song
 */
@Component("taskJob")
public class TaskJobs {
    private final static Logger logger = LoggerFactory.getLogger(TaskJobs.class);
    //清除60天前的数据
    private final static int DAYS_BEFORE_TODAY = -60;
    //配置任务的执行时间，可以配置多个（根据测试名称时间）
    private final static String hms4MgdClearByIsodate = "T04:00:00";
    //配置任务的执行时间，可以配置多个(根据插入时间删除)
    private final static String hms4MgdClearByInsertIsodate = "T03:00:00";
    PropertiesUtil propertiesUtil = new PropertiesUtil("/home/scc/configuration/rcloud_configuration.properties");
    ShellCallUtil shellCallUtil = new ShellCallUtil(propertiesUtil);

    /**
     * 更新用于插入数据的MongoClient所指向的集合
     */
    @Scheduled(cron="0 0 0 1 * ?")
    public void dataMgdUpdate(){
        try{
            logger.info("Start updating data mongoclient's collection...");

            //将dataMgd重新指向一个集合
            SimpleMgd dataMgd = BeanContext.context.getBean("dataMgd", SimpleMgd.class);
            dataMgd.resetCol(TimeUtils.getStrIsoMTime());
            //建立一个索引
            if(!dataMgd.getIndexName().equals("")) {
                dataMgd.collection.createIndex(Indexes.descending(dataMgd.getIndexName()), new SingleResultCallback<String>() {
                    @Override
                    public void onResult(final String result, final Throwable t) {
                        logger.info(String.format("db.col create index by \"%s\"(indexName_-1)", result));
                    }
                });
            }
            logger.info("Updated data mongoclient's collection successfully");
        }catch (Exception e){
            logger.error("", e);
        }
    }
    /**
     * 每个连接通道实时数据下行
     * @throws ParseException
     */
//    @Scheduled(cron="0/5 * * * * ?")
    public void sendRtData(){
        try{

            for(Iterator<Map.Entry<String, RuiliPcChannelAttr>> item = RuiliPcTcpHandler.getPcChMap().entrySet().iterator(); item.hasNext();) {
                Map.Entry<String,RuiliPcChannelAttr> entry = item.next();
                ChannelHandlerContext ctx = entry.getValue().getContext();
                synchronized (ctx){
//                    logger.info(String.format("ctx : %s ",ctx.channel().toString()));
                    ctx.channel().flush();
//                    ctx.writeAndFlush(Unpooled.copiedBuffer("sendRtData...",CharsetUtil.UTF_8));
                }
            }
        }catch(Exception e){
            logger.error("", e);
        }
    }
//    @Scheduled(cron="0/1 * * * * ?")
    public void writeRtData(){
        try{

            for(Iterator<Map.Entry<String, RuiliPcChannelAttr>> item = RuiliPcTcpHandler.getPcChMap().entrySet().iterator(); item.hasNext();) {
                Map.Entry<String,RuiliPcChannelAttr> entry = item.next();
                ChannelHandlerContext ctx = entry.getValue().getContext();
                synchronized (ctx){
                    ctx.channel().write(Unpooled.copiedBuffer("sendRtData...",CharsetUtil.UTF_8));
                }
            }
        }catch(Exception e){
            logger.error("", e);
        }
    }
    /**
     * 基于从testName提取出来的isodate为基础，进行数据清除
     *
     * @apiNote 使用isodate而不使用insertIsodate的原因：
     * 如果私有云数据库本身系统时间有问题，可能会造成把所有数据都删除的问题。
     * 而isodate从配置文件或者是数据中提取，删除也只是会删除那一次的配置文件或者数据
     * @deprecated
     */
    //取消以下注释，则周期性运行
//	@Scheduled(cron="0 0 4 * * ?")  //凌晨4点执行数据库清空指令（DAYS_BEFORE_TODAY天之前的数据）
    public void mgdClearByIsodate() {
        try {
            logger.info("mgdClearByInsertIsodate Start Clearing N-day-before datas and configurations");
            SimpleMgd generalMgd = BeanContext.context.getBean("generalMgd", SimpleMgd.class);
            SimpleMgd infoMgd = BeanContext.context.getBean("infoMgd", SimpleMgd.class);
            generalMgd.getClient().startSession(new SingleResultCallback<ClientSession>() {
                @Override
                public void onResult(final ClientSession sess, final Throwable t) {
                    if(t != null) {
                        logger.warn("StartSession Throwable is not null",t);
                    }
                    //如果不支持事务，则不开启
                    if(sess != null) {
                        sess.startTransaction();
                    }
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        //今日日期
                        Calendar calendar=new GregorianCalendar();
                        logger.info(String.format("Date today : " + sdf.format(calendar.getTime()) + TaskJobs.hms4MgdClearByIsodate));
                        //N天前的日期
                        calendar.add(Calendar.DATE, TaskJobs.DAYS_BEFORE_TODAY);
                        String upperBound = sdf.format(calendar.getTime()) + TaskJobs.hms4MgdClearByIsodate;
                        logger.info(String.format("Date before N days : " + upperBound));
                        //基于插入时间（系统时间，所以系统时间不能错）
                        BasicDBObject filter = new BasicDBObject();
                        filter.put(RuiliInfodbSegment.TESTINFOMONGODB_KEY_ISODATE, new BasicDBObject("$lte",upperBound));
                        // 返回的document包含那些内容，后面只有testname需要
                        BasicDBObject projections = new BasicDBObject();
                        projections.append(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME, 1)
                                .append(RuiliInfodbSegment.TESTINFOMONGODB_KEY_COL_INDEX_OF_DATA, 1)
                                .append("_id", 0);
                        //设置指向配置文件的col
                        generalMgd.resetCol(infoMgd.getColName());
                        FindIterable<Document> findIter = generalMgd.collection.find(filter).projection(projections)
                                .sort(new BasicDBObject(RuiliInfodbSegment.TESTINFOMONGODB_KEY_COL_INDEX_OF_DATA, 1));
                        findIter.forEach(new Block<Document>() {
                            @Override
                            public void apply(Document doc) {
                                try {
                                    //指向数据集合
                                    if(!((String)doc.get(RuiliInfodbSegment.TESTINFOMONGODB_KEY_COL_INDEX_OF_DATA)).equals(generalMgd.getColName())) {
                                        generalMgd.resetCol((String)doc.get(RuiliInfodbSegment.TESTINFOMONGODB_KEY_COL_INDEX_OF_DATA));
                                    }
                                    logger.info(String.format("Connected to db.col(%s.%s) ", generalMgd.getDbName(),generalMgd.getColName()));
                                    String testName = (String)doc.get(RuiliInfodbSegment.TESTINFOMONGODB_KEY_TESTNAME);
                                    //删除ADC和CAN数据
                                    generalMgd.collection.deleteMany(new BasicDBObject(RuiliDatadbSegment.MONGODB_KEY_TESTNAME,testName), new SingleResultCallback<DeleteResult>() {
                                        @Override
                                        public void onResult(final DeleteResult result, final Throwable t) {
                                            logger.info(String.format("Cleared %d documents", result.getDeletedCount()));
                                        }
                                    });
                                }catch(Exception e) {
                                    logger.info("",e);
                                }
                            }
                        },  new SingleResultCallback<Void>() {
                            @Override
                            public void onResult(final Void result, final Throwable t) {
                                //删除config数据，需要改变generalMgdInterface，所以必须在上一步结束后进行
                                generalMgd.resetCol(infoMgd.getColName());
                                generalMgd.collection.deleteMany(filter, new SingleResultCallback<DeleteResult>() {
                                    @Override
                                    public void onResult(final DeleteResult result, final Throwable t) {
                                        logger.info(String.format("Cleared %d configurations", result.getDeletedCount()));
                                    }
                                });
                            }
                        });

                        if(sess != null) {
                            sess.commitTransaction(new SingleResultCallback<Void>() {
                                @Override
                                public void onResult(final Void result, final Throwable t) {
                                }
                            });
                        }
                    }catch(Exception e) {
                        logger.error("",e);
                        if(sess != null) {
                            sess.abortTransaction(new SingleResultCallback<Void>() {
                                @Override
                                public void onResult(final Void result, final Throwable t) {
                                }
                            });
                        }
                    }
                }
            });
        }catch (Exception e) {
            logger.error("",e);
        }
    }

    /**
     * 检查磁盘空间的大小，如果过小则发送邮件
     */
    @Scheduled(cron="0 00 02 * * MON-FRI")  //每周周一到五凌晨2点
    public void checkDiskSpace() {
        try {
            propertiesUtil.updateProps();
            String script = shellCallUtil.getProperty(PropertyLabel.DISK_SPACE_SCRIPT_KEY);
            String args = shellCallUtil.getProperty(PropertyLabel.DISK_SPACE_ARGS_KEY);
            String workspace = shellCallUtil.getProperty(PropertyLabel.DISK_SPACE_WORKSPACE_KEY);
            String freeSpace = shellCallUtil.callShell(script,args,workspace);
            logger.info("Check Disk Space Free: DIR = " + workspace+"\t" + freeSpace);
            Integer freeSpaceInt = Integer.parseInt(freeSpace.split("G")[0]);
            // 开启邮件提醒
            if (freeSpaceInt < Integer.parseInt(propertiesUtil.readValue(PropertyLabel.DISK_SPACE_MINIMUM_G_KEY))) {
                logger.warn("R-CLOUD STORAGE NOT ENOUGH");
                if (propertiesUtil.readValue(PropertyLabel.MAIL_ENABLE_KEY).equals(PropertyLabel.MAIL_ENABLE_YES)) {
                    String subject = "R-CLOUD 服务器空间不足";
                    String content = "<br>剩余存储空间 ：" + freeSpace+ "<br><br>-----<br>瑞立集团网络技术中心";
                    // 逐个发送给用户
                    EmailSender emailSender = new EmailSender(propertiesUtil);
                    for (String mailAdr : propertiesUtil.readValue(PropertyLabel.MAIL_LIST_KEY).split(PropertyLabel.MAIL_LIST_SPLIT)) {
                        emailSender.sendMail(mailAdr, subject, content);
                        logger.info("Email Send to : " + mailAdr);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("",e);
        }
    }


    /**
     * 删除，分别从config和data集合删除数据，没有加速措施
     * @deprecated
     */
//    @Scheduled(cron="0 0 3 1 * ?")  //每个月1号凌晨3点清除一次
    public void mgdClearByInsertIsodateSeperate() {
        try {
            logger.info("mgdClearByInsertIsodate Start Clearing N-day-before documents and configurations");
            SimpleMgd generalMgd = BeanContext.context.getBean("generalMgd", SimpleMgd.class);

            generalMgd.getClient().startSession(new SingleResultCallback<ClientSession>() {
                @Override
                public void onResult(final ClientSession sess, final Throwable t) {
                    if(t != null) {
                        logger.warn("StartSession Throwable is not null",t);
                    }
                    //如果不支持事务，则不开启
                    if(sess != null) {
                        sess.startTransaction();
                    }
                    try {
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                        //今日日期
                        Calendar calendar = new GregorianCalendar();
                        logger.info(String.format("Date today ： " + sdf.format(calendar.getTime()) + TaskJobs.hms4MgdClearByInsertIsodate));
                        //N天前的日期
                        calendar.add(Calendar.DATE, TaskJobs.DAYS_BEFORE_TODAY);
                        String upperBound = sdf.format(calendar.getTime()) + TaskJobs.hms4MgdClearByInsertIsodate;
                        logger.info(String.format("Date before N days ： " + upperBound));
                        BasicDBObject filter = new BasicDBObject();
                        MongoIterable<String> colNameList = generalMgd.getDb().listCollectionNames();
                        colNameList.forEach(new Block<String>() {
                            @Override
                            public void apply(String colName) {
                                try {
                                    //设置指向集合
                                    generalMgd.resetCol(colName);
                                    if(colName.equals("config")) {
                                        //testInfoMgd的dataMgd的插入文档时间字段相同，均为insertIsodate
                                        filter.put(RuiliInfodbSegment.TESTINFOMONGODB_KEY_INSERT_ISO_DATE, new BasicDBObject("$lte",upperBound));
                                    }else {
                                        filter.put(RuiliDatadbSegment.MONGODB_KEY_INSERT_ISO_DATE, new BasicDBObject("$lte",upperBound));
                                    }
                                    generalMgd.collection.deleteMany(filter, new SingleResultCallback<DeleteResult>() {
                                        @Override
                                        public void onResult(final DeleteResult result, final Throwable t) {
                                            if(colName.equals("config")) {
                                                logger.info(String.format("Cleared %d configurations", result.getDeletedCount()));
                                            }else {
                                                logger.info(String.format("Cleared %d documents", result.getDeletedCount()));
                                            }

                                        }
                                    });
                                }catch(Exception e) {
                                    logger.info("",e);
                                }
                            }
                        },  new SingleResultCallback<Void>() {
                            @Override
                            public void onResult(final Void result, final Throwable t) {
                                logger.info("mgdClearByInsertIsodate Cleared N-day-before documents and configurations Over");
                            }
                        });
                        if(sess != null) {
                            sess.commitTransaction(new SingleResultCallback<Void>() {
                                @Override
                                public void onResult(final Void result, final Throwable t) {
                                }
                            });
                        }
                    }catch(Exception e) {
                        logger.error("",e);
                        if(sess != null) {
                            sess.abortTransaction(new SingleResultCallback<Void>() {
                                @Override
                                public void onResult(final Void result, final Throwable t) {
                                }
                            });
                        }
                    }
                }
            });
        }catch (Exception e) {
            logger.error("",e);
        }
    }

}
