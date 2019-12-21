package cn.sorl.rcloud.dal.network;

import cn.sorl.rcloud.common.util.PropertiesUtil;
import cn.sorl.rcloud.common.util.PropertyLabel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
/**
 * emial sender
 * @auther scc
 * @date 2019-12-10
 */
public class EmailSender {
    private static final Logger logger = LoggerFactory.getLogger(EmailSender.class);
    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    private PropertiesUtil propertiesUtil;
    Properties props = new Properties();

    public EmailSender (PropertiesUtil propertiesUtil) {
        this.propertiesUtil = propertiesUtil;
        this.updateProps(this.propertiesUtil);
    }
    /**
     * 更新Props
     */
    public void updateProps(PropertiesUtil propertiesUtil) {
        try {
            props.clear();
            // 重新读取文件，更新属性
            propertiesUtil.updateProps();
            // 表示SMTP发送邮件，需要进行身份验证
            props.put(PropertyLabel.MAIL_SMTP_AUTH_KEY, propertiesUtil.readValue(PropertyLabel.MAIL_SMTP_AUTH_KEY));
            props.put(PropertyLabel.MAIL_SMTP_HOST_KEY,propertiesUtil.readValue(PropertyLabel.MAIL_SMTP_HOST_KEY));
            props.put(PropertyLabel.MAIL_USER_KEY,propertiesUtil.readValue(PropertyLabel.MAIL_USER_KEY));
            props.put(PropertyLabel.MAIL_PASSWORD_KEY,propertiesUtil.readValue(PropertyLabel.MAIL_PASSWORD_KEY));
            props.put(PropertyLabel.MAIL_SMTP_PORT_KEY,propertiesUtil.readValue(PropertyLabel.MAIL_SMTP_PORT_KEY));
            props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
            props.put("mail.smtp.socketFactory.fallback", "false");
        } catch (Exception e) {
            logger.error("",e);
        }

    }
    /**
     *
     * @param mailAdr 收件人
     * @param subject 邮件标题
     * @param content 邮件内容
     */
    public void sendMail(String mailAdr,String subject,String content){
        try{

            // 构建授权信息，用于进行SMTP进行身份验证
            Authenticator authenticator = new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    // 用户名、密码
                    String userName = props.getProperty(PropertyLabel.MAIL_USER_KEY);
                    String password = props.getProperty(PropertyLabel.MAIL_PASSWORD_KEY);
                    return new PasswordAuthentication(userName, password);
                }
            };
            // 使用环境属性和授权信息，创建邮件会话
            Session mailSession = Session.getInstance(props, authenticator);
            // 创建邮件消息
            MimeMessage message = new MimeMessage(mailSession);
            InternetAddress from = new InternetAddress(
                    props.getProperty(PropertyLabel.MAIL_USER_KEY));
            message.setFrom(from);

            // 设置收件人
            InternetAddress to = new InternetAddress(mailAdr);
            message.setRecipient(RecipientType.TO, to);

            // 设置抄送
            //InternetAddress cc = new InternetAddress("luo_aaaaa@yeah.net");
            //message.setRecipient(RecipientType.CC, cc);

            // 设置密送，其他的收件人不能看到密送的邮件地址
            //InternetAddress bcc = new InternetAddress("aaaaa@163.com");
            //message.setRecipient(RecipientType.CC, bcc);

            // 设置邮件标题
            message.setSubject(subject);
            // 设置邮件的内容体
            message.setContent(content, "text/html;charset=UTF-8");
            // 发送邮件
            Transport.send(message);
        }catch (Exception e) {
            logger.error("",e);
        }
    }
}
