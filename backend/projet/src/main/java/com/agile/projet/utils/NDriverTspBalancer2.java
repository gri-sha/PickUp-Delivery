package com.agile.projet.utils;

import com.agile.projet.model.Delivery;
import com.agile.projet.model.DemandeDelivery;
import com.agile.projet.model.Entrepot;
import com.agile.projet.model.PickupDeliveryModel;

import java.util.*;

public class NDriverTspBalancer2 {

    public static final class StepTimeline {
        private final long nodeId;
        private final double travelFromPrevSec;
        private final double serviceSec;
        private final double cumulativeSec;

        public StepTimeline(long nodeId, double travelFromPrevSec, double serviceSec, double cumulativeSec) {
            this.nodeId = nodeId;
            this.travelFromPrevSec = travelFromPrevSec;
            this.serviceSec = serviceSec;
            this.cumulativeSec = cumulativeSec;
        }

        public long getNodeId() { return nodeId; }
        public double getTravelFromPrevSec() { return travelFromPrevSec; }
        public double getServiceSec() { return serviceSec; }
        public double getCumulativeSec() { return cumulativeSec; }
    }

    public static final class DriverTour {
        private final int driverIndex;
        private final List<Long> pathIds;
        private final double distanceMeters;
        private final double travelTimeSeconds;
        private final double serviceTimeSeconds;
        private final double totalTimeSeconds;
        private final List<StepTimeline> timeline;

        public DriverTour(int driverIndex,
                          List<Long> pathIds,
                          double distanceMeters,
                          double travelTimeSeconds,
                          double serviceTimeSeconds,
                          double totalTimeSeconds,
                          List<StepTimeline> timeline) {
            this.driverIndex = driverIndex;
            this.pathIds = pathIds;
            this.distanceMeters = distanceMeters;
            this.travelTimeSeconds = travelTimeSeconds;
            this.serviceTimeSeconds = serviceTimeSeconds;
            this.totalTimeSeconds = totalTimeSeconds;
            this.timeline = timeline;
        }

        public int getDriverIndex() { return driverIndex; }
        public List<Long> getPathIds() { return pathIds; }
        public double getDistanceMeters() { return distanceMeters; }
        public double getTravelTimeSeconds() { return travelTimeSeconds; }
        public double getServiceTimeSeconds() { return serviceTimeSeconds; }
        public double getTotalTimeSeconds() { return totalTimeSeconds; }
        public List<StepTimeline> getTimeline() { return timeline; }
    }

    public static final class NDriverSolution {
        private final List<DriverTour> tours;
        private final double targetSeconds;
        private final double globalTotalSeconds;

        public NDriverSolution(List<DriverTour> tours, double targetSeconds, double globalTotalSeconds) {
            this.tours = tours;
            this.targetSeconds = targetSeconds;
            this.globalTotalSeconds = globalTotalSeconds;
        }

        public List<DriverTour> getTours() { return tours; }
        public double getTargetSeconds() { return targetSeconds; }
        public double getGlobalTotalSeconds() { return globalTotalSeconds; }
    }

