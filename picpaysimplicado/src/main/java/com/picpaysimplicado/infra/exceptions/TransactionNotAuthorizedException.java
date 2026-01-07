package com.picpaysimplicado.infra.exceptions;

public class TransactionNotAuthorizedException extends RuntimeException {

    public TransactionNotAuthorizedException() {
        super("Transação não autorizada!");
    }
}
