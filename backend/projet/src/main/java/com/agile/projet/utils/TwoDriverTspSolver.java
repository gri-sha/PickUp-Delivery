package com.agile.projet.utils;

import com.agile.projet.model.Delivery;
import com.agile.projet.model.DemandeDelivery;
import com.agile.projet.model.Entrepot;
import com.agile.projet.model.PickupDeliveryModel;

import java.util.*;

/**
 * Calcule une répartition sur 2 livreurs à partir de la matrice de coûts et du modèle Pickup&Delivery.
 *
 * Idée :
 *  1. TSP global sur toutes les demandes (1 livreur).
 *  2. On ordonne les demandes dans l'ordre de cette tournée.
 *  3. On prend un préfixe de demandes pour le driver 1 (pickup+delivery ensemble),
 *     on calcule un TSP juste sur ce sous-ensemble et on vérifie son temps.
 *  4. Si la durée dépasse la limite, on raccourcit le préfixe.
 *  5. Le reste des demandes va au driver 2 (TSP séparé pour lui).
 */
public class TwoDriverTspSolver {

    public static final class TwoDriverSolution {
        private final List<Long> driver1PathIds;
        private final List<Long> driver2PathIds;
        private final double driver1DurationSeconds;
        private final double driver2DurationSeconds;



        public TwoDriverSolution(List<Long> d1, List<Long> d2,
                                 double t1, double t2) {
            this.driver1PathIds = d1;
            this.driver2PathIds = d2;
            this.driver1DurationSeconds = t1;
            this.driver2DurationSeconds = t2;
        }

        public List<Long> getDriver1PathIds() { return driver1PathIds; }
        public List<Long> getDriver2PathIds() { return driver2PathIds; }
        public double getDriver1DurationSeconds() { return driver1DurationSeconds; }
        public double getDriver2DurationSeconds() { return driver2DurationSeconds; }
    }

    /**
     * Calcule une solution à 2 livreurs.
     *
     * @param model            PickupDeliveryModel déjà rempli (plan, demandes, matrices).
     * @param maxDurationSec   durée max pour une tournée (en secondes), ex: 3600 pour 1h.
     * @param speedMetersPerSec vitesse constante des livreurs (m/s).
     */

