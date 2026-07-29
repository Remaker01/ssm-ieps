package com.ieps;

import javax.mail.MessagingException;

import com.ieps.util.MailUtil;

public class TestEmail {
    public static void main(String[] args) {
        try {
            MailUtil.send_mail("jiawuliang@163.com", "123456");
            System.out.println("邮件发送成功!");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}