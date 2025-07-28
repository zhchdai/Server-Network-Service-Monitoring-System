package com.dzc.listener;

import jakarta.annotation.Resource;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RabbitListener(queues = "mail")
public class MailQueueListener {

    @Resource
    JavaMailSender sender;

    @Value("${spring.mail.username}")
    String username;

    @RabbitHandler
    public void sendMailMessage(Map<String, Object> data) {
        String email = data.get("email").toString();
        Integer code = (Integer) data.get("code");
        String clientId = data.get("clientId").toString();
        SimpleMailMessage message = switch (data.get("type").toString()) {
            case "reset" -> createMessage("B21030913-amqp-test",
                    "name: ZhanchiDai",
                    email);
            case "modify" -> createMessage("您的邮件修改验证邮件",
                    "您正在绑定新的电子邮件地址，验证码: " + code + "，有效时间3分钟，如非本人操作，请无视。",
                    email);
            case "host" -> createMessage("主机已离线",
                    "编号为"+clientId+"的主机已离线，请检查主机状态",
                    email);
            case "http" -> createMessage("主机HTTP服务已停止",
                    "编号为"+clientId+"的主机HTTP服务已停止，请检查主机状态",
                    email);
            case "https" -> createMessage("主机HTTPS服务已停止",
                    "编号为"+clientId+"的主机HTTPS服务已停止，请检查主机状态",
                    email);
            case "ftp" -> createMessage("主机FTP服务已停止",
                    "编号为"+clientId+"的主FTP服务已停止，请检查主机状态",
                    email);
            case "sftp" -> createMessage("主机SFTP服务已停止",
                    "编号为"+clientId+"的主机SFTP服务已停止，请检查主机状态",
                    email);
            case "ftps" -> createMessage("主机FTPS服务已停止",
                    "编号为"+clientId+"的主机FTPS服务已停止，请检查主机状态",
                    email);
            case "smtp" -> createMessage("主机SMTP服务已停止",
                    "编号为"+clientId+"的主机SMTP服务已停止，请检查主机状态",
                    email);
            default -> null;
        };
        if (message == null) return;
        sender.send(message);
    }


    private SimpleMailMessage createMessage(String title, String content, String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject(title);
        message.setText(content);
        message.setTo(email);
        message.setFrom(username);
        return message;
    }
}
