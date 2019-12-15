package cn.sorl.rcloud.common.util;

/**
 * 存储属性的一些key
 * @author neyzoter sone
 * @date 2019-12-11
 */
public class PropertyLabel {
    public static final String PROPERTIES_FILE_ADDR = "/etc/rcloud_configuration.properties";

    public static final String MAIL_ENABLE_KEY = "mail.enable";
    public static final String MAIL_ENABLE_YES = "yes";
    public static final String MAIL_ENABLE_NO = "no";
    public static final String MAIL_LIST_KEY = "mail.list";
    public static final String MAIL_LIST_SPLIT = ",";
    public static final String MAIL_PASSWORD_KEY = "mail.password";
    public static final String MAIL_SMTP_HOST_KEY = "mail.smtp.host";
    public static final String MAIL_USER_KEY = "mail.user";
    public static final String MAIL_SMTP_AUTH_KEY = "mail.smtp.auth";


    public static final String DISK_SPACE_MINIMUM_G_KEY = "diskspace.minimumG";
    public static final String DISK_SPACE_SCRIPT_KEY = "diskspace.script";
    public static final String DISK_SPACE_ARGS_KEY = "diskspace.args";
    public static final String DISK_SPACE_WORKSPACE_KEY = "diskspace.workspace";

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
    public static final String DB_Minimum_G_KEY = "db.minimumG";
}
