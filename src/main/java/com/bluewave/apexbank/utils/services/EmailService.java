package com.bluewave.apexbank.utils.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    @Value("${spring.mail.username}")
    private String senderEmail;

    private final JavaMailSender javaMailSender;

    public void sendEmail(String to,String subject, String body){
        SimpleMailMessage message=new SimpleMailMessage();
        message.setTo(to);
        message.setFrom(senderEmail);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
        log.info("email send {} successfully", to);

    }


}
