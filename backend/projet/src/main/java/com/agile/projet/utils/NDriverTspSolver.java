package com.agile.projet.utils;

import com.agile.projet.model.Delivery;
import com.agile.projet.model.DemandeDelivery;
import com.agile.projet.model.Entrepot;
import com.agile.projet.model.PickupDeliveryModel;

import java.util.*;

/**
 * Répartit une tournée sur N livreurs en respectant :
 *  - un driver doit faire pickup + delivery d'une même livraison
 *  - TSP avec précédence pickup -> delivery (nouvelle version CalculTSP)
 *  - recalcul d'un TSP indépendant pour chaque driver sur son sous-ensemble de noeuds
 *  - calcule la durée cumulée par driver (trajet + temps de service)
 *
 * Stratégie de répartition :
 *  1) TSP global (avec précédence) pour obtenir un ordre "raisonnable"
 *  2) Trier les Delivery selon leur première apparition (pickup ou delivery) dans ce chemin global
 *  3) Découper en N "chunks" contigus aussi équitables que possible (diff ≤ 1)
 *  4) Pour chaque chunk : sous-problème, TSP local (avec précédence), durée
 */
public class NDriverTspSolver {

    public static final class DriverSolution {
        private final int driverIndex;
        private final List<Long> pathIds;          // ordre des noeuds (IDs) visités (cycle implicite)
        private final double durationSeconds;      // trajet + services

        public DriverSolution(int driverIndex, List<Long> pathIds, double durationSeconds) {
            this.driverIndex = driverIndex;
            this.pathIds = pathIds;
            this.durationSeconds = durationSeconds;
        }

        public int getDriverIndex() { return driverIndex; }
        public List<Long> getPathIds() { return pathIds; }
        public double getDurationSeconds() { return durationSeconds; }
    }

    public static final class MultiDriverSolution {
        private final List<DriverSolution> drivers;

        public MultiDriverSolution(List<DriverSolution> drivers) {
            this.drivers = drivers;
        }

        public List<DriverSolution> getDrivers() { return drivers; }

        /** Pratique: max durée parmi les drivers (équilibrage temporel à faire ensuite si besoin). */
        public double getMaxDurationSeconds() {
            double m = 0.0;
            for (DriverSolution d : drivers) m = Math.max(m, d.getDurationSeconds());
            return m;
        }

        /** Pratique: somme des durées (pas forcément pertinente si exécution parallèle). */
        public double getTotalDurationSeconds() {
            double s = 0.0;
            for (DriverSolution d : drivers) s += d.getDurationSeconds();
            return s;
        }
    }

