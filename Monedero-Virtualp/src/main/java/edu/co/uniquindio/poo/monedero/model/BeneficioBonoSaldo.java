package edu.co.uniquindio.poo.monedero.model;

import edu.co.uniquindio.poo.monedero.exceptions.EntidadNoEncontradaException;

/** 1000 puntos: bono de 50 unidades en el monedero principal. */
public class BeneficioBonoSaldo extends Beneficio {
    public static final double VALOR_BONO = 50;

    public BeneficioBonoSaldo() {
        super(3, "Bono de $50 en el monedero principal", 1000, "BONO_SALDO");
    }

    @Override
    public void aplicar(Cliente cliente) {
        if (cliente.getMonederos().isEmpty()) {
            throw new EntidadNoEncontradaException("El cliente no tiene monederos para recibir el bono");
        }
        Monedero principal = cliente.getMonederos().get(0);
        principal.setSaldo(principal.getSaldo() + VALOR_BONO);
    }
}