    /**
     * NOUVEAU PARAMÈTRE :
     * @param maxTimePerDriverSeconds temps max autorisé par driver (<=0 → ignoré)
     */
    public static NDriverSolution solve(PickupDeliveryModel model,
                                        int nDrivers,
                                        double speedMetersPerSec,
                                        double maxTimePerDriverSeconds) {

        if (model == null || model.getMatriceCout() == null || model.getVertexOrder() == null)
            throw new IllegalStateException("Modèle non initialisé");

        if (nDrivers <= 0)
            throw new IllegalArgumentException("nDrivers >= 1 requis");

        if (speedMetersPerSec <= 0)
            throw new IllegalArgumentException("Vitesse > 0 requise");

        DemandeDelivery demande = model.getDemandeDelivery();
        Entrepot entrepot = model.getEntrepot();
        long depotId = entrepot.getAdresse();

        double[][] globalCost = model.getMatriceCout().getCostMatrix();
        List<Long> globalVertexOrder = model.getVertexOrder();
        int[] globalPickup = model.getPickupOfDelivery();

        Map<Long, Long> serviceTimes = buildServiceTimeMap(demande);

        /* ===== TSP GLOBAL ===== */
        CalculTSP globalTsp = new CalculTSP(globalCost, globalVertexOrder, globalPickup);
        globalTsp.solveFromId(depotId);

        TourStats globalStats = computeTourStats(
                globalTsp, globalCost, globalVertexOrder,
                serviceTimes, 0, speedMetersPerSec
        );

        double globalTotal = globalStats.totalSeconds;
        double balancedTarget = globalTotal / nDrivers;

        /* ===== NOUVELLE LOGIQUE (SEULE MODIF) ===== */
        double effectiveTarget =
                (maxTimePerDriverSeconds > 0)
                        ? Math.min(balancedTarget, maxTimePerDriverSeconds)
                        : balancedTarget;

        effectiveTarget =maxTimePerDriverSeconds;

        List<Delivery> orderedDeliveries =
                orderDeliveriesByGlobalRoute(demande.getDeliveries(), globalTsp.getBestPathIds());

        LinkedList<Delivery> remaining = new LinkedList<>(orderedDeliveries);
        List<DriverTour> result = new ArrayList<>();

        int d = 1;
        while (d <= nDrivers || !remaining.isEmpty()) {

            // Si on a déjà tout assigné :
            // - on complète avec des tournées vides jusqu'à nDrivers (comportement inchangé)
            // - si on est au-delà de nDrivers, on sort.
            if (remaining.isEmpty()) {
                if (d <= nDrivers) {
                    result.add(new DriverTour(d, List.of(), 0, 0, 0, 0, List.of()));
                    d++;
                    continue;
                } else {
                    break;
                }
            }

            int k;

            // Ancien comportement conservé UNIQUEMENT si pas de limite max (<=0)
            if (d == nDrivers && maxTimePerDriverSeconds <= 0) {
                k = remaining.size();
            } else {
                // Sinon (y compris pour le "dernier driver demandé" s'il y a une limite),
                // on prend le plus gros préfixe qui respecte la cible
                k = pickMaxPrefixUnderTarget(
                        model, depotId, remaining,
                        effectiveTarget, speedMetersPerSec, serviceTimes
                );
                if (k <= 0) k = 1;
            }

            List<Delivery> assigned = new ArrayList<>();
            for (int i = 0; i < k; i++) assigned.add(remaining.removeFirst());

            SubTspData sub = buildSubProblem(globalCost, globalVertexOrder, depotId, assigned);
            int[] subPickup = buildSubPickupOfDelivery(globalPickup, globalVertexOrder, sub.vertexOrder);

            CalculTSP tsp = new CalculTSP(sub.costMatrix, sub.vertexOrder, subPickup);
            tsp.solveFromId(depotId);

            TourStats stats = computeTourStats(
                    tsp, sub.costMatrix, sub.vertexOrder,
                    serviceTimes, sub.depotIndex, speedMetersPerSec
            );

            result.add(new DriverTour(
                    d,
                    tsp.getBestPathIds(),
                    stats.distanceMeters,
                    stats.travelSeconds,
                    stats.serviceSeconds,
                    stats.totalSeconds,
                    stats.timeline
            ));

            d++;
        }


        return new NDriverSolution(result, effectiveTarget, globalTotal);
    }

    /* ==================== TOUT LE RESTE STRICTEMENT IDENTIQUE ==================== */

    private static Map<Long, Long> buildServiceTimeMap(DemandeDelivery demande) {
        Map<Long, Long> map = new HashMap<>();
        for (Delivery d : demande.getDeliveries()) {
            map.put(d.getAdresseEnlevement(), d.getDureeEnlevement());
            map.put(d.getAdresseLivraison(), d.getDureeLivraison());
        }
        return map;
    }

    private static List<Delivery> orderDeliveriesByGlobalRoute(List<Delivery> deliveries, List<Long> route) {
        Map<Long, Integer> pos = new HashMap<>();
        for (int i = 0; i < route.size(); i++) pos.putIfAbsent(route.get(i), i);

        deliveries.sort(Comparator.comparingInt(d ->
                Math.min(
                        pos.getOrDefault(d.getAdresseEnlevement(), Integer.MAX_VALUE),
                        pos.getOrDefault(d.getAdresseLivraison(), Integer.MAX_VALUE)
                )
        ));
        return deliveries;
    }

    private static final class SubTspData {
        final double[][] costMatrix;
        final List<Long> vertexOrder;
        final int depotIndex;
        SubTspData(double[][] m, List<Long> v, int d) {
            costMatrix = m; vertexOrder = v; depotIndex = d;
        }
    }

