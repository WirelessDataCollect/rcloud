package cn.sorl.rcloud.dal.netty;

/**
 * Data Collection Node Attribution
 * @author Charles Song
 * @date 2020-4-13
 */
public class RCloudNodeAttr {
    /**
     * 一组ADC数据有多少个Byte，共4个通道，每个通道2个bytes
     */
    public static final int LENGTH_ONE_GROUP_ADC = 8;
    /**
     * 一组CAN数据有多少个Byte,1byte用于存储CAN1或者CAN2，4bytes存储偏移时间（相对于帧头内保存的时间）,20bytes存储数据
     */
    public static final int LENGTH_ONE_GROUP_CAN = 25;
    /**
     * 模组id最大不超过
     */
    public static final int  WIFI_CLIENT_ID_MAX= 255;
    /**
     * adc数据通道最多不超过
     */
    public static final byte  ADC_CHANNEL_MAX= 4;
    /**
     * ADC一个周期所占的bytes
     */
    public static final short ADC_BYTES_NUM = 2*ADC_CHANNEL_MAX;
    /**
     * 测试名称长度
     */
    public static final byte MAX_TEST_NAME = 64;
    /**
     * 时间、IO、id、数据类型这些数据的长度
     */
    public static final byte PACKAGE_TIME_IO_LENGTH = 16;
    /**
     * 一帧的头的长度，包括测试名称、yyyy_mm_dd、headtime、data_count等
     */
    public static final byte HEAD_FRAME_LENGTH = MAX_TEST_NAME + PACKAGE_TIME_IO_LENGTH;
    /**
     * 校验开始的位置，和headtime的低八位相等
     */
    public static final short CHECK_UBYTE = 15;
    /**
     * 年月日开始的下标,下标越大，越高位
     */
    public static final int YYYY_MM_DD_START_IDX = 0;
    /**
     * 毫秒开始的下标
     */
    public static final int HEADTIME_START_IDX = 4;
    /**
     * 数据量大小开始的下标
     */
    public static final int DATA_COUNT_START_IDX = 8;
    //
    /**
     * 这里保存wifi模组的id的下标
     */
    public static final int WIFI_CLIENT_ID_IDX = 12;
    /**
     * IO开始的地址，1byte最多可保存8个IO数据
     */
    public static final int IO_IN_IDX = 13;
    /**
     * 数据类型，包括CAN数据和ADC数据
     */
    public static final int DATA_TYPE_IDX = 14;
    /**
     * CAN数据标记
     */
    public static final short CAN_DATA_PACKAGE_LABEL = 1;
    /**
     * ADC数据标记
     */
    public static final short ADC_DATA_PACKAGE_LABEL = 2;
    /**
     * CAN数据String标记
     */
    public static final String CAN_DATA_PACKAGE_STR = "CAN";
    /**
     * ADC数据String标记
     */
    public static final String ADC_DATA_PACKAGE_STR = "ADC";

    public static final String DATA_PACKAGE_TYPE_NOT_CLEAR_STR = "DEFAULT";
    /**
     * 测试名称紧接着time io等
     */
    public final static int TEST_NAME_IDX = PACKAGE_TIME_IO_LENGTH;
}
