package edu.co.uniquindio.poo.monedero.model;

import edu.co.uniquindio.poo.monedero.exceptions.SaldoInsuficienteException;
import edu.co.uniquindio.poo.monedero.exceptions.TransaccionNoReversibleException;

public class Transferencia extends Transaccion {

    public Transferencia() {
        super();
        this.tipo = "Transferencia";
    }

    @Override
    public void ejecutar() {
        validarMonto();
        validarMonedero();
        if (clienteOrigen == null || clienteDestino == null) {
            throw new IllegalArgumentException("Debe especificar clientes");
        }
        Monedero destino = resolverDestino();
        if (destino == monedero) {
            throw new IllegalArgumentException("El monedero de origen y destino no pueden ser el mismo");
        }
        double total = monto + comision;
        if (monedero.getSaldo() < total) {
            throw new SaldoInsuficienteException(String.format(
                    "Saldo insuficiente. Se requieren $%,.2f (monto + comisión $%,.2f)", total, comision));
        }
        monedero.setSaldo(monedero.getSaldo() - total);
        destino.setSaldo(destino.getSaldo() + monto);
        this.monederoDestino = destino;
        monedero.registrarTransaccion(this);
        destino.registrarTransaccion(this); // el receptor también la ve en su historial
    }

    /**
     * Si no se eligió monedero destino se usa el primero del cliente destino.
     */
    private Monedero resolverDestino() {
        if (monederoDestino != null) {
            if (!clienteDestino.getMonederos().contains(monederoDestino)) {
                throw new IllegalArgumentException("El monedero destino no pertenece al cliente destino");
            }
            return monederoDestino;
        }
        if (clienteDestino.getMonederos().isEmpty()) {
            throw new IllegalArgumentException("Cliente destino sin monederos");
        }
        return clienteDestino.getMonederos().get(0);
    }

    @Override
    public void revertir() {
        validarNoRevertida();
        if (monederoDestino.getSaldo() < monto) {
            throw new TransaccionNoReversibleException(
                    "No se puede revertir: el monedero destino ya no tiene saldo suficiente");
        }
        monederoDestino.setSaldo(monederoDestino.getSaldo() - monto);
        monedero.setSaldo(monedero.getSaldo() + monto + comision);
        revertida = true;
    }

    @Override
    public double calcularComisionBase() {
        // Transferencias entre monederos del mismo cliente no tienen comisión
        if (clienteOrigen != null && clienteOrigen == clienteDestino) {
            return 0;
        }
        return monto * Tarifas.COMISION_TRANSFERENCIA;
    }

    /**
     * Descuento por rango + beneficio "10% menos en comisión por transferencias".
     */
    @Override
    public double aplicarBeneficios(Cliente cliente, double comisionBase) {
        double comisionFinal = comisionBase * (1 - cliente.getRango().getDescuentoComision());
        if (cliente.tieneDescuentoTransferencia()) {
            comisionFinal *= 0.90;
        }
        return comisionFinal;
    }

    @Override
    public boolean esGasto() {
        return true;
    }

    @Override
    public int calcularPuntos() {
        if (clienteOrigen != null && clienteOrigen == clienteDestino) {
            return 0;
        }
        return (int) (monto / 100) * 3;
    }
}