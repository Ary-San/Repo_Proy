package com.checkout.backend.email;

import com.checkout.backend.exceptions.EmailSenderException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import java.io.File;
import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


@Slf4j
@Service
public class EmailServiceImplemented implements EmailService {

    @Autowired
    private JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String sender;

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Void> sendSimpleMail(EmailDetails details) {
        log.info("[mail] thread={} sending to {}",
                Thread.currentThread().getName(), details.getRecipient());
        try {
            SimpleMailMessage mailMessage =
                    new SimpleMailMessage();

            mailMessage.setFrom(sender);
            mailMessage.setTo(details.getRecipient());
            mailMessage.setText(details.getMsgBody());
            mailMessage.setSubject(details.getSubject());
            javaMailSender.send(mailMessage);
            return CompletableFuture.completedFuture(null);
        } catch (MailException e) {
            throw new EmailSenderException("Error while sending mail", e);
        }
    }

    @Async("mailExecutor")
    @Override
    public CompletableFuture<Void> sendMailWithAttachment(EmailDetails details) {
        log.info("[mail with attachment] thread={} sending to {}",
                Thread.currentThread().getName(), details.getRecipient());
        File attachment = new File(details.getAttachment());
        if (!attachment.exists()) {
            throw new IllegalArgumentException(
                    "Attachment not found: " + details.getAttachment());
        }

        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setFrom(sender);
            helper.setTo(details.getRecipient());
            helper.setText(details.getMsgBody());
            helper.setSubject(details.getSubject());

            FileSystemResource file = new FileSystemResource(attachment);
            helper.addAttachment(file.getFilename(), file);

            javaMailSender.send(mimeMessage);
            return CompletableFuture.completedFuture(null);
        } catch (MessagingException | MailException e) {
            throw new EmailSenderException("Error while sending mail", e);
        }
    }
}