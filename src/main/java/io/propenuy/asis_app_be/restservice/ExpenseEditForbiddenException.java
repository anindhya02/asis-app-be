package io.propenuy.asis_app_be.restservice;

public class ExpenseEditForbiddenException extends RuntimeException {
    public ExpenseEditForbiddenException(String message) {
        super(message);
    }
}
