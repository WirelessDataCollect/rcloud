package cn.sorl.rcloud.common.diagnosis;

import com.sun.management.OperatingSystemMXBean;

import java.lang.management.ManagementFactory;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 服务器管理器
 * @author nezoter sone
 * @date 2019-12-16
 */
public class ServerMonitor {
    private double cpuUsageRate;
    private double memUsageRate;
    private Queue<Double> cpuUsageRateQueue;
    private int cpuUsageRateQueueLen;
    private int memUsageRateQueueLen;
    private Queue<Double> memUsageRateQueue;
    public ServerMonitor (int cpuCheckQueLen, int memCheckQueLen) {
        this.cpuUsageRate = this.getCpuUsageRate();
        this.memUsageRate = this.getMemUsageRate();

        this.cpuUsageRateQueueLen = cpuCheckQueLen;
        cpuUsageRateQueue =  new LinkedBlockingDeque<>( this.cpuUsageRateQueueLen);
        for (int i = 0 ; i < cpuCheckQueLen ; i ++) {
            cpuUsageRateQueue.add(this.cpuUsageRate);
        }

        this.memUsageRateQueueLen = memCheckQueLen;
        memUsageRateQueue = new LinkedBlockingDeque<>(this.memUsageRateQueueLen);
        for (int i = 0 ; i < memCheckQueLen ; i ++) {
            memUsageRateQueue.add(this.memUsageRate);
        }
    }

    /**
     * 获取CPU利用率
     * @return
     */
    public double getCpuUsageRate () {
        double cpuLoad = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getSystemCpuLoad();
        return cpuLoad;
    }

    /**
     * 获取物理内存空间利用率
     * @return
     */
    public double getMemUsageRate () {
        double memAll = ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getTotalPhysicalMemorySize();
        double memLoad = memAll - ((OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean()).getFreePhysicalMemorySize();
        return (memLoad/memAll);
    }

    /**
     * 更新cpu占用率队列
     */
    public void updateCpuUsageRateQueue () {
        this.cpuUsageRateQueue.remove();
        this.cpuUsageRateQueue.add(this.getCpuUsageRate());
    }

    /**
     * 更新内存占用率队列
     */
    public void updateMemUsageRateQueue () {
        this.memUsageRateQueue.remove();
        this.memUsageRateQueue.add(this.getCpuUsageRate());
    }
    /**
     * 获取平均内存使用率
     * @return
     */
    public double getMemUsageRateAverage () {
        Iterator<Double> iter = this.memUsageRateQueue.iterator();
        double sum = 0;
        int num = 0;
        while (iter.hasNext()) {
            sum += iter.next();
            num += 1;
        }
        if (num != 0) {
            return (sum / num);
        }else {
            return this.getMemUsageRate();
        }

    }

    /**
     * 获取平均内存使用率
     * @return
     */
    public double getCpuUsageRateAverage () {
        Iterator<Double> iter = this.cpuUsageRateQueue.iterator();
        double sum = 0;
        int num = 0;
        while (iter.hasNext()) {
            sum += iter.next();
            num += 1;
        }
        if (num != 0) {
            return (sum / num);
        }else {
            return this.getCpuUsageRate();
        }

    }

}
