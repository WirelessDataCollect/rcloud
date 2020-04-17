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
 * +------+-------+-----+-------+-------+-----+-----+------+-------+---------+------------+
 * | Year | Month | Day | Timer | Count | Id  | IO  | Type | Check | TstName |    Data    |
 * +------+-------+-----+-------+------+-----+-----+-------+-------+---------+------------+
 *   0：1     2      3     4:7     8:11   12    13     14     15      16：79    80：...
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
    @Override
    protected void decode (ChannelHandlerContext ctx, ByteBuf buffer,
                           List<Object> out) throws Exception {
        SimpleDateFormat sdfYYYY = new SimpleDateFormat("yyyy");
        SimpleDateFormat sdfMM = new SimpleDateFormat("MM");
        SimpleDateFormat sdfDD = new SimpleDateFormat("dd");

        Date date = new Date();
        int yymd = Integer.parseInt(sdfYYYY.format(date)) << 16 +
                Integer.parseInt(sdfMM.format(date)) << 8+
                Integer.parseInt(sdfDD.format(date));
        // 基本长度
        if (buffer.readableBytes() > RCloudNodeAttr.HEAD_FRAME_LENGTH) {
            // 记录包头开始的index
            int beginReader;
            while (true) {
                // 获取包头开始的index
                beginReader = buffer.readerIndex();
                // 标记包头开始的index
                buffer.markReaderIndex();
                // 协议开始
                if (buffer.readInt() == yymd) {
                    break;
                }
                // 未读到包头，略过一个字节
                // 每次略过，一个字节，去读取，包头信息的开始标记
                buffer.resetReaderIndex();
                buffer.readByte();

                // 当略过，一个字节之后，
                // 数据包的长度，又变得不满足
                // 此时，应该结束。等待后面的数据到达
                if (buffer.readableBytes() < RCloudNodeAttr.HEAD_FRAME_LENGTH) {
                    return;
                }
            }
            // 时间
            long timer = buffer.readUnsignedInt();
            // 数据个数
            int count = buffer.readInt();
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
            String testName = String.copyValueOf(nameChar);
            // 判断请求数据包数据是否到齐
            if (buffer.readableBytes() < count) {
                // 还原读指针
                buffer.readerIndex(beginReader);
                return;
            }

            // 读取data数据
            byte[] data = new byte[count];
            buffer.readBytes(data);
            // 重置
            buffer.resetReaderIndex();
            // 获取raw data
            byte[] rawData = new byte[count + RCloudNodeAttr.HEAD_FRAME_LENGTH];
            buffer.readBytes(data);

            NodeMsg nodeMsg = new NodeMsg(yymd, timer, count, id, io, type, check, testName, data, rawData);
            out.add(nodeMsg);
        }
    }
}