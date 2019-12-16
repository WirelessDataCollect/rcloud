package cn.sorl.rcloud.dal.mongodb.mgdpo;

/**
 * data.space的内容
 * @author neyzoter sone
 * @date 2019-12-12
 */
public class RuiliDbStateSegment {
    /**
     * 数据库空闲区域
     */
    public final static String DB_DISK_FREE_SPACE_G_KEY = "db_disk_freeSpaceG";
    public final static String DB_CPU_USAGERATE_15MIN_KEY = "db_cpu_usagerate_15min";
    public final static String DB_CPU_LOGICAL_KERNEL_NUM_KEY = "db_cpu_logicalKernel_num";
    public final static String DB_MEM_USAGE_RATE_KEY= "db_mem_usagerate";
}