    public static TwoDriverSolution solveForTwoDrivers(PickupDeliveryModel model,
                                                       double maxDurationSec,
                                                       double speedMetersPerSec) {

        int[] pickupOfDelivery = model.getPickupOfDelivery();
        if (model == null || model.getMatriceCout() == null || model.getVertexOrder() == null) {
            throw new IllegalStateException("PickupDeliveryModel ou matrice non initialisés");
        }
        if (speedMetersPerSec <= 0) {
            throw new IllegalArgumentException("La vitesse doit être > 0");
        }

        double[][] globalCost = model.getMatriceCout().getCostMatrix();
        List<Long> globalVertexOrder = model.getVertexOrder();
        DemandeDelivery demande = model.getDemandeDelivery();
        if (demande == null || demande.getDeliveries().isEmpty()) {
            // Pas de demandes : tournée vide pour les deux.
            return new TwoDriverSolution(List.of(), List.of(), 0.0, 0.0);
        }

        Entrepot entrepot = model.getEntrepot();
        if (entrepot == null || entrepot.getAdresse() == null) {
            throw new IllegalStateException("Entrepôt non défini dans le modèle");
        }
        long depotId = entrepot.getAdresse();

        // 1) Tournée globale (un seul livreur) pour obtenir un ordre "pseudo-optimisé"
        CalculTSP globalTsp = new CalculTSP(globalCost, globalVertexOrder);
        globalTsp.solveFromId(depotId);
        List<Long> globalRouteIds = globalTsp.getBestPathIds();

        // 2) Ordre des demandes (Delivery) dans cette tournée
        List<Delivery> deliveriesOrdered = orderDeliveriesByGlobalRoute(demande.getDeliveries(), globalRouteIds);

        // 3) Pré-calcul des temps de service par ID de noeud
        Map<Long, Long> serviceTimes = buildServiceTimeMap(demande);

        // 4) On essaie de donner le "plus possible" au driver 1 en respectant maxDurationSec
        TwoDriverSolution best = null;

        for (int k = deliveriesOrdered.size(); k >= 0; k--) {
            // demandes [0..k-1] pour driver 1, [k..end] pour driver 2
            List<Delivery> d1 = deliveriesOrdered.subList(0, k);
            List<Delivery> d2 = deliveriesOrdered.subList(k, deliveriesOrdered.size());

            // Construire les listes d'IDs (depot + tous pickups/deliveries correspondants)
            SubTspData sub1 = buildSubProblem(globalCost, globalVertexOrder, depotId, d1);
            if (sub1.vertexOrder.size() <= 1) {
                // Rien ou seulement le dépôt -> temps 0 pour le driver 1
                // On teste quand même une solution "driver 1 vide".
                double t1 = 0.0;

                SubTspData sub2 = buildSubProblem(globalCost, globalVertexOrder, depotId, d2);
                List<Long> path2 = List.of();
                double t2 = 0.0;
                if (sub2.vertexOrder.size() > 1) {
                    CalculTSP tsp2 = new CalculTSP(sub2.costMatrix, sub2.vertexOrder);
                    tsp2.solveFromId(depotId);
                    path2 = tsp2.getBestPathIds();
                    t2 = computeTourDurationSeconds(tsp2, sub2.costMatrix, sub2.vertexOrder,
                            serviceTimes, sub2.depotIndex, speedMetersPerSec);
                }

                best = new TwoDriverSolution(List.of(), path2, t1, t2);
                break;
            }

            // TSP pour le driver 1
            CalculTSP tsp1 = new CalculTSP(sub1.costMatrix, sub1.vertexOrder);
            tsp1.solveFromId(depotId);
            List<Long> path1 = tsp1.getBestPathIds();
            if (path1.isEmpty()) {
                continue; // pas de circuit valable
            }
            double duration1 = computeTourDurationSeconds(tsp1, sub1.costMatrix, sub1.vertexOrder,
                    serviceTimes, sub1.depotIndex, speedMetersPerSec);

            if (duration1 <= maxDurationSec) {
                // OK pour le driver 1, on calcule la tournée du driver 2
                SubTspData sub2 = buildSubProblem(globalCost, globalVertexOrder, depotId, d2);
                List<Long> path2 = List.of();
                double duration2 = 0.0;

                if (sub2.vertexOrder.size() > 1) {
                    CalculTSP tsp2 = new CalculTSP(sub2.costMatrix, sub2.vertexOrder);
                    tsp2.solveFromId(depotId);
                    path2 = tsp2.getBestPathIds();
                    duration2 = computeTourDurationSeconds(tsp2, sub2.costMatrix, sub2.vertexOrder,
                            serviceTimes, sub2.depotIndex, speedMetersPerSec);
                }
                best = new TwoDriverSolution(path1, path2, duration1, duration2);
                break; // comme on parcourt k de n → 0, le premier OK maximise la charge du driver 1
            }
        }

        // Si on n'a jamais trouvé de découpage <= 1h, on renvoie tout sur driver 2 (ou tout driver 1)
        if (best == null) {
            // Cas simple : on met toutes les demandes sur le driver 2
            SubTspData sub2 = buildSubProblem(globalCost, globalVertexOrder, depotId, deliveriesOrdered);
            CalculTSP tsp2 = new CalculTSP(sub2.costMatrix, sub2.vertexOrder);
            tsp2.solveFromId(depotId);
            List<Long> path2 = tsp2.getBestPathIds();
            double t2 = computeTourDurationSeconds(tsp2, sub2.costMatrix, sub2.vertexOrder,
                    serviceTimes, sub2.depotIndex, speedMetersPerSec);
            return new TwoDriverSolution(List.of(), path2, 0.0, t2);
        }

        return best;
    }

