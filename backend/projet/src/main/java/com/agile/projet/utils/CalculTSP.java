package com.agile.projet.utils;

import java.util.*;

/**
 * TSP Branch & Bound sur matrice de coûts.
 * - Matrice en indices (0..n-1), mapping via vertexOrder (IDs Long).
 * - Précédence Pickup -> Delivery optionnelle via pickupOfDelivery[indexDelivery] = indexPickup (sinon -1).
 * - Heuristique NN pour incumbent, borne simple minEdge.
 */
public class CalculTSP {

    private final int n;
    private final double[][] cost;          // cost[i][j] = cout (INF si impossible)
    private final List<Long> vertexOrder;   // index -> ID
    private final Map<Long, Integer> idToIndex; // ID -> index (construit ici)

    // Contrainte pickup->delivery: si delivery j, alors pickupOfDelivery[j] = i (index du pickup), sinon -1
    private final int[] pickupOfDelivery;

    // État de recherche
    private final boolean[] visited;
    private double bestCost = Double.POSITIVE_INFINITY;
    private List<Integer> bestPath = new ArrayList<>();

    // borne simple : plus petite arête finie du graphe
    private double minEdge = 0.0;

    // ---------- Construction ----------

    /**
     * @param costMatrix   matrice carrée NxN de coûts (INF si pas de trajet)
     * @param vertexOrder  liste de IDs (Long) telle que vertexOrder.get(i) = ID à l'index i
     *
     * Contrainte pickup->delivery désactivée (tous à -1).
     */
    public CalculTSP(double[][] costMatrix, List<Long> vertexOrder) {
        Objects.requireNonNull(costMatrix, "costMatrix ne peut pas être null");
        Objects.requireNonNull(vertexOrder, "vertexOrder ne peut pas être null");

        this.n = costMatrix.length;
        if (this.n != vertexOrder.size()) {
            throw new IllegalArgumentException("Taille de vertexOrder (" + vertexOrder.size()
                    + ") différente de la taille de la matrice (" + n + ").");
        }
        for (int i = 0; i < n; i++) {
            if (costMatrix[i] == null || costMatrix[i].length != n) {
                throw new IllegalArgumentException("La matrice de coûts doit être carrée (ligne " + i + ").");
            }
        }

        this.cost = costMatrix;
        this.vertexOrder = new ArrayList<>(vertexOrder);
        this.visited = new boolean[n];

        // Construire ID -> index
        this.idToIndex = new HashMap<>(n * 2);
        for (int i = 0; i < n; i++) {
            Long id = this.vertexOrder.get(i);
            if (this.idToIndex.put(id, i) != null) {
                throw new IllegalArgumentException("ID dupliqué dans vertexOrder: " + id);
            }
        }

        // Par défaut: aucune contrainte (tous -1)
        this.pickupOfDelivery = new int[n];
        Arrays.fill(this.pickupOfDelivery, -1);

        precomputeMinEdge();
    }

    /**
     * @param pickupOfDelivery tableau de taille N:
     *                         pour un index 'delivery', valeur = index 'pickup' requis, sinon -1.
     *
     * Contrainte pickup->delivery activée.
     */
    public CalculTSP(double[][] costMatrix, List<Long> vertexOrder, int[] pickupOfDelivery) {
        this(costMatrix, vertexOrder); // vérifs + init
        if (pickupOfDelivery == null || pickupOfDelivery.length != n) {
            throw new IllegalArgumentException("pickupOfDelivery nul ou taille != n");
        }
        // Remplacer les -1 par la contrainte fournie
        System.arraycopy(pickupOfDelivery, 0, this.pickupOfDelivery, 0, n);
    }

    // ---------- API publique ----------

    /** Démarrer depuis un ID (adresse Long) de l'entrepôt (ou autre). */
    public void solveFromId(long startId) {
        Integer startIndex = idToIndex.get(startId);
        if (startIndex == null) {
            throw new IllegalArgumentException("Start ID inconnu: " + startId);
        }
        solveFromIndex(startIndex);
    }

    /** Démarrer depuis un index (0..n-1). */
    public void solveFromIndex(int startIndex) {
        if (startIndex < 0 || startIndex >= n) {
            throw new IllegalArgumentException("startIndex hors bornes: " + startIndex);
        }

        // Reset état
        Arrays.fill(visited, false);
        bestCost = Double.POSITIVE_INFINITY;
        bestPath.clear();

        // 1) Heuristique NN pour initialiser un incumbent (optionnel mais recommandé)
        Tour nn = nearestNeighbor(startIndex);
        if (nn.cost < Double.POSITIVE_INFINITY) {
            setIncumbent(nn.path, nn.cost);
        }

        // 2) Branch & Bound
        visited[startIndex] = true;
        ArrayList<Integer> path = new ArrayList<>();
        path.add(startIndex);

        branchAndBound(path, startIndex, 0.0);

        visited[startIndex] = false;
    }