    /**
     * @param model PickupDeliveryModel déjà prêt: matrice de coûts + vertexOrder + pickupOfDelivery + demande + entrepot
     * @param nDrivers nombre de livreurs
     * @param speedMetersPerSec vitesse (m/s) pour convertir la distance en temps
     */
    public static MultiDriverSolution solveForNDrivers(PickupDeliveryModel model,
                                                       int nDrivers,
                                                       double speedMetersPerSec) {
        if (model == null) throw new IllegalArgumentException("model null");
        if (nDrivers <= 0) throw new IllegalArgumentException("nDrivers doit être > 0");
        if (speedMetersPerSec <= 0) throw new IllegalArgumentException("La vitesse doit être > 0");

        if (model.getMatriceCout() == null || model.getVertexOrder() == null) {
            throw new IllegalStateException("PickupDeliveryModel ou matrice non initialisés");
        }
        if (model.getPickupOfDelivery() == null) {
            throw new IllegalStateException("pickupOfDelivery non initialisé dans le modèle");
        }

        DemandeDelivery demande = model.getDemandeDelivery();
        if (demande == null || demande.getDeliveries() == null || demande.getDeliveries().isEmpty()) {
            // Aucun point -> solutions vides
            List<DriverSolution> empty = new ArrayList<>();
            for (int i = 0; i < nDrivers; i++) empty.add(new DriverSolution(i, List.of(), 0.0));
            return new MultiDriverSolution(empty);
        }

        Entrepot entrepot = model.getEntrepot();
        if (entrepot == null || entrepot.getAdresse() == null) {
            throw new IllegalStateException("Entrepôt non défini dans le modèle");
        }
        long depotId = entrepot.getAdresse();

        double[][] globalCost = model.getMatriceCout().getCostMatrix();
        List<Long> globalVertexOrder = model.getVertexOrder();

        // 1) TSP global AVEC précédence pickup->delivery
        CalculTSP globalTsp = new CalculTSP(globalCost, globalVertexOrder, model.getPickupOfDelivery());
        globalTsp.solveFromId(depotId);
        List<Long> globalRouteIds = globalTsp.getBestPathIds();

        // 2) Ordonner les Delivery selon leur première apparition dans le chemin global
        List<Delivery> deliveriesOrdered = orderDeliveriesByGlobalRoute(demande.getDeliveries(), globalRouteIds);

        // 3) Répartition équitable en N chunks contigus (diff de taille <= 1)
        List<List<Delivery>> buckets = splitEquitably(deliveriesOrdered, nDrivers);

        // 4) Temps de service
        Map<Long, Long> serviceTimes = buildServiceTimeMap(demande);

        // 5) TSP par driver (sous-problème)
        List<DriverSolution> result = new ArrayList<>(nDrivers);

        for (int i = 0; i < nDrivers; i++) {
            List<Delivery> assigned = buckets.get(i);

            if (assigned.isEmpty()) {
                result.add(new DriverSolution(i, List.of(), 0.0));
                continue;
            }

            SubTspData sub = buildSubProblem(globalCost, globalVertexOrder, depotId, assigned);

            if (sub.vertexOrder.size() <= 1) {
                result.add(new DriverSolution(i, List.of(), 0.0));
                continue;
            }

            int[] subPickup = buildSubPickupOfDelivery(
                    model.getPickupOfDelivery(),
                    globalVertexOrder,
                    sub.vertexOrder
            );

            CalculTSP tsp = new CalculTSP(sub.costMatrix, sub.vertexOrder, subPickup);
            tsp.solveFromId(depotId);
            List<Long> pathIds = tsp.getBestPathIds();

            double duration = 0.0;
            if (pathIds != null && !pathIds.isEmpty()) {
                duration = computeTourDurationSeconds(
                        tsp, sub.costMatrix, sub.vertexOrder, serviceTimes, sub.depotIndex, speedMetersPerSec
                );
            } else {
                duration = Double.POSITIVE_INFINITY; // pas de solution valable
            }

            result.add(new DriverSolution(i, pathIds == null ? List.of() : pathIds, duration));
        }

        return new MultiDriverSolution(result);
    }

    // -------------------- Structures internes --------------------

    private static final class SubTspData {
        final double[][] costMatrix;
        final List<Long> vertexOrder;
        final int depotIndex;

        SubTspData(double[][] m, List<Long> v, int depotIndex) {
            this.costMatrix = m;
            this.vertexOrder = v;
            this.depotIndex = depotIndex;
        }
    }

    // -------------------- Helpers --------------------

    /** Ordre des Delivery en fonction de leur première apparition (pickup ou delivery) dans la tournée globale. */
    private static List<Delivery> orderDeliveriesByGlobalRoute(List<Delivery> deliveries,
                                                               List<Long> globalRouteIds) {
        Map<Long, Integer> position = new HashMap<>();
        for (int i = 0; i < globalRouteIds.size(); i++) {
            long id = globalRouteIds.get(i);
            position.putIfAbsent(id, i);
        }

        List<Delivery> ordered = new ArrayList<>(deliveries);
        ordered.sort(Comparator.comparingInt(d -> {
            Integer pPickup = position.get(d.getAdresseEnlevement());
            Integer pDel    = position.get(d.getAdresseLivraison());
            int pp = (pPickup == null) ? Integer.MAX_VALUE : pPickup;
            int pd = (pDel    == null) ? Integer.MAX_VALUE : pDel;
            return Math.min(pp, pd);
        }));
        return ordered;
    }

    /** Split contigu en N groupes équilibrés (différence de taille <= 1). */
    private static List<List<Delivery>> splitEquitably(List<Delivery> ordered, int nDrivers) {
        int m = ordered.size();
        int base = m / nDrivers;
        int rem  = m % nDrivers;

        List<List<Delivery>> buckets = new ArrayList<>(nDrivers);
        int idx = 0;
        for (int i = 0; i < nDrivers; i++) {
            int size = base + (i < rem ? 1 : 0);
            if (size <= 0) {
                buckets.add(new ArrayList<>());
                continue;
            }
            buckets.add(new ArrayList<>(ordered.subList(idx, idx + size)));
            idx += size;
        }
        return buckets;
    }

    /** Map idNoeud -> temps de service (en secondes). */
    private static Map<Long, Long> buildServiceTimeMap(DemandeDelivery demande) {
        Map<Long, Long> map = new HashMap<>();
        if (demande == null || demande.getDeliveries() == null) return map;

        for (Delivery d : demande.getDeliveries()) {
            map.put(d.getAdresseEnlevement(), d.getDureeEnlevement());
            map.put(d.getAdresseLivraison(), d.getDureeLivraison());
        }
        return map;
    }

