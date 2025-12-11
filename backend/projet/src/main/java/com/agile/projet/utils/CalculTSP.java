package com.agile.projet.utils;

import java.util.*;

/**
 * TSP Branch & Bound sur matrice de coûts.
 * - Matrice en indices (0..n-1), mapping via vertexOrder (IDs Long).
 * - Contrainte Pickup -> Delivery optionnelle via pickupOfDelivery[indexDelivery] = indexPickup (sinon -1).
 */
public class CalculTSP {

    private final int n;
    private final double[][] cost;          // cost[i][j]
    private final List<Long> vertexOrder;   // index -> ID
    private final Map<Long, Integer> idToIndex; // ID -> index

    // Contrainte: si j est une livraison, pickupOfDelivery[j] = index du pickup, sinon -1
    private final int[] pickupOfDelivery;

    private final boolean[] visited;

    private double bestCost = Double.POSITIVE_INFINITY;
    private List<Integer> bestPath = new ArrayList<>();

    private double minEdge = Double.POSITIVE_INFINITY;

    // ---------- Constructeur sans contrainte (optionnel) ----------
    public CalculTSP(double[][] costMatrix, List<Long> vertexOrder) {
        Objects.requireNonNull(costMatrix);
        Objects.requireNonNull(vertexOrder);

        this.n = costMatrix.length;
        if (n != vertexOrder.size())
            throw new IllegalArgumentException("vertexOrder.size != matrix size");

        for (int i = 0; i < n; i++) {
            if (costMatrix[i] == null || costMatrix[i].length != n) {
                throw new IllegalArgumentException("Matrix must be square (row " + i + ")");
            }
        }

        this.cost = costMatrix;
        this.vertexOrder = new ArrayList<>(vertexOrder);
        this.visited = new boolean[n];

        this.idToIndex = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            Long id = this.vertexOrder.get(i);
            if (this.idToIndex.put(id, i) != null) {
                throw new IllegalArgumentException("Duplicate ID in vertexOrder: " + id);
            }
        }

        this.pickupOfDelivery = new int[n];
        Arrays.fill(this.pickupOfDelivery, -1); // pas de contrainte par défaut

        precomputeMinEdge();
    }

    // ---------- Constructeur avec contrainte pickup->delivery ----------
    public CalculTSP(double[][] costMatrix, List<Long> vertexOrder, int[] pickupOfDelivery) {
        this(costMatrix, vertexOrder);
        if (pickupOfDelivery == null || pickupOfDelivery.length != n) {
            throw new IllegalArgumentException("pickupOfDelivery null or wrong length");
        }
        System.arraycopy(pickupOfDelivery, 0, this.pickupOfDelivery, 0, n);
    }

    // ---------- API publique ----------

    public void solveFromId(long startId) {
        Integer idx = idToIndex.get(startId);
        if (idx == null) throw new IllegalArgumentException("Start ID unknown: " + startId);
        solveFromIndex(idx);
    }

    public void solveFromIndex(int startIndex) {
        if (startIndex < 0 || startIndex >= n)
            throw new IllegalArgumentException("startIndex out of range");

        Arrays.fill(visited, false);
        bestCost = Double.POSITIVE_INFINITY;
        bestPath.clear();

        visited[startIndex] = true;
        ArrayList<Integer> path = new ArrayList<>();
        path.add(startIndex);

        branchAndBound(path, startIndex, 0.0);

        visited[startIndex] = false;
    }

    public double getBestCost() { return bestCost; }

    public List<Integer> getBestPathIndices() {
        return new ArrayList<>(bestPath);
    }

    public List<Long> getBestPathIds() {
        List<Long> ids = new ArrayList<>(bestPath.size());
        for (int idx : bestPath) {
            ids.add(vertexOrder.get(idx));
        }
        return ids;
    }

    // ---------- Branch & Bound avec contrainte ----------

    private void branchAndBound(ArrayList<Integer> path, int last, double currentCost) {
        if (path.size() == n) {
            double back = cost[last][path.get(0)];
            if (!Double.isInfinite(back)) {
                double finalCost = currentCost + back;
                if (finalCost < bestCost) {
                    bestCost = finalCost;
                    bestPath = new ArrayList<>(path);
                }
            }
            return;
        }

        if (bound(path.size(), currentCost) >= bestCost) return;

        List<Integer> candidates = new ArrayList<>();
        for (int next = 0; next < n; next++) {
            if (visited[next]) continue;
            if (Double.isInfinite(cost[last][next])) continue;

            // 💥 CONTRAINTE PICKUP -> DELIVERY :
            int reqPickup = pickupOfDelivery[next];   // -1 si ce n'est pas une livraison
            if (reqPickup != -1 && !visited[reqPickup]) {
                // next est une livraison mais son pickup n'est pas encore visité => interdit
                continue;
            }

            candidates.add(next);
        }

        candidates.sort(Comparator.comparingDouble(a -> cost[last][a]));

        for (int next : candidates) {
            double edge = cost[last][next];
            double newCost = currentCost + edge;
            if (newCost >= bestCost) continue;

            visited[next] = true;
            path.add(next);

            branchAndBound(path, next, newCost);

            path.remove(path.size() - 1);
            visited[next] = false;
        }
    }

    // ---------- Borne ----------

    private void precomputeMinEdge() {
        double m = Double.POSITIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                if (i != j && !Double.isInfinite(cost[i][j]) && cost[i][j] < m) {
                    m = cost[i][j];
                }
            }
        }
        this.minEdge = Double.isInfinite(m) ? 0.0 : m;
    }

    private double bound(int pathSize, double currentCost) {
        int remaining = n - pathSize;
        return currentCost + remaining * minEdge;
    }
}
