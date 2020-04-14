package cn.sorl.rcloud.dal.netty;

import lombok.Getter;
import lombok.Setter;

/**
 * +------+-------+-----+-------+-------+-----+-----+------+-------+---------+------------+
 * | Year | Month | Day | Timer | Count | Id  | IO  | Type | Check | TstName |    Data    |
 * +------+-------+-----+-------+------+-----+-----+-------+-------+---------+------------+
 *   0：1     2      3     4:7     8:11   12    13     14     15      16：79    80：...
 */
@Getter
@Setter
public class NodeMsgHead {
    private short year;
    private short month;
    private short day;
    private long timer;
    private long count;
    private short id;
    private short io;
    private short type;
    private short check;
    private String tstName;
    private byte[] data;
}