    public static TwoDriverSolution solveForTwoDrivers2(PickupDeliveryModel model,
                                                       double maxDurationSec,
                                                       double speedMetersPerSec) {

        int[] pickupOfDelivery = model.getPickupOfDelivery();
        if (model == null || model.getMatriceCout() == null || model.getVertexOrder() == null) {
            throw new IllegalStateException("PickupDeliveryModel ou matrice non initialisés");
        }
        if (speedMetersPerSec <= 0) {
            throw new IllegalArgumentException("La vitesse doit être > 0");
        }

        double[][] globalCost = model.getMatriceCout().getCostMatrix();
        List<Long> globalVertexOrder = model.getVertexOrder();
        DemandeDelivery demande = model.getDemandeDelivery();
        if (demande == null || demande.getDeliveries().isEmpty()) {
            // Pas de demandes : tournée vide pour les deux.
            return new TwoDriverSolution(List.of(), List.of(), 0.0, 0.0);
        }

        Entrepot entrepot = model.getEntrepot();
        if (entrepot == null || entrepot.getAdresse() == null) {
            throw new IllegalStateException("Entrepôt non défini dans le modèle");
        }
        long depotId = entrepot.getAdresse();

        // 1) Tournée globale (un seul livreur) pour obtenir un ordre "pseudo-optimisé"


        CalculTSP globalTsp = new CalculTSP(globalCost, globalVertexOrder);
        globalTsp.solveFromId(depotId);
        List<Long> globalRouteIds = globalTsp.getBestPathIds();

        // 2) Ordre des demandes (Delivery) dans cette tournée
        List<Delivery> deliveriesOrdered = orderDeliveriesByGlobalRoute(demande.getDeliveries(), globalRouteIds);

        // 3) Pré-calcul des temps de service par ID de noeud
        Map<Long, Long> serviceTimes = buildServiceTimeMap(demande);

        // 4) On essaie de donner le "plus possible" au driver 1 en respectant maxDurationSec
        TwoDriverSolution best = null;

        for (int k = deliveriesOrdered.size(); k >= 0; k--) {
            // demandes [0..k-1] pour driver 1, [k..end] pour driver 2
            List<Delivery> d1 = deliveriesOrdered.subList(0, k);
            List<Delivery> d2 = deliveriesOrdered.subList(k, deliveriesOrdered.size());

            // Construire les listes d'IDs (depot + tous pickups/deliveries correspondants)
            SubTspData sub1 = buildSubProblem(globalCost, globalVertexOrder, depotId, d1);
            if (sub1.vertexOrder.size() <= 1) {
                // Rien ou seulement le dépôt -> temps 0 pour le driver 1
                // On teste quand même une solution "driver 1 vide".
                double t1 = 0.0;

                SubTspData sub2 = buildSubProblem(globalCost, globalVertexOrder, depotId, d2);
                List<Long> path2 = List.of();
                double t2 = 0.0;
                if (sub2.vertexOrder.size() > 1) {
                    int[] subPickup2 = buildSubPickupOfDelivery(
                            model.getPickupOfDelivery(),
                            model.getVertexOrder(),
                            sub2.vertexOrder
                    );
                    CalculTSP tsp2 = new CalculTSP(sub2.costMatrix, sub2.vertexOrder,subPickup2);
                    tsp2.solveFromId(depotId);
                    path2 = tsp2.getBestPathIds();
                    t2 = computeTourDurationSeconds(tsp2, sub2.costMatrix, sub2.vertexOrder,
                            serviceTimes, sub2.depotIndex, speedMetersPerSec);
                }

                best = new TwoDriverSolution(List.of(), path2, t1, t2);
                break;
            }

            // TSP pour le driver 1
            int[] subPickup = buildSubPickupOfDelivery(
                    model.getPickupOfDelivery(),
                    model.getVertexOrder(),
                    sub1.vertexOrder
            );
            CalculTSP tsp1 = new CalculTSP(sub1.costMatrix, sub1.vertexOrder,subPickup);
            tsp1.solveFromId(depotId);
            List<Long> path1 = tsp1.getBestPathIds();
            if (path1.isEmpty()) {
                continue; // pas de circuit valable
            }
            double duration1 = computeTourDurationSeconds(tsp1, sub1.costMatrix, sub1.vertexOrder,
                    serviceTimes, sub1.depotIndex, speedMetersPerSec);

            if (duration1 <= maxDurationSec) {
                // OK pour le driver 1, on calcule la tournée du driver 2
                SubTspData sub2 = buildSubProblem(globalCost, globalVertexOrder, depotId, d2);
                List<Long> path2 = List.of();
                double duration2 = 0.0;

                if (sub2.vertexOrder.size() > 1) {
                    int[] subPickup2 = buildSubPickupOfDelivery(
                            model.getPickupOfDelivery(),
                            model.getVertexOrder(),
                            sub2.vertexOrder
                    );
                    CalculTSP tsp2 = new CalculTSP(sub2.costMatrix, sub2.vertexOrder,subPickup2);
                    tsp2.solveFromId(depotId);
                    path2 = tsp2.getBestPathIds();
                    duration2 = computeTourDurationSeconds(tsp2, sub2.costMatrix, sub2.vertexOrder,
                            serviceTimes, sub2.depotIndex, speedMetersPerSec);
                }
                best = new TwoDriverSolution(path1, path2, duration1, duration2);
                break; // comme on parcourt k de n → 0, le premier OK maximise la charge du driver 1
            }
        }

        // Si on n'a jamais trouvé de découpage <= 1h, on renvoie tout sur driver 2 (ou tout driver 1)
        if (best == null) {
            // Cas simple : on met toutes les demandes sur le driver 2
            SubTspData sub2 = buildSubProblem(globalCost, globalVertexOrder, depotId, deliveriesOrdered);
            int[] subPickup2 = buildSubPickupOfDelivery(
                    model.getPickupOfDelivery(),
                    model.getVertexOrder(),
                    sub2.vertexOrder
            );
            CalculTSP tsp2 = new CalculTSP(sub2.costMatrix, sub2.vertexOrder,subPickup2);
            tsp2.solveFromId(depotId);
            List<Long> path2 = tsp2.getBestPathIds();
            double t2 = computeTourDurationSeconds(tsp2, sub2.costMatrix, sub2.vertexOrder,
                    serviceTimes, sub2.depotIndex, speedMetersPerSec);
            return new TwoDriverSolution(List.of(), path2, 0.0, t2);
        }

        return best;
    }

