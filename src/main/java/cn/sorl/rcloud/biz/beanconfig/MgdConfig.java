package cn.sorl.rcloud.biz.beanconfig;

import cn.sorl.rcloud.common.time.TimeUtils;
import cn.sorl.rcloud.dal.mongodb.mgdobj.SimpleMgd;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MgdConfig {
    @Bean(name = "dataMgd")
    public SimpleMgd dataMgd(){
        return new SimpleMgd("udp", TimeUtils.getStrIsoMTime(), "test");
    }
    @Bean(name = "infoMgd")
    public SimpleMgd infoMgd(){
        return new SimpleMgd("udp", "config", "test");
    }
    @Bean(name = "generalMgd")
    public SimpleMgd generalMgd(){
        return new SimpleMgd("udp", TimeUtils.getStrIsoMTime(),"");
    }
    @Bean(name = "adminMgd")
    public SimpleMgd adminMgd(){
        return new SimpleMgd("data", "admin", "");
    }
    @Bean(name = "spaceMgd")
    public SimpleMgd spaceMgd(){
        return new SimpleMgd("data", "space", "");
    }
}
