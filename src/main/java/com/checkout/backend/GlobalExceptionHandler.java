package com.checkout.backend;

import com.checkout.backend.exceptions.ApiException;
import com.checkout.backend.exceptions.EmailSenderException;
import com.checkout.backend.exceptions.dto.ErrorResponseDTO.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.MissingRequestValueException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String INTERNAL_ERROR_MESSAGE =
            "An internal error occurred. Please try again later.";

    // Domain / business exceptions

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException ex) {
        return problem(ex.getStatus(), "Request error", ex.getMessage(), "api-error");
    }

    @ExceptionHandler(EmailSenderException.class)
    public ProblemDetail handleEmailSend(EmailSenderException ex) {
        log.error("Email sending failed", ex);
        return problem(HttpStatus.BAD_GATEWAY, "Email sending failed",
                "The email could not be sent. Please try again later.", "email-send-failed");
    }

    @ExceptionHandler(TaskRejectedException.class)
    public ProblemDetail handleOverloaded(TaskRejectedException ex) {
        log.warn("Mail queue full", ex);
        return problem(HttpStatus.SERVICE_UNAVAILABLE, "Service busy",
                "Too many emails queued, try again shortly", "mail-queue-full");
    }

    // Validation

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<FieldErrorDTO> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldError)
                .toList();

        log.debug("Validation failed on {}: {}", request.getRequestURI(), fieldErrors);

        return problemWithErrors(HttpStatus.BAD_REQUEST, "Invalid request body",
                "The request contains invalid fields.", "invalid-request-body", fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ProblemDetail handleMethodValidation(HandlerMethodValidationException ex, HttpServletRequest request) {
        List<FieldErrorDTO> fieldErrors = ex.getParameterValidationResults().stream()
                .flatMap(result -> toFieldErrors(result).stream())
                .toList();

        log.debug("Parameter validation failed on {}: {}", request.getRequestURI(), fieldErrors);

        return problemWithErrors(HttpStatus.BAD_REQUEST, "Invalid request parameters",
                "The request contains invalid parameters.", "invalid-request-parameters", fieldErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        List<FieldErrorDTO> fieldErrors = ex.getConstraintViolations().stream()
                .map(violation -> new FieldErrorDTO(
                        lastNode(String.valueOf(violation.getPropertyPath())),
                        violation.getMessage()))
                .toList();

        log.debug("Constraint violated on {}: {}", request.getRequestURI(), fieldErrors);

        return problemWithErrors(HttpStatus.BAD_REQUEST, "Invalid request parameters",
                "The request contains invalid parameters.", "invalid-request-parameters", fieldErrors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.debug("Unreadable body on {}: {}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, "Malformed request body",
                "The request body could not be read. Make sure it is valid JSON.", "malformed-body");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParameter(MissingServletRequestParameterException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Missing parameter",
                "Required parameter '" + ex.getParameterName() + "' is missing.", "missing-parameter");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ProblemDetail handleMissingHeader(MissingRequestHeaderException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Missing header",
                "Required header '" + ex.getHeaderName() + "' is missing.", "missing-header");
    }

    @ExceptionHandler(MissingRequestValueException.class)
    public ProblemDetail handleMissingRequestValue(MissingRequestValueException ex) {
        return problem(HttpStatus.BAD_REQUEST, "Missing value",
                "A required value is missing from the request.", "missing-value");
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ProblemDetail handleMissingPathVariable(MissingPathVariableException ex, HttpServletRequest request) {
        log.error("Invalid mapping on {}: missing path variable '{}' in the URI template",
                request.getRequestURI(), ex.getVariableName(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Server error", INTERNAL_ERROR_MESSAGE, "server-error");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> expected = ex.getRequiredType();
        String detail = "Value '" + ex.getValue() + "' is not valid for parameter '"
                + ex.getName() + "'"
                + (expected != null ? ", expected " + expected.getSimpleName() : "")
                + ".";
        return problem(HttpStatus.BAD_REQUEST, "Invalid parameter", detail, "invalid-parameter");
    }

    //Auth

    @ExceptionHandler(AuthenticationException.class)
    public ProblemDetail handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.debug("Authentication failed on {}: {}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.UNAUTHORIZED, "Unauthorized", "Invalid credentials.", "unauthorized");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        if (!isAuthenticated()) {
            log.debug("Anonymous access rejected on {}", request.getRequestURI());
            return problem(HttpStatus.UNAUTHORIZED, "Unauthorized",
                    "You must authenticate to access this resource.", "unauthorized");
        }
        log.debug("Access denied on {}: {}", request.getRequestURI(), ex.getMessage());
        return problem(HttpStatus.FORBIDDEN, "Forbidden",
                "You do not have permission to perform this action.", "forbidden");
    }

    // Routing

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", "The requested route does not exist.", "not-found");
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ProblemDetail handleNoHandlerFound(NoHandlerFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Not found", "The requested route does not exist.", "not-found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return problem(HttpStatus.METHOD_NOT_ALLOWED, "Method not allowed",
                "Method " + ex.getMethod() + " is not allowed on this route.", "method-not-allowed");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ProblemDetail handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        String supported = String.join(", ", ex.getSupportedMediaTypes().stream()
                .map(Object::toString)
                .toList());
        String detail = "Content type '" + ex.getContentType() + "' is not supported."
                + (supported.isEmpty() ? "" : " Supported types: " + supported + ".");
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Unsupported media type", detail, "unsupported-media-type");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ProblemDetail handleMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex) {
        return problem(HttpStatus.NOT_ACCEPTABLE, "Not acceptable",
                "This API only responds in JSON format.", "not-acceptable");
    }

    //Data / status

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity violation on {}", request.getRequestURI(), ex);
        return problem(HttpStatus.CONFLICT, "Conflict",
                "The operation conflicts with existing data.", "conflict");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatus(ResponseStatusException ex, HttpServletRequest request) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        if (status.is5xxServerError()) {
            log.error("Error with explicit status on {} {}", request.getMethod(), request.getRequestURI(), ex);
            return problem(status, "Server error", INTERNAL_ERROR_MESSAGE, "server-error");
        }

        String detail = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        log.debug("Explicit status {} on {}: {}", status.value(), request.getRequestURI(), detail);
        return problem(status, status.getReasonPhrase(), detail, "response-status-error");
    }

    // Fallback

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, HttpServletRequest request) {
        log.error("Unhandled error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Server error", INTERNAL_ERROR_MESSAGE, "server-error");
    }

    // Helpers

    private ProblemDetail problem(HttpStatus status, String title, String detail, String typeSlug) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://checkout.com/errors/" + typeSlug));
        pd.setProperty("timestamp", Instant.now().toString());
        return pd;
    }

    private ProblemDetail problemWithErrors(HttpStatus status, String title, String detail,
                                            String typeSlug, List<FieldErrorDTO> fieldErrors) {
        ProblemDetail pd = problem(status, title, detail, typeSlug);
        pd.setProperty("errors", fieldErrors);
        return pd;
    }

    private static List<FieldErrorDTO> toFieldErrors(ParameterValidationResult result) {
        if (result instanceof ParameterErrors errors) {
            return errors.getFieldErrors().stream()
                    .map(GlobalExceptionHandler::toFieldError)
                    .toList();
        }

        String name = result.getMethodParameter().getParameterName();
        return result.getResolvableErrors().stream()
                .map(error -> new FieldErrorDTO(
                        name != null ? name : "parameter",
                        resolvableMessage(error)))
                .toList();
    }

    private static String resolvableMessage(MessageSourceResolvable error) {
        return error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value.";
    }

    private static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    private static FieldErrorDTO toFieldError(FieldError error) {
        String message = error.getDefaultMessage() != null
                ? error.getDefaultMessage()
                : "Invalid value.";
        return new FieldErrorDTO(error.getField(), message);
    }

    private static String lastNode(String propertyPath) {
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }
}