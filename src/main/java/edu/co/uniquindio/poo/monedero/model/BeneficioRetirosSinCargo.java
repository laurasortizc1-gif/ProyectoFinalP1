package edu.co.uniquindio.poo.monedero.model;

/** 500 puntos: un mes sin cargos por retiros. */
public class BeneficioRetirosSinCargo extends Beneficio {

    public BeneficioRetirosSinCargo() {
        super(2, "Un mes sin cargos por retiros", 500, "RETIROS_SIN_CARGO");
    }

    @Override
    public void aplicar(Cliente cliente) {
        cliente.activarRetirosSinCargo(1);
    }
}
