package cn.sorl.rcloud.common.util;

/**
 * 存储属性的一些key
 * @author neyzoter sone
 * @date 2019-12-11
 */
public class PropertyLabel {
    /**
     * properties文件存放位置
     */
    public static final String PROPERTIES_FILE_ADDR = "/usr/local/rcloud/rcloud_configuration.properties";
    /**
     * 是否开启邮件提醒
     */
    public static final String MAIL_ENABLE_KEY = "mail.enable";
    /**
     * 邮箱开始提醒
     */
    public static final String MAIL_ENABLE_YES = "yes";
    /**
     * 邮箱不开启提醒
     */
    public static final String MAIL_ENABLE_NO = "no";
    /**
     * 管理员列表
     */
    public static final String MAIL_LIST_KEY = "mail.list";
    /**
     * 管理员列表分割符
     */
    public static final String MAIL_LIST_SPLIT = ",";
    /**
     * 邮箱密码
     */
    public static final String MAIL_PASSWORD_KEY = "mail.password";
    /**
     * smtp服务器地址
     */
    public static final String MAIL_SMTP_HOST_KEY = "mail.smtp.host";
    /**
     * 发送消息的邮箱
     */
    public static final String MAIL_USER_KEY = "mail.user";
    /**
     * 开启smtp验证
     */
    public static final String MAIL_SMTP_AUTH_KEY = "mail.smtp.auth";
    /**
     * smtp服务器的端口
     */
    public static final String MAIL_SMTP_PORT_KEY = "mail.smtp.port";

    /**
     * 服务器剩余磁盘空间阈值
     */
    public static final String SERVER_DISK_MINIMUM_G_KEY = "server.disk.minimumG";
    /**
     * 服务器查询结果（内存、磁盘、CPU）存放地址
     */
    public static final String SERVER_PROPS_ADDR_KEY = "server.props.addr";
    /**
     * CPU使用率阈值
     */
    public static final String SERVER_CPU_MAX_USAGE_KEY = "server.cpu.maxUsage";
    /**
     * 内存使用率阈值
     */
    public static final String SERVER_MEM_MAX_USAGE_KEY = "server.mem.maxUsage";
    /**
     * 检测到故障发送邮件的间隔
     */
    public static final String SERVER_DIAGNOSIS_EMAIL_TIME_KEY = "server.diagnosis.emailtime";

    /**
     * mongodb是否开启用户验证
     */
    public static final String DB_MONGODB_ADMIN_ENABLE_KEY = "db.mongodb.adminEnable";
    /**
     * mongodb的验证数据库
     */
    public static final String DB_MONGODB_ADMIN_DB_KEY = "db.mongodb.adminDb";
    /**
     * mongodb开启验证
     */
    public static final String DB_MONGODB_ADMIN_ENABLE_YES = "yes";
    /**
     * mongodb不开启验证
     */
    public static final String DB_MONGODB_ADMIN_ENABLE_NO = "no";
    /**
     * mongodb的用户名
     */
    public static final String DB_MONGODB_USER_KEY="db.mongodb.user";
    /**
     * mongodb用户密码
     */
    public static final String DB_MONGODB_PASSWORD="db.mongodb.password";
    /**
     * mongodb访问地址
     */
    public static final String DB_MONGODB_ADDR_KEY = "db.mongodb.addr";
    /**
     * mongodb端口
     */
    public static final String DB_MONGODB_PORT_KEY = "db.mongodb.port";
    /**
     * 数据库服务器磁盘空间阈值
     */
    public static final String DB_DISK_MINIMUM_G_KEY = "db.disk.minimumG";
    /**
     * 数据库服务器CPU使用率阈值
     */
    public static final String DB_CPU_MAX_USAGE_KEY = "db.cpu.maxUsage";
    /**
     * 数据库服务器内存使用率阈值
     */
    public static final String DB_MEM_MAX_USAGE_KEY = "db.mem.maxUsage";

    /**
     * 应用服务器脚本查询结果1，CPU使用率
     */
    public static final String SERVER_CPU_USAGE_RAGE_15MIN_KEY ="server.cpu.usagerate.15min";
    /**
     * 应用服务器脚本查询结果2，逻辑核个数
     */
    public static final String SERVER_CPU_LOGICAL_KERNEL_NUM_KEY="server.cpu.logicalKernel.num";
    /**
     * 应用服务器脚本查询结果3，磁盘剩余空间
     */
    public static final String SERVER_DISK_FREE_SPACE_G_KEY="server.disk.freeSpaceG";
    /**
     * 应用服务器脚本查询结果4，内存使用率
     */
    public static final String SERVER_MEM_USAGE_RATE_KEY="server.mem.usagerate";
    /**
     * 应用服务器连接设备的协议，包括TCP和UDP
     */
    public static final String SERVER_NODE_PORT_PROTOCAL="server.node.proto";
    /**
     * 应用服务器连接设备的最多个数
     */
    public static final String SERVER_NODE_CONNECT_NUM="server.node.maxNum";
    /**
     * 应用服务器连接客户端的最多个数
     */
    public static final String SERVER_CLIENT_CONNECT_NUM="server.client.maxNum";
}
