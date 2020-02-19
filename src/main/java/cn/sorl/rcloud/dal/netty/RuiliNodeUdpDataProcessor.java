package cn.sorl.rcloud.dal.netty;

import cn.sorl.rcloud.common.time.TimeUtils;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliDatadbSegment;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.async.SingleResultCallback;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @description 数据处理器，用于处理设备50发送到服务器的ByteBuf数据
 * @remarks 数据格式：[0:PACKAGE_TIME_IO_LENGTH-1]：[时间:ADC数据长度:IO:ID:校验码]；
 * [PACKAGE_TIME_IO_LENGTH-1:PACKAGE_TIME_IO_LENGTH+MAX_TEST_NAME-1]：[时间:ADC数据长度:IO:ID:校验码]；
 * @examp 00 00 10 10 48 10 01 00 10 00 00 00 01 01 00 48 74 65 73 74 31 5f 32 30 31 39 30 33 30 31 5f 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 01 02 00 05 01 03 00 06 01 02 00 05 01 03 00 05
 * @remoteIP 115.159.154.160
 * @author  nesc420
 * @data    2019-4-8
 * @version 0.0.3
 */
public class RuiliNodeUdpDataProcessor {
    private int BytebufLength;
    private short checkUbyte;
    private short nodeId;
    private long yyyy_mm_dd;
    private long headtime;
    //从testName获取的iso时间
    private String isodate;
    //插入文档时候的iso时间
    private String insertIsodate;
    private long data_count;
    private short io1,io2;
    //数据类型 @ref “CAN“ or “ADC“
    private String dataType;
    private String testName;

    private SimpleMgd dataMgd;
    private static final Logger logger = LoggerFactory.getLogger(RuiliNodeUdpAttr.class);

    /**
     * 构造类
     */
    public RuiliNodeUdpDataProcessor(SimpleMgd dataMgd){
        this.dataMgd = dataMgd;
    }
//    @PostConstruct
//    public void init(){
//
//    }
    /**
     * 数据库连接对象的构造函数
     *
     * @param simpleMgd 数据库
     * @return 无
     */
    public void setMongodb(SimpleMgd simpleMgd) {
        this.dataMgd = simpleMgd;
        //指向当前的数据集合
        this.dataMgd.resetCol(TimeUtils.getStrIsoMTime());
    }
    /**
     * 数据库连接对象返回
     * @return
     */
    public SimpleMgd getMongodb() {
        return this.dataMgd;
    }
    /**
     * 总的数据包的解析和存储方法
     *
     * @param msg 传入的ByteBuf数据。
     * @return none
     */
    public void dataProcess(ByteBuf msg){
        //更新帧头的信息
        if(!getFrameHead(msg)) {
            return;
        }
        byte[] byteRawData = new byte[msg.readableBytes()];
        //读取msg，写入到byteRawData
        msg.readBytes(byteRawData);
        Document doc = new Document(RuiliDatadbSegment.MONGODB_KEY_NODE_ID,nodeId)//该包的节点
                .append(RuiliDatadbSegment.MONGODB_KEY_YYYYMMDD, yyyy_mm_dd)//改包的年月日
                .append(RuiliDatadbSegment.MONGODB_KEY_HEADTIME,headtime)//改包的起始时间
                .append(RuiliDatadbSegment.MONGODB_KEY_ISO_DATE,isodate)//iso时间，从testName中获取
                .append(RuiliDatadbSegment.MONGODB_KEY_INSERT_ISO_DATE, insertIsodate)//插入文档的时间
                .append(RuiliDatadbSegment.MONGODB_KEY_IO1,io1)//数字通道1
                .append(RuiliDatadbSegment.MONGODB_KEY_IO2,io2)//数字通道2
                .append(RuiliDatadbSegment.MONGODB_KEY_DATA_COUNT,data_count)//数据个数
                .append(RuiliDatadbSegment.MONGODB_KEY_DATA_TYPE, dataType)//数据类型
                .append(RuiliDatadbSegment.MONGODB_KEY_TESTNAME,testName)//测试名称
                .append(RuiliDatadbSegment.MONGODB_KEY_RAW_DATA,byteRawData );//原始数据
        //解析数据
        if(dataType.equals(RuiliNodeUdpAttr.ADC_DATA_PACKAGE_STR)) {
            //生成document
            BasicDBObject bdoAdcVal = getAdcVal4CH(msg,(short)(data_count));
            //解析后的ADC数字量
            doc.append(RuiliDatadbSegment.MONGODB_KEY_ADC_VAL,bdoAdcVal);
        }else if(dataType.equals(RuiliNodeUdpAttr.CAN_DATA_PACKAGE_STR)) {
            //TODO:(songchaochao,19-4-9,如果是CAN数据则不解析)
        }

        /*doc存入数据库*/
        //mongodb.insertOne已加锁
        try{
            dataMgd.insertOne(doc, new SingleResultCallback<Void>() {
                public void onResult(final Void result, final Throwable t) {
                    ///用于指示DOC是否成功插入
//			    	logger.info("Document inserted!");
                }});
        }catch(Exception e) {
            logger.error("", e);
        }
    }

