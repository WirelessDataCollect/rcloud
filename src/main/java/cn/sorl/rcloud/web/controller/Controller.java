package cn.sorl.rcloud.web.controller;

import cn.sorl.rcloud.biz.beanconfig.BeanContext;
import cn.sorl.rcloud.biz.pool.AsyncPoolIf;
import cn.sorl.rcloud.common.time.TimeUtils;
import cn.sorl.rcloud.common.util.PropertiesUtil;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import cn.sorl.rcloud.dal.mongodb.mgdpo.RuiliAdmindbSegment;
import com.mongodb.BasicDBObject;
import com.mongodb.async.SingleResultCallback;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.Future;

//支持CROS
@CrossOrigin
@RestController
public class Controller {
    Logger logger = LoggerFactory.getLogger(Controller.class);
    @Autowired
    AsyncPoolIf asyncPoolIf;
    @RequestMapping("/api/v1/database/getUser")
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
    @RequestMapping(value = "/api/v1/database/addUser", method = RequestMethod.POST)
    public String addUser(@RequestParam(value=RuiliAdmindbSegment.MONGODB_USER_NAME_KEY) String userName,
                          @RequestParam(value=RuiliAdmindbSegment.MONGODB_USER_KEY_KEY) String userKey,
                          @RequestParam(value=RuiliAdmindbSegment.MONGODB_USER_PRIORTY) String userPrivilege){
        SimpleMgd adminMgd = BeanContext.context.getBean("adminMgd", SimpleMgd.class);
        Document admin = new Document(RuiliAdmindbSegment.MONGODB_USER_NAME_KEY, userName)
                .append(RuiliAdmindbSegment.MONGODB_USER_KEY_KEY, userKey)
                .append(RuiliAdmindbSegment.MONGODB_USER_PRIORTY, userPrivilege)
                .append(RuiliAdmindbSegment.MONGODB_USER_CREATE_TIME, TimeUtils.getStrIsoSTime());
        adminMgd.collection.insertOne(admin, new SingleResultCallback<Void>() {
            @Override
            public void onResult(Void aVoid, Throwable throwable) {
                logger.info(String.format("Inserted One User(%s)", userName));
            }
        });
        return "OK";
    }
    @RequestMapping(value = "/api/v1/database/delete_user_name",method = RequestMethod.DELETE)
    public String deleteUser(@RequestParam(value="userName") String userName) {
        SimpleMgd adminMgd = BeanContext.context.getBean("adminMgd", SimpleMgd.class);
        adminMgd.collection.deleteOne(new BasicDBObject(RuiliAdmindbSegment.MONGODB_USER_NAME_KEY,userName),new SingleResultCallback<DeleteResult>() {
            @Override
            public void onResult(DeleteResult result, Throwable throwable) {
                if(result.wasAcknowledged()){
                    logger.info("Deleted User(s)");
                }else{
                    logger.info("Deleted unAcknowledged");
                }
            }
        });
        return userName;
    }
}
