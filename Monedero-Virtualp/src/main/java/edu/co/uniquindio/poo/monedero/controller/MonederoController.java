package edu.co.uniquindio.poo.monedero.controller;

import edu.co.uniquindio.poo.monedero.model.*;

public class MonederoController {

    private final TransaccionController transaccionController;

    public MonederoController() {
        this(new TransaccionController());
    }

    public MonederoController(TransaccionController transaccionController) {
        this.transaccionController = transaccionController;
    }

    public Transaccion depositar(Monedero monedero, double monto) {
        return transaccionController.ejecutarTransaccion(
                transaccionController.crearTransaccion("Deposito", monto, monedero.getDueño(), null, monedero));
    }

    public Transaccion retirar(Monedero monedero, double monto) {
        return transaccionController.ejecutarTransaccion(
                transaccionController.crearTransaccion("Retiro", monto, monedero.getDueño(), null, monedero));
    }

    public Transaccion transferir(Cliente origen, Cliente destino, double monto, Monedero monederoOrigen) {
        return transaccionController.ejecutarTransaccion(
                transaccionController.crearTransaccion("Transferencia", monto, origen, destino, monederoOrigen));
    }

    /** Transferencia entre dos monederos del mismo cliente (multimonedero), sin comisión. */
    public Transaccion moverEntreMonederos(Cliente cliente, Monedero origen, Monedero destino, double monto) {
        return transaccionController.ejecutarTransaccion(
                transaccionController.crearTransaccion("Transferencia", monto, cliente, cliente, origen, destino));
    }
}
