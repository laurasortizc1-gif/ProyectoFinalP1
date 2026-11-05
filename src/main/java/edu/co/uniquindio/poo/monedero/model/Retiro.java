package edu.co.uniquindio.poo.monedero.model;

import edu.co.uniquindio.poo.monedero.exceptions.SaldoInsuficienteException;

public class Retiro extends Transaccion {

    public Retiro() {
        super();
        this.tipo = "Retiro";
    }

    public Retiro(Monedero monedero, Cliente clienteOrigen, double monto) {
        super(monedero, clienteOrigen, null, monto);
        this.tipo = "Retiro";
    }

    @Override
    public void ejecutar() {
        validarMonto();
        validarMonedero();
        double total = monto + comision;
        if (monedero.getSaldo() < total) {
            throw new SaldoInsuficienteException(String.format(
                    "Saldo insuficiente. Se requieren $%,.2f (monto + comisión $%,.2f)", total, comision));
        }
        monedero.setSaldo(monedero.getSaldo() - total);
        monedero.registrarTransaccion(this);
    }

    @Override
    public void revertir() {
        validarNoRevertida();
        // Se devuelve el dinero retirado y la comisión cobrada
        monedero.setSaldo(monedero.getSaldo() + monto + comision);
        revertida = true;
    }

    @Override
    public double calcularComisionBase() {
        return monto * Tarifas.COMISION_RETIRO;
    }

    /** Beneficio "Un mes sin cargos por retiros". */
    @Override
    public double aplicarBeneficios(Cliente cliente, double comisionBase) {
        return cliente.tieneRetirosSinCargo() ? 0 : comisionBase;
    }

    @Override
    public boolean esGasto() {
        return true;
    }

    @Override
    public int calcularPuntos() {
        return (int) (monto / 100) * 2;
    }
}
