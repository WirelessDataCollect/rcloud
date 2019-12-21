package cn.sorl.rcloud.common.util;

/**
 * 存储属性的一些key
 * @author neyzoter sone
 * @date 2019-12-11
 */
public class PropertyLabel {
    public static final String PROPERTIES_FILE_ADDR = "/usr/local/rcloud/rcloud_configuration.properties";

    public static final String MAIL_ENABLE_KEY = "mail.enable";
    public static final String MAIL_ENABLE_YES = "yes";
    public static final String MAIL_ENABLE_NO = "no";
    public static final String MAIL_LIST_KEY = "mail.list";
    public static final String MAIL_LIST_SPLIT = ",";
    public static final String MAIL_PASSWORD_KEY = "mail.password";
    public static final String MAIL_SMTP_HOST_KEY = "mail.smtp.host";
    public static final String MAIL_USER_KEY = "mail.user";
    public static final String MAIL_SMTP_AUTH_KEY = "mail.smtp.auth";
    public static final String MAIL_SMTP_PORT_KEY = "mail.smtp.port";


    public static final String SERVER_DISK_MINIMUM_G_KEY = "server.disk.minimumG";
    public static final String SERVER_PROPS_ADDR_KEY = "server.props.addr";
    public static final String SERVER_CPU_MAX_USAGE_KEY = "server.cpu.maxUsage";
    public static final String SERVER_MEM_MAX_USAGE_KEY = "server.mem.maxUsage";
    public static final String SERVER_DIAGNOSIS_EMAIL_TIME_KEY = "server.diagnosis.emailtime";

    public static final String DB_MONGODB_ADMIN_ENABLE_KEY = "db.mongodb.adminEnable";
    public static final String DB_MONGODB_ADMIN_DB_KEY = "db.mongodb.adminDb";
    public static final String DB_MONGODB_ADMIN_ENABLE_YES = "yes";
    public static final String DB_MONGODB_ADMIN_ENABLE_NO = "no";
    public static final String DB_MONGODB_USER_KEY="db.mongodb.user";
    public static final String DB_MONGODB_PASSWORD="db.mongodb.password";
    public static final String DB_MONGODB_ADDR_KEY = "db.mongodb.addr";
    public static final String DB_MONGODB_PORT_KEY = "db.mongodb.port";
    public static final String DB_DISK_MINIMUM_G_KEY = "db.disk.minimumG";
    public static final String DB_CPU_MAX_USAGE_KEY = "db.cpu.maxUsage";
    public static final String DB_MEM_MAX_USAGE_KEY = "db.mem.maxUsage";

    //脚本实时更改的内容
    //此内容存放地址在PropertyLabel.SERVER_PROPS_ADDR_KEY文件中保存
    public static final String SERVER_CPU_USAGE_RAGE_15MIN_KEY ="server.cpu.usagerate.15min";
    public static final String SERVER_CPU_LOGICAL_KERNEL_NUM_KEY="server.cpu.logicalKernel.num";
    public static final String SERVER_DISK_FREE_SPACE_G_KEY="server.disk.freeSpaceG";
    public static final String SERVRE_MEM_USAGE_RATE_KEY="server.mem.usagerate";
}