    // ---------- Structures internes ----------

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

    // ---------- Helpers ----------

    /** Ordre des Delivery en fonction de leur première apparition (pickup ou delivery) dans la tournée globale. */
    private static List<Delivery> orderDeliveriesByGlobalRoute(List<Delivery> deliveries,
                                                               List<Long> globalRouteIds) {
        Map<Long, Integer> position = new HashMap<>();
        for (int i = 0; i < globalRouteIds.size(); i++) {
            long id = globalRouteIds.get(i);
            // on garde la première occurrence
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
     * Construit un sous-problème TSP : matrice & vertexOrder restreints au dépôt + toutes les
     * adresses (pickup & delivery) d'une liste de Delivery.
     *
     * On garantit qu'un driver qui prend un pickup prend aussi la livraison, car on ajoute toujours
     * les deux adresses pour chaque Delivery.
     */
    private static SubTspData buildSubProblem(double[][] globalCost,
                                              List<Long> globalVertexOrder,
                                              long depotId,
                                              List<Delivery> deliveries) {
        // Ensemble d'IDs à inclure
        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(depotId);
        for (Delivery d : deliveries) {
            if (d.getAdresseEnlevement() != null) ids.add(d.getAdresseEnlevement());
            if (d.getAdresseLivraison() != null) ids.add(d.getAdresseLivraison());
        }

        // Map ID -> index global
        Map<Long, Integer> idToGlobalIndex = new HashMap<>();
        for (int i = 0; i < globalVertexOrder.size(); i++) {
            idToGlobalIndex.put(globalVertexOrder.get(i), i);
        }

        // Construire vertexOrder local (dépot en premier)
        List<Long> localVertexOrder = new ArrayList<>();
        localVertexOrder.add(depotId);
        for (Long id : ids) {
            if (!Objects.equals(id, depotId)) {
                localVertexOrder.add(id);
            }
        }

        int n = localVertexOrder.size();
        double[][] subMatrix = new double[n][n];

        for (int i = 0; i < n; i++) {
            Long idFrom = localVertexOrder.get(i);
            Integer gi = idToGlobalIndex.get(idFrom);
            if (gi == null) {
                // pas dans la matrice globale -> on met tout à INF
                Arrays.fill(subMatrix[i], Double.POSITIVE_INFINITY);
                continue;
            }
            for (int j = 0; j < n; j++) {
                Long idTo = localVertexOrder.get(j);
                Integer gj = idToGlobalIndex.get(idTo);
                if (gj == null) {
                    subMatrix[i][j] = Double.POSITIVE_INFINITY;
                } else {
                    subMatrix[i][j] = globalCost[gi][gj];
                }
            }
        }

        int depotIndex = 0; // on a forcé le dépôt à l'indice 0
        return new SubTspData(subMatrix, localVertexOrder, depotIndex);
    }

    /**
     * Calcule la durée d'une tournée TSP (cycle) en secondes :
     * somme(distance)/vitesse + somme(temps service sur chaque arrêt sauf le dépôt initial).
     */
    private static double computeTourDurationSeconds(CalculTSP solver,
                                                     double[][] matrix,
                                                     List<Long> vertexOrder,
                                                     Map<Long, Long> serviceTimes,
                                                     int depotIndex,
                                                     double speedMetersPerSec) {

        List<Integer> path = solver.getBestPathIndices();
        if (path == null || path.isEmpty()) {
            return Double.POSITIVE_INFINITY;
        }
        double time = 0.0;
        int n = path.size();

        for (int k = 0; k < n; k++) {
            int from = path.get(k);
            int to   = (k == n - 1) ? path.get(0) : path.get(k + 1);
            double dist = matrix[from][to];
            if (Double.isInfinite(dist)) {
                return Double.POSITIVE_INFINITY;
            }
            time += dist / speedMetersPerSec;

            // temps de service sur le noeud d'arrivée (sauf si c'est le dépôt)
            if (to != depotIndex) {
                long idTo = vertexOrder.get(to);
                Long svc = serviceTimes.get(idTo);
                if (svc != null) {
                    time += svc;
                }
            }
        }
        return time;
    }


    private static int[] buildSubPickupOfDelivery(
            int[] globalPickupOfDelivery,
            List<Long> globalVertexOrder,
            List<Long> subVertexOrder
    ) {
        int n = subVertexOrder.size();
        int[] subPickup = new int[n];
        Arrays.fill(subPickup, -1);

        // ID -> global index
        Map<Long, Integer> globalIndex = new HashMap<>();
        for (int i = 0; i < globalVertexOrder.size(); i++) {
            globalIndex.put(globalVertexOrder.get(i), i);
        }

        // ID -> local index
        Map<Long, Integer> localIndex = new HashMap<>();
        for (int i = 0; i < subVertexOrder.size(); i++) {
            localIndex.put(subVertexOrder.get(i), i);
        }

        // parcourir chaque delivery possible du sous-TSP
        for (int localD = 0; localD < n; localD++) {

            long idD = subVertexOrder.get(localD);
            Integer globalD = globalIndex.get(idD);
            if (globalD == null) continue;

            int globalP = globalPickupOfDelivery[globalD];
            if (globalP < 0) continue;

            // quel est l'ID du pickup correspondant ?
            long idPickup = globalVertexOrder.get(globalP);

            // existe-t-il dans le sous-problème ?
            Integer localP = localIndex.get(idPickup);
            if (localP != null) {
                // → contrainte valable dans le sous-TSP
                subPickup[localD] = localP;
            }
        }

        return subPickup;
    }

}
