package cn.sorl.rcloud.biz.threads;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.common.diagnosis.ServerMonitor;
import cn.sorl.rcloud.common.time.TimeUtils;
import cn.sorl.rcloud.common.util.PropertiesUtil;
import cn.sorl.rcloud.common.util.PropertyLabel;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliDatadbSegment;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliDbStateSegment;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliInfodbSegment;
import cn.sorl.rcloud.dal.netty.RuiliPcChannelAttr;
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
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.Map;

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
    PropertiesUtil propertiesUtil = new PropertiesUtil(PropertyLabel.PROPERTIES_FILE_ADDR);

    // 服务器状态检测周期
    private final static String SERVER_CHECK_TIME = "20";

    /**
     * 服务器状态管理
     */
    ServerMonitor serverMonitor = new ServerMonitor(10,10);
    /**
     * 服务器问题出现后的时间，用于实现发邮件不要过于频繁
     */
    int serverErrorAppearenceTime = 0;
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
//    @Scheduled(cron="0/10 * * * * *")  // 测试用，每10秒检查一次
    @Scheduled(cron="0 0 2 * * ?")  //每天凌晨2点
    public void checkDatabase() {
        try {
            propertiesUtil.updateProps();
            // 检查数据库
            SimpleMgd dbStateMgd = (SimpleMgd) BeanContext.context.getBean("dbStateMgd");
            BasicDBObject projections = new BasicDBObject();
            projections.append(RuiliDbStateSegment.DB_MEM_USAGE_RATE_KEY, 1)
                    .append(RuiliDbStateSegment.DB_CPU_LOGICAL_KERNEL_NUM_KEY,1)
                    .append(RuiliDbStateSegment.DB_CPU_USAGERATE_15MIN_KEY,1)
                    .append(RuiliDbStateSegment.DB_DISK_FREE_SPACE_G_KEY,1)
                    .append("_id", 0);
            //BasicDBObject时Bson的实现
            BasicDBObject filter = new BasicDBObject();
            dbStateMgd.collection.find().first(new SingleResultCallback<Document>() {
                @Override
                public void onResult(Document result, Throwable t) {
                    propertiesUtil.updateProps();
                    int freeDiskSpaceInt = Integer.parseInt(result.getString(RuiliDbStateSegment.DB_DISK_FREE_SPACE_G_KEY));
                    int freeDiskSpaceIntMin = Integer.parseInt(propertiesUtil.readValue(PropertyLabel.SERVER_DISK_MINIMUM_G_KEY));
                    double memUsageRate = Double.parseDouble(result.getString(RuiliDbStateSegment.DB_MEM_USAGE_RATE_KEY));
                    double memUsageRateMax = Double.parseDouble(propertiesUtil.readValue(PropertyLabel.SERVER_MEM_MAX_USAGE_KEY));
                    // 获取单个CPU核的使用率
                    double cpuAllKernelUsage = Double.parseDouble(result.getString(RuiliDbStateSegment.DB_CPU_USAGERATE_15MIN_KEY));
                    int cpuLogicalKernelNum = Integer.parseInt(result.getString(RuiliDbStateSegment.DB_CPU_LOGICAL_KERNEL_NUM_KEY));
                    double cpuSingleKernelUsageRate = cpuAllKernelUsage / cpuLogicalKernelNum;
                    double cpuSingleKernelUsageRateMax = Double.parseDouble(propertiesUtil.readValue(PropertyLabel.SERVER_CPU_MAX_USAGE_KEY));
                    // 开启邮件提醒
                    if (freeDiskSpaceInt < freeDiskSpaceIntMin||memUsageRate > memUsageRateMax ||cpuSingleKernelUsageRate > cpuSingleKernelUsageRateMax) {
                        logger.warn("DB Server Faulty!");
                        dbAppServerAlarm(propertiesUtil,cpuSingleKernelUsageRate,cpuSingleKernelUsageRateMax,memUsageRate,memUsageRateMax,freeDiskSpaceInt,freeDiskSpaceIntMin,1.0);
                    }
                }
            });

        } catch (Exception e) {
            logger.error("",e);
        }
    }

    /**
     * 服务器状态管理
     */
    @Scheduled(cron="0 0/"+SERVER_CHECK_TIME+" * * * ?")
