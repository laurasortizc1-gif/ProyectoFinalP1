package edu.co.uniquindio.poo.monedero.model;

import java.util.List;

import edu.co.uniquindio.poo.monedero.model.interfaces.ICanjeable;

/**
 * Beneficio canjeable con puntos. Es abstracta: cada beneficio concreto
 * implementa su propio efecto en aplicar() (Polimorfismo).
 */
public abstract class Beneficio implements ICanjeable {
    private int idBeneficio;
    private String descripcion;
    private int costoPuntos;
    private String efecto;

    protected Beneficio(int id, String descripcion, int costoPuntos, String efecto) {
        this.idBeneficio = id;
        this.descripcion = descripcion;
        this.costoPuntos = costoPuntos;
        this.efecto = efecto;
    }

    /** Catálogo de beneficios disponibles en el sistema. */
    public static List<Beneficio> catalogo() {
        return List.of(
                new BeneficioDescuentoTransferencia(),
                new BeneficioRetirosSinCargo(),
                new BeneficioBonoSaldo()
        );
    }

    public int getIdBeneficio() { return idBeneficio; }
    public void setIdBeneficio(int idBeneficio) { this.idBeneficio = idBeneficio; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public int getCostoPuntos() { return costoPuntos; }
    public void setCostoPuntos(int costoPuntos) { this.costoPuntos = costoPuntos; }

    public String getEfecto() { return efecto; }
    public void setEfecto(String efecto) { this.efecto = efecto; }

    @Override
    public String toString() {
        return descripcion + " (" + costoPuntos + " pts)";
    }
}
