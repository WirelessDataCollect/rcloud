package cn.sorl.rcloud.dal.netty;

public class RuiliPcCmdAttr {
    //优先级高
    public final static String PC_START_ONE_TEST = "StartTest";
    public final static String MONGODB_FIND_DOCS = "MongoFindDocs";//获取mongodb中的集合名称
    public final static String MONGODB_FIND_DOCS_NAMES = "MongoFindDocsNames";//获取mongodb中的集合名称
    //中等优先级
    public final static String PC_WANT_LOGIN = "Login";//登录指令
    public final static String PC_WANT_GET_TEST_CONFIG = "GetTestConfig";//获取测试配置文件
    public final static String PC_WANT_GET_RTDATA = "GetRtdata";//获取实时数据，必须先login（进入信任区）
    public final static String PC_STOP_GET_RTDATA = "StopGetRtdata";//停止获取实时数据
    public final static String MONGODB_CREATE_COL = "MongoCreateCol";//创建一个数据集合，每次实验都要创建
    //优先级低
    public final static String PC_WANT_DISCONNECT = "Disconnect";//断开连接
    public final static String HEART_BEAT_SIGNAL = "HeartBeat";//心跳包

    //用于分割消息的字符
    public final static String SEG_CMD_INFO = "\\+";//分割命令和信息
    public final static String SEG_INFO1_INFON = ";";//分割多个子信息
    public final static String SEG_KEY_VALUE = ":";//分割key和calue
    public final static String SEG_LOWER_UPPER_BOUND = ",";//分割value的上下界
    public final static String SEG_LIST_BOUND = ",";//分割value的列表，如dataType:CAN,ADC
    public final static String SEG_TOW_PACK = "\n";
    //	public static Md5 md5 = (Md5) App.getApplicationContext().getBean("md5");
    //给某个命令的返回信息
    public final static String DONE_SIGNAL_OK = "OK";//成功
    public final static String DONE_SIGNAL_OVER = "OVER";//结束，一般用于，数据发送
    public final static String DONE_SIGNAL_ERROR = "ERROR";//失败
    public final static String SEG_CMD_DONE_SIGNAL = SEG_KEY_VALUE;//分割Key:Value,如Login:OK，登录成功。如MongoFindDocs:rllllaw

}