    /**
     * Sous-problème : dépôt + tous les noeuds (pickup & delivery) des Delivery assignées.
     * Garantit qu'un driver qui a une Delivery possède bien ses deux noeuds.
     */
    private static SubTspData buildSubProblem(double[][] globalCost,
                                              List<Long> globalVertexOrder,
                                              long depotId,
                                              List<Delivery> deliveries) {
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(depotId);
        for (Delivery d : deliveries) {
            if (d.getAdresseEnlevement() != null) ids.add(d.getAdresseEnlevement());
            if (d.getAdresseLivraison() != null) ids.add(d.getAdresseLivraison());
        }

        Map<Long, Integer> idToGlobalIndex = new HashMap<>();
        for (int i = 0; i < globalVertexOrder.size(); i++) {
            idToGlobalIndex.put(globalVertexOrder.get(i), i);
        }

        List<Long> localVertexOrder = new ArrayList<>();
        localVertexOrder.add(depotId);
        for (Long id : ids) {
            if (!Objects.equals(id, depotId)) localVertexOrder.add(id);
        }

        int n = localVertexOrder.size();
        double[][] subMatrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            Long idFrom = localVertexOrder.get(i);
            Integer gi = idToGlobalIndex.get(idFrom);
            if (gi == null) {
                Arrays.fill(subMatrix[i], Double.POSITIVE_INFINITY);
                continue;
            }
            for (int j = 0; j < n; j++) {
                Long idTo = localVertexOrder.get(j);
                Integer gj = idToGlobalIndex.get(idTo);
                subMatrix[i][j] = (gj == null) ? Double.POSITIVE_INFINITY : globalCost[gi][gj];
            }
        }

        return new SubTspData(subMatrix, localVertexOrder, 0);
    }

    /**
     * Construit le pickupOfDelivery du sous-TSP (indices locaux).
     * subPickup[localDeliveryIndex] = localPickupIndex, sinon -1.
     */
    private static int[] buildSubPickupOfDelivery(int[] globalPickupOfDelivery,
                                                  List<Long> globalVertexOrder,
                                                  List<Long> subVertexOrder) {
        int n = subVertexOrder.size();
        int[] subPickup = new int[n];
        Arrays.fill(subPickup, -1);

        Map<Long, Integer> globalIndex = new HashMap<>();
        for (int i = 0; i < globalVertexOrder.size(); i++) {
            globalIndex.put(globalVertexOrder.get(i), i);
        }

        Map<Long, Integer> localIndex = new HashMap<>();
        for (int i = 0; i < subVertexOrder.size(); i++) {
            localIndex.put(subVertexOrder.get(i), i);
        }

        for (int localD = 0; localD < n; localD++) {
            long idD = subVertexOrder.get(localD);
            Integer globalD = globalIndex.get(idD);
            if (globalD == null) continue;

            int globalP = globalPickupOfDelivery[globalD];
            if (globalP < 0) continue;

            long idPickup = globalVertexOrder.get(globalP);
            Integer localP = localIndex.get(idPickup);
            if (localP != null) subPickup[localD] = localP;
        }

        return subPickup;
    }

    /**
     * Durée = somme(distance)/vitesse + somme(temps service sur chaque arrivée (hors dépôt)).
     * On parcourt le cycle implicite du meilleur chemin (dernier -> premier).
     */
    private static double computeTourDurationSeconds(CalculTSP solver,
                                                     double[][] matrix,
                                                     List<Long> vertexOrder,
                                                     Map<Long, Long> serviceTimes,
                                                     int depotIndex,
                                                     double speedMetersPerSec) {
        List<Integer> path = solver.getBestPathIndices();
        if (path == null || path.isEmpty()) return Double.POSITIVE_INFINITY;

        double time = 0.0;
        int n = path.size();

        for (int k = 0; k < n; k++) {
            int from = path.get(k);
            int to   = (k == n - 1) ? path.get(0) : path.get(k + 1);

            double dist = matrix[from][to];
            if (Double.isInfinite(dist)) return Double.POSITIVE_INFINITY;

            time += dist / speedMetersPerSec;

            if (to != depotIndex) {
                long idTo = vertexOrder.get(to);
                Long svc = serviceTimes.get(idTo);
                if (svc != null) time += svc;
            }
        }
        return time;
    }
}