    private static SubTspData buildSubProblem(double[][] globalCost,
                                              List<Long> globalVertexOrder,
                                              long depotId,
                                              List<Delivery> deliveries) {

        LinkedHashSet<Long> ids = new LinkedHashSet<>();
        ids.add(depotId);
        for (Delivery d : deliveries) {
            ids.add(d.getAdresseEnlevement());
            ids.add(d.getAdresseLivraison());
        }

        Map<Long, Integer> gIndex = new HashMap<>();
        for (int i = 0; i < globalVertexOrder.size(); i++)
            gIndex.put(globalVertexOrder.get(i), i);

        List<Long> local = new ArrayList<>(ids);
        int n = local.size();
        double[][] m = new double[n][n];

        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                m[i][j] = globalCost[gIndex.get(local.get(i))][gIndex.get(local.get(j))];

        return new SubTspData(m, local, 0);
    }

    private static int[] buildSubPickupOfDelivery(int[] globalPickup,
                                                  List<Long> globalOrder,
                                                  List<Long> subOrder) {

        int[] sub = new int[subOrder.size()];
        Arrays.fill(sub, -1);

        Map<Long, Integer> gIndex = new HashMap<>();
        for (int i = 0; i < globalOrder.size(); i++)
            gIndex.put(globalOrder.get(i), i);

        Map<Long, Integer> lIndex = new HashMap<>();
        for (int i = 0; i < subOrder.size(); i++)
            lIndex.put(subOrder.get(i), i);

        for (int i = 0; i < subOrder.size(); i++) {
            int g = gIndex.get(subOrder.get(i));
            int gp = globalPickup[g];
            if (gp >= 0) {
                Integer lp = lIndex.get(globalOrder.get(gp));
                if (lp != null) sub[i] = lp;
            }
        }
        return sub;
    }

    private static final class TourStats {
        final double distanceMeters, travelSeconds, serviceSeconds, totalSeconds;
        final List<StepTimeline> timeline;
        TourStats(double d, double t, double s, double tot, List<StepTimeline> tl) {
            distanceMeters = d; travelSeconds = t; serviceSeconds = s;
            totalSeconds = tot; timeline = tl;
        }
    }

    private static TourStats computeTourStats(CalculTSP tsp,
                                              double[][] matrix,
                                              List<Long> order,
                                              Map<Long, Long> serviceTimes,
                                              int depotIndex,
                                              double speed) {

        List<Integer> path = tsp.getBestPathIndices();
        double dist = 0, travel = 0, service = 0, cumul = 0;
        List<StepTimeline> tl = new ArrayList<>();

        tl.add(new StepTimeline(order.get(path.get(0)), 0, 0, 0));

        for (int i = 0; i < path.size(); i++) {
            int from = path.get(i);
            int to = (i == path.size() - 1) ? path.get(0) : path.get(i + 1);

            double d = matrix[from][to];
            double t = d / speed;
            dist += d;
            travel += t;
            cumul += t;

            double s = 0;
            if (to != depotIndex && serviceTimes.containsKey(order.get(to))) {
                s = serviceTimes.get(order.get(to));
                service += s;
                cumul += s;
            }

            tl.add(new StepTimeline(order.get(to), t, s, cumul));
        }

        return new TourStats(dist, travel, service, travel + service, tl);
    }

    private static int pickMaxPrefixUnderTarget(PickupDeliveryModel model,
                                                long depotId,
                                                List<Delivery> remaining,
                                                double target,
                                                double speed,
                                                Map<Long, Long> serviceTimes) {

        for (int k = remaining.size(); k >= 1; k--) {
            SubTspData sub = buildSubProblem(
                    model.getMatriceCout().getCostMatrix(),
                    model.getVertexOrder(),
                    depotId,
                    remaining.subList(0, k)
            );

            int[] pickup = buildSubPickupOfDelivery(
                    model.getPickupOfDelivery(),
                    model.getVertexOrder(),
                    sub.vertexOrder
            );

            CalculTSP tsp = new CalculTSP(sub.costMatrix, sub.vertexOrder, pickup);
            tsp.solveFromId(depotId);

            TourStats stats = computeTourStats(
                    tsp, sub.costMatrix, sub.vertexOrder,
                    serviceTimes, sub.depotIndex, speed
            );

            if (stats.totalSeconds <= target) return k;
        }
        return 0;
    }
}
