package cn.sorl.rcloud.web.controller;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.FindIterable;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NodeDataController {
    Logger logger = LoggerFactory.getLogger(NodeDataController.class);
    String UserList = "";
    Integer UserNum = 0;
    private final Object nodeDataControllerLock = new Object();
    @RequestMapping("/getUser")
    public String getUser(){
        SimpleMgd infoMgd = BeanContext.context.getBean("infoMgd", SimpleMgd.class);
        FindIterable<Document> findIter = infoMgd.collection.find();
        synchronized (UserList) {
            synchronized (UserNum) {
                findIter.forEach(new Block<Document>() {
                    @Override
                    public void apply(Document document) {
                        UserList += document.toJson() + "\n";
                    }
                }, new SingleResultCallback<Void>() {
                    private final Object callBackLock = new Object();
                    @Override
                    public void onResult(Void aVoid, Throwable throwable) {
                        synchronized (callBackLock) {
                            callBackLock.notifyAll();
                        }
                    }
                });
            }
        }
//        synchronized (nodeDataControllerLock){
//            try{
//                nodeDataControllerLock.wait();
//            }catch(Exception e){
//                logger.info("",e);
//            }
//        }
        return "GET IN";
    }
}
