package cn.sorl.rcloud.dal.netty;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * <pre>
 * +------+-------+-----+-------+-------+-----+-----+------+-------+---------+------------+
 * | Year | Month | Day | Timer | Count | Id  | IO  | Type | Check | TstName |    Data    |
 * +------+-------+-----+-------+------+-----+-----+-------+-------+---------+------------+
 *   0：1     2      3     4:7     8:11   12    13     14     15      16：79    80：...
 * </pre>
 * @author Charles Song
 * @date 2020-4-17
 */
@Getter
@Setter
public class NodeMsg implements Serializable {
    private static final long serialVersionUID = -4186625114888181635L;
    private long yymd;
    private long timer;
    private int count;
    private short id;
    private short io;
    private String type;
    private short check;
    private String tstName;
    private String isotime;
    private byte[] data;
    private byte[] byteRawData;

    public NodeMsg(long yymd, long timer, int count, short id, short io, short type, short check, String testName, byte[] data, byte[] rawData) {
        this.setYymd(yymd);
        this.setTimer(timer);
        this.setCount(count);
        this.setId(id);
        this.setIo(io);
        if (RCloudNodeAttr.CAN_DATA_PACKAGE_LABEL == type) {
            this.setType(RCloudNodeAttr.CAN_DATA_PACKAGE_STR);
        } else if (RCloudNodeAttr.ADC_DATA_PACKAGE_LABEL == type){
            this.setType(RCloudNodeAttr.ADC_DATA_PACKAGE_STR);
        } else {
            this.setType(RCloudNodeAttr.DATA_PACKAGE_TYPE_NOT_CLEAR_STR);
        }
        this.setCheck(check);
        this.setTstName(testName);
        this.setData(data);
        this.setByteRawData(rawData);
        this.setIsotime((testName.split("[/_]",2))[1]);
    }

    @Override
    public String toString() {
        String str ;
        str = String.format("yymd : %d, timer : %d, count : %d, id : %d, io : %d, type : %s, check : %d, tstName : %s, isotime : %s",
                yymd,timer, count, id, io, type, check, tstName, isotime);
        return str;
    }
}