    /**
     * 数据包的校验方法
     *
     * @param checkByte1,checkByte2： 两个校验字节，相等才能通过
     * @return true：通过;false:不通过
     */
    private boolean isRightPkg(short checkByte1,short checkByte2){
        if(checkByte1 == checkByte2) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * 获取测试名称
     *
     * @param msg 数据
     * @return String 测试名称
     */
    public static String getFrameHeadTestName(ByteBuf msg) {
        //获取测试名称
        ByteBuf testNameTemp = Unpooled.buffer(RuiliNodeUdpAttr.MAX_TEST_NAME);
        msg.getBytes(RuiliNodeUdpAttr.TEST_NAME_IDX,testNameTemp,RuiliNodeUdpAttr.MAX_TEST_NAME);
        return testNameTemp.toString(CharsetUtil.UTF_8).trim();
    }
    /**
     * 数据包的提取前16bits帧头数据和测试名称
     *
     * YYYY_MM_DD:年月日32bits[0:3], HeadTime:毫秒32bits[4:7], count:数据长度32bits[8:11],
     *
     * nodeId:模组id8bits[12],IO:数字电平[13],dataType:数据类型[14],checkUbyte:校验8bits[15]
     *
     * @param msg
     * @note msg的两个校验字节msg[4]和msg[15]，相等才能通过
     * @return true：成功获取数据帧头;false:数据有问题
     */
    private boolean getFrameHead(ByteBuf msg) {

        //得到帧头+实际数据的Bytebuf字节长度
        BytebufLength = msg.readableBytes();
        //获取设备的id
        nodeId = msg.getUnsignedByte(RuiliNodeUdpAttr.WIFI_CLIENT_ID_IDX);
        //校验设备的id
        if((nodeId<0) ||(nodeId>RuiliNodeUdpAttr.WIFI_CLIENT_ID_MAX)) {
            logger.warn("NodeId Error : Pkg Abandoned!");
            return false;
        }
        //获取headtime/微秒
        headtime = (long)(msg.getUnsignedByte(RuiliNodeUdpAttr.HEADTIME_START_IDX)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.HEADTIME_START_IDX+1)<<8)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.HEADTIME_START_IDX+2)<<16)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.HEADTIME_START_IDX+3)<<24));
        //获取校验byte
        checkUbyte = msg.getUnsignedByte(RuiliNodeUdpAttr.CHECK_UBYTE);
        //校验位校验，headtime的最低8bits需要和帧头校验位相同
        if(!isRightPkg((short)(headtime&0xff),(short)checkUbyte)){
            logger.warn("CheckUbyte Error : Pkg Abandoned");
            return false;
        }
        yyyy_mm_dd = (long)(msg.getUnsignedByte(RuiliNodeUdpAttr.YYYY_MM_DD_START_IDX)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.YYYY_MM_DD_START_IDX+1)<<8)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.YYYY_MM_DD_START_IDX+2)<<16)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.YYYY_MM_DD_START_IDX+3)<<24));
        data_count = (long)(msg.getUnsignedByte(RuiliNodeUdpAttr.DATA_COUNT_START_IDX)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.DATA_COUNT_START_IDX+1)<<8)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.DATA_COUNT_START_IDX+2)<<16)|
                (msg.getUnsignedByte(RuiliNodeUdpAttr.DATA_COUNT_START_IDX+3)<<24));
        //数据个数的校验
        if((data_count<0)||(data_count !=(BytebufLength - RuiliNodeUdpAttr.HEAD_FRAME_LENGTH))) {
            logger.warn("Count Error : Abandoned");
            return false;
        }
        //获取io电平
        io1 = (short) (msg.getUnsignedByte(RuiliNodeUdpAttr.IO_IN_IDX) &  ((short)0x0001));
        io2 = (short) ((msg.getUnsignedByte(RuiliNodeUdpAttr.IO_IN_IDX) &  ((short)0x0002))>>1);
        //获取数据类型，并采取不同的处理措施
        if(RuiliNodeUdpAttr.CAN_DATA_PACKAGE_LABEL == (short) msg.getUnsignedByte(RuiliNodeUdpAttr.DATA_TYPE_IDX)) {
            dataType = RuiliNodeUdpAttr.CAN_DATA_PACKAGE_STR;
        }else if(RuiliNodeUdpAttr.ADC_DATA_PACKAGE_LABEL == (short) msg.getUnsignedByte(RuiliNodeUdpAttr.DATA_TYPE_IDX)) {
            dataType = RuiliNodeUdpAttr.ADC_DATA_PACKAGE_STR;
        }else {
            logger.warn("Data Type Error : Abandoned");
            return false;
        }
        this.testName = getFrameHeadTestName(msg);
        String[] testNamSplitStr = testName.split("/",2);
        //测试名称错误
        if(testNamSplitStr.length < 2) {
            logger.warn("ISO Time in TestName Error : Abandoned");
            return false;
        }else {//测试名称没有错误，包含iso时间
            this.isodate = (testName.split("/",2))[1];
        }
        //今日日期
        this.insertIsodate = TimeUtils.getStrIsoSTime();
        return true;
    }

    /**
     * 数据包的提取,除了前16bits帧头外的adc数据，adc数据以[channel1低八位,channel1高八位,channel2低八位,channel2高八位...]传输
     *
     * channle_num是通道数，adc_count_short是每个通道包含多少个数据（数据12bit，用hsort）
     * ----!!必须在getFrameHead更新后才能调用!!------
     *
     * @param msg 包括帧头在内的所有数据
     * @param adc_count adc数据的byte位数
     * @return BasicDBObject
     */
    private BasicDBObject getAdcVal4CH(ByteBuf msg, int adc_count) {
        int adc_count_short = adc_count/2/RuiliNodeUdpAttr.ADC_CHANNEL_MAX;
        BasicDBList ch1 = new BasicDBList();
        BasicDBList ch2 = new BasicDBList();
        BasicDBList ch3 = new BasicDBList();
        BasicDBList ch4 = new BasicDBList();
        for(int idx = 0,idx_start; idx<adc_count_short ; idx++) {
            idx_start = idx * 8 + RuiliNodeUdpAttr.HEAD_FRAME_LENGTH;
            ch1.add((short)( (msg.getUnsignedByte(idx_start)<<8) |
                    (msg.getUnsignedByte(idx_start + 1)) ));
            ch2.add((short)( (msg.getUnsignedByte(idx_start + 2)<<8) |
                    (msg.getUnsignedByte(idx_start + 3)) ));
            ch3.add((short)( (msg.getUnsignedByte(idx_start + 4)<<8) |
                    (msg.getUnsignedByte(idx_start + 5)) ));
            ch4.add((short)( (msg.getUnsignedByte(idx_start + 6)<<8) |
                    (msg.getUnsignedByte(idx_start + 7)) ));
        }
        return (new BasicDBObject()).append("ch1", ch1).append("ch2", ch2).append("ch3", ch3).append("ch4", ch4);
    }
}
