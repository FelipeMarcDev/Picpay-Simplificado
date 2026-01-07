package com.picpaysimplicado.infra;

import com.picpaysimplicado.dtos.ExceptionDTO;
import com.picpaysimplicado.infra.exceptions.TransactionNotAuthorizedException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpServerErrorException;
import tools.jackson.databind.ObjectMapper;

@RestControllerAdvice
public class ControllerException {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity threatDuplicateEntry(DataIntegrityViolationException e) {
        ExceptionDTO exceptionDTO = new ExceptionDTO("Usuário já cadastrado!", "400");
        return ResponseEntity.badRequest().body(exceptionDTO);
    }
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity threat404(EntityNotFoundException e) {
       return ResponseEntity.notFound().build();
    }
    @ExceptionHandler(TransactionNotAuthorizedException.class)
    public ResponseEntity<ExceptionDTO> handleTransactionNotAuthorized(TransactionNotAuthorizedException e) {
        ExceptionDTO dto = new ExceptionDTO(e.getMessage(), "403");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(dto);
    }
    @ExceptionHandler(HttpServerErrorException.GatewayTimeout.class)
    public ResponseEntity<Object> threatGatewayTimeout(
            HttpServerErrorException.GatewayTimeout e
    ) {
        try {
            ObjectMapper mapper = new ObjectMapper();

            // body ORIGINAL da API externa
            Object body = mapper.readValue(
                    e.getResponseBodyAsString(),
                    Object.class
            );

            return ResponseEntity
                    .status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(body);

        } catch (Exception ex) {
            // fallback caso o body não seja JSON válido
            ExceptionDTO exceptionDTO = new ExceptionDTO(
                    "The service is not available, try again later",
                    "504"
            );
            return ResponseEntity
                    .status(HttpStatus.GATEWAY_TIMEOUT)
                    .body(exceptionDTO);
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity threatGeneralException(Exception e) {
        ExceptionDTO exceptionDTO = new ExceptionDTO(e.getMessage(), "500");
        return ResponseEntity.internalServerError().body(exceptionDTO);
    }

}
