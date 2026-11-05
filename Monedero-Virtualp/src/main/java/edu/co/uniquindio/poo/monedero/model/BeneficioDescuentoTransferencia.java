package edu.co.uniquindio.poo.monedero.model;

/** 100 puntos: reducción del 10% en la comisión por transferencias durante 30 días. */
public class BeneficioDescuentoTransferencia extends Beneficio {

    public BeneficioDescuentoTransferencia() {
        super(1, "10% menos comisión en transferencias (30 días)", 100, "DESCUENTO_TRANSFERENCIA");
    }

    @Override
    public void aplicar(Cliente cliente) {
        cliente.activarDescuentoTransferencia(30);
    }
}