    /** Coût du meilleur tour trouvé (inclut le retour au départ). */
    public double getBestCost() {
        return bestCost;
    }

    /** Meilleur tour en indices (sans répéter le retour à la base). */
    public List<Integer> getBestPathIndices() {
        return new ArrayList<>(bestPath);
    }

    /** Meilleur tour en IDs (Long). */
    public List<Long> getBestPathIds() {
        List<Long> ids = new ArrayList<>(bestPath.size());
        for (int idx : bestPath) {
            ids.add(vertexOrder.get(idx));
        }
        return ids;
    }

    /** Accès au mapping ID->index si besoin. */
    public Map<Long, Integer> getIdToIndex() {
        return Collections.unmodifiableMap(idToIndex);
    }

    // ---------- Heuristique NN & Incumbent ----------

    private static final class Tour {
        final List<Integer> path;
        final double cost;
        Tour(List<Integer> path, double cost) { this.path = path; this.cost = cost; }
    }

    /** Heuristique "Nearest Neighbor" depuis startIndex. Renvoie un tour réalisable ou +INF si impossible. */
    private Tour nearestNeighbor(int startIndex) {
        boolean[] used = new boolean[n];
        ArrayList<Integer> path = new ArrayList<>(n);
        int cur = startIndex;
        used[cur] = true;
        path.add(cur);
        double total = 0.0;

        for (int step = 1; step < n; step++) {
            int best = -1;
            double wBest = Double.POSITIVE_INFINITY;
            for (int j = 0; j < n; j++) {
                if (!used[j] && cost[cur][j] < wBest) {
                    wBest = cost[cur][j];
                    best = j;
                }
            }
            if (best == -1 || Double.isInfinite(wBest)) {
                return new Tour(Collections.emptyList(), Double.POSITIVE_INFINITY);
            }
            used[best] = true;
            path.add(best);
            total += wBest;
            cur = best;
        }

        double back = cost[cur][startIndex];
        if (Double.isInfinite(back)) {
            return new Tour(Collections.emptyList(), Double.POSITIVE_INFINITY);
        }
        total += back;
        return new Tour(path, total);
    }

    /** Fixe manuellement un incumbent (bestPath/bestCost). */
    private void setIncumbent(List<Integer> path, double totalCost) {
        if (path == null || path.size() != n) return;
        this.bestPath = new ArrayList<>(path);
        this.bestCost = totalCost;
    }

    // ---------- Branch & Bound ----------

    private void branchAndBound(ArrayList<Integer> path, int last, double currentCost) {
        // Tous visités -> fermer le cycle
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

        // Borne simple (optimiste)
        if (bound(path.size(), currentCost) >= bestCost) {
            return; // élagage
        }

        // Générer les candidats restants…
        List<Integer> candidates = new ArrayList<>();
        for (int next = 0; next < n; next++) {
            if (visited[next]) continue;
            if (Double.isInfinite(cost[last][next])) continue;

            // --------- CONTRAINTE PICKUP -> DELIVERY ----------
            // Si 'next' est une livraison (pickupOfDelivery[next] != -1),
            // on n'autorise pas de la visiter tant que son pickup n'a pas été visité.
            int reqPickup = pickupOfDelivery[next];
            if (reqPickup != -1 && !visited[reqPickup]) {
                continue; // coupe la branche
            }
            // ---------------------------------------------------

            candidates.add(next);
        }
        // …puis trier par coût croissant pour trouver vite une bonne solution
        candidates.sort(Comparator.comparingDouble(a -> cost[last][a]));

        // Explorer
        for (int next : candidates) {
            double edge = cost[last][next];
            double newCost = currentCost + edge;
            if (newCost >= bestCost) continue; // inutile si déjà plus mauvais

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
        this.minEdge = Double.isInfinite(m) ? 0.0 : m; // évite les NaN
    }

    /** Borne simple : coût courant + nbRestants * plus petite arête. */
    private double bound(int pathSize, double currentCost) {
        int remaining = n - pathSize;
        return currentCost + remaining * minEdge;
    }
}