//    @Scheduled(cron="0/5 * * * * ?")
    public void checkServerState () {
        PropertiesUtil serverPropUtil = new PropertiesUtil(propertiesUtil.readValue(PropertyLabel.SERVER_PROPS_ADDR_KEY));
        propertiesUtil.updateProps();
        int diagnosisEmailTime = Integer.parseInt(propertiesUtil.readValue(PropertyLabel.SERVER_DIAGNOSIS_EMAIL_TIME_KEY));
        if (this.serverErrorAppearenceTime > diagnosisEmailTime) {
            this.serverErrorAppearenceTime = 0;
        }else if(this.serverErrorAppearenceTime > 0) {
            this.serverErrorAppearenceTime += 1;
        }else if(this.serverErrorAppearenceTime == 0) {
            int freeDiskSpaceInt = Integer.parseInt(serverPropUtil.readValue(PropertyLabel.SERVER_DISK_FREE_SPACE_G_KEY));
            int freeDiskSpaceIntMin = Integer.parseInt(propertiesUtil.readValue(PropertyLabel.SERVER_DISK_MINIMUM_G_KEY));
            double memUsageRate = Double.parseDouble(serverPropUtil.readValue(PropertyLabel.SERVRE_MEM_USAGE_RATE_KEY));
            double memUsageRateMax = Double.parseDouble(propertiesUtil.readValue(PropertyLabel.SERVER_MEM_MAX_USAGE_KEY));
            // 获取单个CPU核的使用率
            double cpuAllKernelUsage = Double.parseDouble(serverPropUtil.readValue(PropertyLabel.SERVER_CPU_USAGE_RAGE_15MIN_KEY));
            int cpuLogicalKernelNum = Integer.parseInt(serverPropUtil.readValue(PropertyLabel.SERVER_CPU_LOGICAL_KERNEL_NUM_KEY));
            double cpuSingleKernelUsageRate = cpuAllKernelUsage / cpuLogicalKernelNum;
            double cpuSingleKernelUsageRateMax = Double.parseDouble(propertiesUtil.readValue(PropertyLabel.SERVER_CPU_MAX_USAGE_KEY));
            // 开启邮件提醒
            if (freeDiskSpaceInt < freeDiskSpaceIntMin||memUsageRate > memUsageRateMax ||cpuSingleKernelUsageRate > cpuSingleKernelUsageRateMax) {
                logger.warn("Application Server Faulty!");
                dbAppServerAlarm(propertiesUtil,cpuSingleKernelUsageRate,cpuSingleKernelUsageRateMax,memUsageRate,memUsageRateMax,freeDiskSpaceInt,freeDiskSpaceIntMin,diagnosisEmailTime*Integer.parseInt(SERVER_CHECK_TIME)/1440.0);
                this.serverErrorAppearenceTime+=1;
            }
        }
    }

    /**
     * 发送邮件提醒管理员修复
     * @param propertiesUtil
     * @param cpuSingleKernelUsageRate
     * @param cpuSingleKernelUsageRateMax
     * @param memUsageRate
     * @param memUsageRateMax
     * @param freeDiskSpaceInt
     * @param freeDiskSpaceIntMin
     * @param diagnosisEmailTimeDay
     */
    private void dbAppServerAlarm (PropertiesUtil propertiesUtil,double cpuSingleKernelUsageRate, double cpuSingleKernelUsageRateMax, double memUsageRate, double memUsageRateMax, int freeDiskSpaceInt, int freeDiskSpaceIntMin, double diagnosisEmailTimeDay) {
        if (propertiesUtil.readValue(PropertyLabel.MAIL_ENABLE_KEY).equals(PropertyLabel.MAIL_ENABLE_YES)) {
            String subject = "R-CLOUD 应用服务器";
            if (cpuSingleKernelUsageRate > cpuSingleKernelUsageRateMax) {
                subject += "CPU使用率过高；";
            }
            if (memUsageRate > memUsageRateMax) {
                subject += "内存空间不足；";
            }
            if (freeDiskSpaceInt < freeDiskSpaceIntMin) {
                subject += "磁盘空间不足；";
            }

            String content = String.format("<br>CPU单个逻辑核使用率 ： %.2f %%（警戒线 ： < %.2f %%）<br>内存使用率 ： %.2f%% （警戒线 ： < %.2f %%）<br>磁盘剩余空间 ： %d G （警戒线 ： > %d G）<br>下次提醒时间：%.3f 天后<br><br>-----<br>瑞立集团网络技术中心",
                    cpuSingleKernelUsageRate * 100, cpuSingleKernelUsageRateMax * 100, memUsageRate * 100 , memUsageRateMax * 100,freeDiskSpaceInt,freeDiskSpaceIntMin, diagnosisEmailTimeDay);
            logger.warn(content);
            if (propertiesUtil.readValue(PropertyLabel.MAIL_ENABLE_KEY).equals(PropertyLabel.MAIL_ENABLE_YES)) {
                EmailSender emailSender = new EmailSender(propertiesUtil);
                for (String mailAdr : propertiesUtil.readValue(PropertyLabel.MAIL_LIST_KEY).split(PropertyLabel.MAIL_LIST_SPLIT)) {
                    emailSender.sendMail(mailAdr, subject, content);
                    logger.info("Email Send to : " + mailAdr);
                }
            }
        }
    }
    /**
     * 每10min检查一次的配置，MongoDB数据库的地址是否改变了
     */
