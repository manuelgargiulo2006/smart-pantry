package com.pantry.service;

/** Errore di logica con un messaggio da mostrare all'utente (es. "categoria già esistente"). */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
