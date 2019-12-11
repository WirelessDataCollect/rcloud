package cn.sorl.rcloud.common.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

/**
 * shell脚本调用工具
 * @author neyzoter sone
 * @date 2019-12-11
 */
public class ShellCallUtil {
    private static final Logger logger = LoggerFactory.getLogger(ShellCallUtil.class);

    PropertiesUtil propertiesUtil;
    public ShellCallUtil (PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
    }

    /**
     * 调用shell
     * @param script
     * @param args
     * @param workspace
     */
    public String callShell (String script, String args, String... workspace){
        String returnLine = "";
        try {
            String cmd = "./" + script + " " + args;
            File dir = null;
            if(workspace[0] != null){
                dir = new File(workspace[0]);
            }
            String[] evnp = {"val=2", "call=Bash Shell"};
            Process process = Runtime.getRuntime().exec(cmd, evnp, dir);
            BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
            returnLine = input.readLine();
            input.close();
        }
        catch (Exception e){
            e.printStackTrace();
        } finally {
            return returnLine;
        }

    }


    /**
     * 更新属性
     */
    public void updateProps (){
        propertiesUtil.updateProps();
    }
    /**
     * 更新属性
     */
    public void updateProps (String path){
        propertiesUtil.updateProps(path);
    }

    /**
     * 获取属性
     */
    public String getProperty (String key){
        String property = "";
        try {
            property = propertiesUtil.readValue(key);
        } catch (Exception e) {
            logger.error("",e);
        }finally {
            return property;
        }

    }
}
