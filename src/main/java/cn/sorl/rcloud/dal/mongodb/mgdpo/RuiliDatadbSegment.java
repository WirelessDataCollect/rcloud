package cn.sorl.rcloud.dal.mongodb.mgdpo;

public class RuiliDatadbSegment {


    //MongoDB的数据key：本次测试的名称
    public final static String MONGODB_KEY_TESTNAME = "test";
    //MongoDB的数据key：本个设备的名称
    public final static String MONGODB_KEY_NODE_ID = "nodeId";
    //MongoDB的数据key：年月日
    public final static String MONGODB_KEY_YYYYMMDD = "yyyy_mm_dd";
    //MongoDB的数据key：每天的时间精确到1ms
    public final static String MONGODB_KEY_HEADTIME = "headtime";
    //MongoDB的数据的iso时间
    public final static String MONGODB_KEY_ISO_DATE = "isodate";
    //MongoDB的数据的iso时间
    public final static String MONGODB_KEY_INSERT_ISO_DATE = "insertIsodate";
    //MongoDB的数据key：有多少个byte数据
    public final static String MONGODB_KEY_DATA_COUNT = "data_count";
    //MongoDB的数据key：数字通道1
    public final static String MONGODB_KEY_IO1 = "io1";
    //MongoDB的数据key：数字通道2
    public final static String MONGODB_KEY_IO2 = "io2";
    //MongoDB的数据key：数据类型，包括ADC和CAN
    public final static String MONGODB_KEY_DATA_TYPE = "dataType";
    //MongoDB的数据key：adc的数值
    public final static String MONGODB_KEY_ADC_VAL = "adc_val";
    //MongoDB的数据key：原始数据
    public final static String MONGODB_KEY_RAW_DATA = "raw_data";
}
