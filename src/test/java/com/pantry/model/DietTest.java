package com.pantry.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DietTest {

    @Test
    void unOnnivoroMangiaTutto() {
        assertTrue(Diet.ONNIVORA.accepts(Diet.ONNIVORA));
        assertTrue(Diet.ONNIVORA.accepts(Diet.VEGANA));
    }

    @Test
    void unVegetarianoNonMangiaRicetteOnnivore() {
        assertFalse(Diet.VEGETARIANA.accepts(Diet.ONNIVORA));
        assertTrue(Diet.VEGETARIANA.accepts(Diet.VEGETARIANA));
        assertTrue(Diet.VEGETARIANA.accepts(Diet.VEGANA));
    }

    @Test
    void unVeganoMangiaSoloRicetteVegane() {
        assertFalse(Diet.VEGANA.accepts(Diet.VEGETARIANA));
        assertTrue(Diet.VEGANA.accepts(Diet.VEGANA));
    }
}
