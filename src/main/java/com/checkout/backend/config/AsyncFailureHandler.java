package com.checkout.backend.config;

import com.checkout.backend.exceptions.EmailSenderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletionException;

@Component
@Slf4j
public class AsyncFailureHandler {
  public void handle(String operation, String recipient, Throwable ex) {
        Throwable cause = (ex instanceof CompletionException && ex.getCause() != null)
                ? ex.getCause() : ex;

        if (cause instanceof EmailSenderException) {
            log.error("[failure] {} to {} failed (SMTP): {}", operation, recipient, cause.getMessage(), cause);
        } else {
            log.error("[failure] {} to {} unexpected error", operation, recipient, cause);
        }
    }
}