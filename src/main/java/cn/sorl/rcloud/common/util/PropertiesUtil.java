package cn.sorl.rcloud.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * 读取文件中的参数
 * @author neyzoter song
 * @daa 2019-12-11
 */
public class PropertiesUtil {
    private static final Logger logger = LoggerFactory.getLogger(PropertiesUtil.class);
    /**
     * 属性
     */
    private Properties props=null;

    private String path = PropertyLabel.PROPERTIES_FILE_ADDR;

    /**
     * 装载属性文件
     * @throws IOException
     */
    public PropertiesUtil(String path){
            this.updateProps(path);
            logger.info(props.toString());
    }

    /**
     * 读取属性
     * @param key
     * @return
     * @throws IOException
     */
    public String readValue(String key) {
        return  props.getProperty(key);
    }

    /**
     * 更新属性
     * @param path
     */
    public void updateProps (String path) {
        this.path = path;
        this.updateProps();

    }
    /**
     * 获取属性
     * @return
     */
    public void updateProps (){
        try{
            BufferedReader bufferedReader = new BufferedReader(new FileReader(this.path));
            props = new Properties();
            props.load(bufferedReader);
        }catch (Exception e) {
            logger.error("",e);
        }
    }
    /**
     * 获取props
     * @return
     */
    public Properties getProps () {
        return props;
    }

}
