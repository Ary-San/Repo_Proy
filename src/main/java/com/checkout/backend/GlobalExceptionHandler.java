package com.checkout.backend;

import com.checkout.backend.exceptions.EmailSenderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(EmailSenderException.class)
    public ProblemDetail handleEmailSend(EmailSenderException ex) {
        log.error("Email sending failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "Email sending failed", ex.getMessage(), "email-send-failed");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleBadRequest(IllegalArgumentException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", ex.getMessage(), "invalid-request");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Server error",
                "An unexpected error occurred", "server-error");
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, String typeSlug) {
        ProblemDetail pd=ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://checkout.com/errors/" + typeSlug));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    @ExceptionHandler(TaskRejectedException.class)
    public ProblemDetail handleOverloaded(TaskRejectedException ex) {
        log.warn("Mail queue full", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service busy",
                "Too many emails queued, try again shortly", "mail-queue-full");
    }
}