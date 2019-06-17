package cn.sorl.rcloud.web.controller;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.biz.pool.AsyncPoolIf;
import cn.sorl.rcloud.biz.pool.AsyncPoolImpl;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import com.mongodb.Block;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.async.client.FindIterable;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.Future;

@RestController
public class NodeDataController {
    Logger logger = LoggerFactory.getLogger(NodeDataController.class);
    @Autowired
    AsyncPoolIf asyncPoolIf;
    @RequestMapping("/getUser")
    public String getUser(){
        Future<String> future = asyncPoolIf.getUserStrAsync();
        while(!future.isDone()){
            //等待
        }
        try{
            return future.get();
        }catch (Exception e){
            logger.info("",e);
            return "ERROR";
        }
    }
}
