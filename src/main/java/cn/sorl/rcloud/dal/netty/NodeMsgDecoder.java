package cn.sorl.rcloud.dal.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 设备数据协议<br/>
 * <pre>
 * 数据包格式
 *
 * 大端模式
 * +------+-------+-----+-------+-------+-----+-----+------+-------+---------+------------+
 * | Year | Month | Day | Timer | Count | Id  | IO  | Type | Check | TstName |    Data    |
 * +------+-------+-----+-------+------+-----+-----+-------+-------+---------+------------+
 *   0：1     2      3     4:7     8:11   12    13     14     15      16：79    80：...
 *
 * count= 0 : 00 00 00 00 10 10 10 10 00 00 00 00 01 02 02 10 44 65 66 75 61 6c 74 54 65 73 74 2f 32 30 32 30 3a 30 34 3a 30 34 54 31 32 3a 30 30 3a 30 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
 *
 * count = 96 : 00 00 00 01 00 00 10 10 00 00 00 60 01 02 02 10 44 65 66 75 61 6c 74 54 65 73 74 2f 32 30 32 30 3a 30 34 3a 30 34 54 31 32 3a 30 30 3a 30 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32
 *
 * 小端模式(采用的方案)
 *  +------+-------+-----+-------+-------+-----+-----+------+-------+---------+------------+
 *  | Day  | Month | Year| Timer | Count | Id  | IO  | Type | Check | TstName |    Data    |
 *  +------+-------+-----+-------+------+-----+-----+-------+-------+---------+------------+
 *    0      1      2:3     4:7     8:11   12    13     14     15      16：79    80：...
 *
 *  count = 96 : 01 00 00 00 10 10 00 00 60 00 00 00 01 02 02 10 44 65 66 75 61 6c 74 54 65 73 74 2f 32 30 32 30 3a 30 34 3a 30 34 54 31 32 3a 30 30 3a 30 31 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32 12 13 10 10 12 12 32 32
 *
 * TO：传输开始，字符
 * Year：年
 * Month：月
 * Day：日
 * Timer：100us计数器，第一个数据的时间,UINT32，小端模式
 * Count：Data数据字节长度,UINT32，小端模式
 * Id：设备Id，0-255
 * IO：IO电平，bit0为IO1，bit1为IO2
 * Type：数据类型，1表示CAN，2表示ADC
 * Check：校验字节，等于Byte4
 * TstName：实验名称
 * Data：数据，具体格式由Type决定，即ADC和CAN两种。ADC单个数据使用8字节保存，CAN单个数据使用25字节保存。
 *       一个数据包可以包括多个数据连续存储。
 *
 * ADC数据格式
 * +-----+-----+-----+-----+-----+-----+
 * | CH1 | CH2 | CH3 | CH4 | CH1 | ... |
 * +-----+-----+-----+-----+-----+-----+
 *   0:1   2:3   4:5   6:7   8:9
 * ADC数据格式以8字节为单位，其中包括4个通道数据CH1-4,每个通道占用2个字节。每个通道的采样时间间隔为1ms。
 *
 * CAN数据格式
 * +------+-------+-------+-------+---- -+-----+-----+-------+-------+------+------+
 * | CANn | Timer | StdId | ExtId | IDE  | RTR | DLC | Data0 | Data1 | ...  | FIM  |
 * +------+-------+-------+-------+-----+-----+-----+--------+-------+------+------+
 *    0      1:4     5:8     9:12   13    14    15     16       17             24
 * </pre>
 * @author Charles Song
 * @date 2020-4-14
 */
public class NodeMsgDecoder extends ByteToMessageDecoder {
    private static final Logger logger = LoggerFactory.getLogger(NodeMsgDecoder.class);
    public static final boolean BIG_ENDIAN = false;
    @Override
    protected void decode (ChannelHandlerContext ctx, ByteBuf buffer,
                           List<Object> out) throws Exception {
        // 基本长度
        if (buffer.readableBytes() > RCloudNodeAttr.HEAD_FRAME_LENGTH) {
            int yymd = genYymd();
//            int yymd = 1;
            // 记录包头开始的index
            int beginReader;
            while (true) {
                // 获取包头开始的index
                beginReader = buffer.readerIndex();
                // 标记包头开始的index
                buffer.markReaderIndex();
                // 协议开始
                int yymdBuff;
                // 大小端模式选择
                if (BIG_ENDIAN) {
                    yymdBuff = buffer.readInt();
                } else {
                    yymdBuff = buffer.readIntLE();
                }
                // 相差不超过1天
                if (yymdBuff - yymd <= 1) {
                    break;
                }
                // 未读到包头，略过一个字节
                // 每次略过，一个字节，去读取，包头信息的开始标记
                buffer.resetReaderIndex();
                buffer.readByte();
                // 计数器加1, 加到跳过的byte count中
                RCloudStreamCountTask.incSkipedByteCount(1);

                // 当略过，一个字节之后，
                // 数据包的长度，又变得不满足
                // 此时，应该结束。等待后面的数据到达
                if (buffer.readableBytes() < RCloudNodeAttr.HEAD_FRAME_LENGTH - RCloudNodeAttr.HEADTIME_START_IDX) {
                    return;
                }
            }
            // 时间
            int timer;
            if (BIG_ENDIAN) {
                timer = buffer.readInt();
            } else {
                timer = buffer.readIntLE();
            }

            // 数据个数
            int count;
            if (BIG_ENDIAN) {
                count = buffer.readInt();
            } else {
                count = buffer.readIntLE();
            }
            // node id
            short id = buffer.readUnsignedByte();
            // io电平
            short io = buffer.readUnsignedByte();
            // 数据类型，ADC或者CAN
            short type = buffer.readUnsignedByte();
            // 检查
            short check = buffer.readUnsignedByte();
            short byte0Mask = 0xff;
            if (check != (short)(timer & byte0Mask)) {
                logger.info("Pkg Abandoned : check byte not correct!");
                return;
            }
            // 测试名称
            char[] nameChar = new char[RCloudNodeAttr.MAX_TEST_NAME];
            for (int i = 0 ; i < RCloudNodeAttr.MAX_TEST_NAME; i ++) {
                nameChar[i] = (char) buffer.readByte();
            }
            String testName = String.copyValueOf(nameChar).trim();
            // 判断请求数据包数据是否到齐
            if (buffer.readableBytes() < count) {
                // 还原读指针
                buffer.readerIndex(beginReader);
                return;
            }

            // 读取data数据
            byte[] data = new byte[count];
            buffer.readBytes(data);
            // 重置为开始的位置
            buffer.readerIndex(beginReader);
            // 获取raw data
            int totalNum = count + RCloudNodeAttr.HEAD_FRAME_LENGTH;
            byte[] rawData = new byte[totalNum];
            buffer.readBytes(rawData);

            // inc the byte count
            RCloudStreamCountTask.incUsefulByteCount(totalNum);

            NodeMsg nodeMsg = new NodeMsg(yymd, timer, count, id, io, type, check, testName, data, rawData);
            out.add(nodeMsg);
        }
    }

    public static int genYymd () {
        SimpleDateFormat sdfYy = new SimpleDateFormat("yyyy");
        SimpleDateFormat sdfM = new SimpleDateFormat("MM");
        SimpleDateFormat sdfD = new SimpleDateFormat("dd");

        Date date = new Date();
        int yymd;
        yymd = Integer.parseInt(sdfYy.format(date)) << 16 |
                Integer.parseInt(sdfM.format(date)) << 8 |
                Integer.parseInt(sdfD.format(date));
        return yymd;
    }
}