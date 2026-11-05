package edu.co.uniquindio.poo.monedero.model;

import edu.co.uniquindio.poo.monedero.exceptions.TransaccionNoReversibleException;

public class Deposito extends Transaccion {

    public Deposito() {
        super();
        this.tipo = "Depósito";
    }

    public Deposito(Monedero monedero, Cliente clienteOrigen, double monto) {
        super(monedero, clienteOrigen, null, monto);
        this.tipo = "Depósito";
    }

    @Override
    public void ejecutar() {
        validarMonto();
        validarMonedero();
        monedero.setSaldo(monedero.getSaldo() + monto);
        monedero.registrarTransaccion(this);
    }

    @Override
    public void revertir() {
        validarNoRevertida();
        if (monedero.getSaldo() < monto) {
            throw new TransaccionNoReversibleException(
                    "No se puede revertir el depósito: el saldo actual es menor al monto depositado");
        }
        monedero.setSaldo(monedero.getSaldo() - monto);
        revertida = true;
    }

    @Override
    public int calcularPuntos() {
        return (int) (monto / 100) * 1;
    }
}
