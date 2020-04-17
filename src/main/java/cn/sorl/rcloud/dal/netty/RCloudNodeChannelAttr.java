package cn.sorl.rcloud.dal.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * 设备通道信息
 *
 * @author  Charles Song
 * @date 2020-4-17
 */
@Getter
@Setter
public class RCloudNodeChannelAttr {
    /**
     * 通道,初始化后不可改变
     */
    private final Channel channel;
    private final ChannelHandlerContext context;

    public RCloudNodeChannelAttr (ChannelHandlerContext ctx) {
        this.context = ctx;
        channel = ctx.channel();
    }
}
