package com.agile.projet.utils;

import java.util.List;
import java.util.Objects;


public class MatriceCout {
    private final double[][] costMatrix;
    public MatriceCout(double[][] costMatrix) { this.costMatrix = costMatrix; }
    public double[][] getCostMatrix() { return costMatrix; }
}
