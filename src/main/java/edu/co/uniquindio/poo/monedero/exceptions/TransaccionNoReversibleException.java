package edu.co.uniquindio.poo.monedero.exceptions;

public class TransaccionNoReversibleException extends RuntimeException {
    public TransaccionNoReversibleException(String mensaje) {
        super(mensaje);
    }
}