//    @Scheduled(cron="0 0/10 * * * ?")
    @Scheduled(cron="0 0/10 * * * ?")
    public void checkProperties10Min() {
        try {
            propertiesUtil.updateProps();
            logger.info(propertiesUtil.getProps().toString());
            SimpleMgd adminMgd = BeanContext.context.getBean("adminMgd", SimpleMgd.class);
            String adminMgdAddr = adminMgd.getMgdAddr();
            // 如果发现了mgd地址该了
            if (!propertiesUtil.getProps().get(PropertyLabel.DB_MONGODB_ADDR_KEY).equals(adminMgdAddr)) {
                logger.info("MongoDB Addr Changed!Starting Change Mongo Client Connection...");
                // 重新建立连接
                adminMgd.conn2Mgd();

                SimpleMgd infoMgd = BeanContext.context.getBean("infoMgd", SimpleMgd.class);
                String infoMgdAddr = infoMgd.getMgdAddr();
                if (!propertiesUtil.getProps().get(PropertyLabel.DB_MONGODB_ADDR_KEY).equals(infoMgdAddr)) {
                    // 重新建立连接
                    infoMgd.conn2Mgd();
                }

                SimpleMgd generalMgd = BeanContext.context.getBean("generalMgd", SimpleMgd.class);
                String generalMgdAddr = generalMgd.getMgdAddr();
                if (!propertiesUtil.getProps().get(PropertyLabel.DB_MONGODB_ADDR_KEY).equals(generalMgdAddr)) {
                    // 重新建立连接
                    generalMgd.conn2Mgd();
                }

                SimpleMgd dataMgd = BeanContext.context.getBean("dataMgd", SimpleMgd.class);
                String dataMgdAddr = dataMgd.getMgdAddr();
                if (!propertiesUtil.getProps().get(PropertyLabel.DB_MONGODB_ADDR_KEY).equals(dataMgdAddr)) {
                    // 重新建立连接
                    dataMgd.conn2Mgd();
                }

            }
        }catch (Exception e) {
            logger.error("", e);
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
