package com.ecommerce.order_service.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j //permite generar registros logs de forma profesionarl y automatica
public class GlobalControllerAdvice {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request){

        log.warn("Recurso no encontrado - Path: {}, Message: {}",request.getDescription(false),ex.getMessage());
        ProblemDetail problemDetail= ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());

        problemDetail.setTitle("Recurso no encontrado");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/not-found"));
        problemDetail.setProperty("Timestap", Instant.now());

        problemDetail.setProperty("Resource", ex.getResourceName());
        problemDetail.setProperty("Field", ex.getFieldName());
        problemDetail.setProperty("Value", ex.getFieldValue());

        return problemDetail;
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValidException(MethodArgumentNotValidException ex){


        ProblemDetail problemDetail= ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "La validacion fallo en uno o mas campos");

        problemDetail.setTitle("Error de Validacion");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/error-validation"));
        problemDetail.setProperty("Timestap", Instant.now());

        Map<String,String> errorMap = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(
                e -> {
                    errorMap.put(e.getField(),e.getDefaultMessage());
                }
        );
        problemDetail.setProperty("errors",errorMap);
        return problemDetail;
    }


    @ExceptionHandler(Exception.class)
    public ProblemDetail handelException(Exception ex, WebRequest request){


        log.warn("A ocurrido un erro inesperado {}: {}",
                request.getDescription(false),ex.getMessage(),ex);
        ProblemDetail problemDetail= ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "A ocurrido un error inesaperado. Por favor contactar, con el administrador");

        problemDetail.setTitle("Interal Server Error");
        problemDetail.setType(URI.create("https://api.ecommerce.com/errors/internal"));
        problemDetail.setProperty("Timestap", Instant.now());

        return problemDetail;
    }

}
