package edu.co.uniquindio.poo.monedero.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Algoritmo de ordenamiento Merge Sort implementado manualmente.
 * Se usa para ordenar las transacciones programadas por fecha de ejecución.
 *
 * Complejidad temporal: O(n log n) en el mejor, promedio y peor caso.
 * Complejidad espacial: O(n) por las listas auxiliares.
 * Es estable: dos transacciones con la misma fecha conservan su orden de creación.
 */
public final class Ordenamiento {

    private Ordenamiento() {}

    public static <T> List<T> mergeSort(List<T> lista, Comparator<? super T> comparador) {
        if (lista.size() <= 1) {
            return new ArrayList<>(lista);
        }
        int medio = lista.size() / 2;
        List<T> izquierda = mergeSort(lista.subList(0, medio), comparador);
        List<T> derecha = mergeSort(lista.subList(medio, lista.size()), comparador);
        return mezclar(izquierda, derecha, comparador);
    }

    private static <T> List<T> mezclar(List<T> izq, List<T> der, Comparator<? super T> comparador) {
        List<T> resultado = new ArrayList<>(izq.size() + der.size());
        int i = 0, j = 0;
        while (i < izq.size() && j < der.size()) {
            if (comparador.compare(izq.get(i), der.get(j)) <= 0) {
                resultado.add(izq.get(i++));
            } else {
                resultado.add(der.get(j++));
            }
        }
        while (i < izq.size()) resultado.add(izq.get(i++));
        while (j < der.size()) resultado.add(der.get(j++));
        return resultado;
    }
}
