package io.propenuy.asis_app_be.restservice;

public class PaymentRequestAccessForbiddenException extends RuntimeException {
    public PaymentRequestAccessForbiddenException(String message) {
        super(message);
    }
}
