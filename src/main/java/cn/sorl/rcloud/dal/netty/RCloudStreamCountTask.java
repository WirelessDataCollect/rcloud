package cn.sorl.rcloud.dal.netty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP 流统计
 * @author Charles Song
 * @date 2020-4-20
 */
public class RCloudStreamCountTask implements Runnable{
    private static Long skipedByteCount = 0L;
    private static Long usefulByteCount = 0L;
    private static Long lastMs = 0L;
    private static RCloudStreamCountTask rCloudStreamCountTask = new RCloudStreamCountTask();

    private final static Logger logger = LoggerFactory.getLogger(RCloudStreamCountTask.class);

    private RCloudStreamCountTask () {
        lastMs = System.currentTimeMillis();
    }
    @Override
    public void run () {
        try {
            logger.info(String.format("Node-Tcp-Stream : Skiped %d bytes , Used %d bytes (%d ms)",getSkipedByteCout(), getUsefulByteCount(), System.currentTimeMillis() - lastMs));
            lastMs = System.currentTimeMillis();
            resetUsefulByteCount();
            resetSkipedByteCount();
        }catch (Exception e) {
            logger.error("",e);
        }
    }

    /**
     * get Useful byte count
     * @return long
     */
    public static long getUsefulByteCount () {
        return usefulByteCount;
    }
    /**
     * get skiped byte count
     * @return long
     */
    public static long getSkipedByteCout () {
        return skipedByteCount;
    }

    /**
     * set Useful byte count
     * @param count byteCount val
     */
    public synchronized static void setUsefulByteCount (long count) {
        usefulByteCount = count;
    }
    /**
     * set skiped byte count
     * @param count byteCount val
     */
    public synchronized static void setSkipedByteCount (long count) {
        skipedByteCount = count;
    }
    /**
     * increse the Useful byte count
     * @param inc incresement val
     * @return resualt
     */
    public synchronized static long incUsefulByteCount (long inc) {
        usefulByteCount += inc;
        return usefulByteCount;
    }
    /**
     * increse the skiped byte count
     * @param inc incresement val
     * @return resualt
     */
    public synchronized static long incSkipedByteCount (long inc) {
        skipedByteCount += inc;
        return skipedByteCount;
    }
    /**
     * reset Useful byte count
     */
    public synchronized static void resetUsefulByteCount () {
        usefulByteCount = 0L;
    }

    /**
     * reset skiped byte count
     */
    public synchronized static void resetSkipedByteCount () {
        skipedByteCount = 0L;
    }
    /**
     * get singleton Instance
     * @return RCloudStreamCountTask
     */
    public static RCloudStreamCountTask getInstance() {
        return rCloudStreamCountTask;
    }
}
