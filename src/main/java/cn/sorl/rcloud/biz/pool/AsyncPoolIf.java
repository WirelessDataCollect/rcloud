package cn.sorl.rcloud.biz.pool;

public interface AsyncPoolIf {
    /**
     * 执行异步任务
     * 可以根据需求，自己加参数拟定，我这里就做个测试演示
     */
    void executeAsync();
}
