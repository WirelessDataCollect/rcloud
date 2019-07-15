package cn.sorl.rcloud.dal.netty;

import cn.sorl.rcloud.common.security.SimpleSaltMd5;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

/**
 *
 * 通信通道信息
 *
 * @author  neyzoter
 */
public class RuiliPcChannelAttr {
    //三个状态，请求连接状态、服务器信任状态(已登录状态)、数据实时接收状态
    public final static int REQUEST_CONNECT_STA = 0x01;
    public final static int LOGINED_STA=0x02;
    public final static int DATA_GET_STA = 0x40;
    private final Channel channel;//通道,初始化后不可改变
    private final ChannelHandlerContext context;
    private String encryption;//加密算法
    private String enryptSalt;
    private String testName;
    private Integer status;//通道状态

    /**
     * PC连接服务器时，初始化该通道
     * @param ctx
     */
    public RuiliPcChannelAttr(ChannelHandlerContext ctx){
        this.context = ctx;
        this.channel = ctx.channel();//保存通道信息
        this.status = RuiliPcChannelAttr.REQUEST_CONNECT_STA;//设置为请求连接状态
        this.encryption = "Md5";//保存RSA加密算法信息
        this.enryptSalt = SimpleSaltMd5.getRandStr();//随机初始化salt
        this.testName = "";
    }

    /**
     * 返回该通道的状态
     * @return Integer 通道的状态
     */
    public Integer getStatus() {
        return this.status;
    }
    /**
     * 设置该通道的状态
     * @param sta 通道状态
     */
    public void setStatus(Integer sta) {
        this.status = sta;
    }
    /**
     * 返回该通道的加密算法
     * @return SimpleRsa 加密算法
     */
    public String getEncryption() {
        return this.encryption;
    }
    /**
     * 返回该通道的salt
     * @return String 盐值字符串
     */
    public String getSalt() {
        return this.enryptSalt;
    }
    /**
     * 返回该通道的Channel类
     * @return Channel 通道类
     */
    public Channel getChannel() {
        return this.channel;
    }
    /**
     * 返回该通道的ChannelHandlerContext类
     * @return Channel 通道类
     */
    public ChannelHandlerContext getContext() {
        return this.context;
    }
    /**
     * 返回该通道所做的测试名称
     * @return Channel 通道类
     */
    public String getTestName() {
        return this.testName;
    }
    /**
     * 设置测试名称
     * @param name 测试名称
     * @return None
     */
    public void setTestName(String name) {
        this.testName = name;
    }
}
