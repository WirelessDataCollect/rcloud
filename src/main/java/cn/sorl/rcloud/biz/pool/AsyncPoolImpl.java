package cn.sorl.rcloud.biz.pool;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliAdmindbSegment;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliInfodbSegment;
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.FindIterable;
import com.mongodb.client.MongoCursor;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class AsyncPoolImpl implements AsyncPoolIf{
    private static final Logger logger = LoggerFactory.getLogger(AsyncPoolImpl.class);
    String UserList = "";
    Integer UserNum = 0;
    private Lock lock = new ReentrantLock();
    private Condition cond = lock.newCondition();
    @Override
    @Async("asyncExecutor")
    public Future<String> getUserStrAsync(){
        logger.info("Start getUserStrAsync");
        SimpleMgd adminMgd = BeanContext.context.getBean("adminMgd", SimpleMgd.class);
        FindIterable<Document> findIter = adminMgd.collection.find()
                .projection(new BasicDBObject().append(RuiliAdmindbSegment.MONGODB_USER_NAME_KEY, 1).append("_id", 0));
        UserList = "";
        //需要lock
        lock.lock();
        try{
            synchronized (UserList) {
                synchronized (UserNum) {
                    findIter.forEach(new Block<Document>() {
                        @Override
                        public void apply(Document document) {
                            UserList += document.toJson();
                        }
                    }, new SingleResultCallback<Void>() {
                        @Override
                        public void onResult(Void aVoid, Throwable throwable) {
                            lock.lock();
                            try{
                                cond.signalAll();
                            }catch (Exception e){
                                logger.info("",e);
                            }finally {
                                lock.unlock();
                            }


                        }
                    });
                }
            }
            cond.await(1, TimeUnit.SECONDS);
        }catch (Exception e){
            logger.info("",e);
        }finally {
            lock.unlock();
        }
        logger.info("End getUserStrAsync");
        return new AsyncResult<>(UserList);
    }
}
