package com.agile.projet.controller;

import com.agile.projet.model.Noeud;
import com.agile.projet.model.PickupDeliveryModel;
import com.agile.projet.model.Tournee;
import com.agile.projet.utils.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;


@Component
public class Controller {
    public final PickupDeliveryModel pickupDeliveryModel = new PickupDeliveryModel();
    private Tournee tournee;

    public Controller() throws Exception { }

    public void createPlan(String planXml) {
        pickupDeliveryModel.createPlan(planXml);
        pickupDeliveryModel.plan.printNoeuds();
        pickupDeliveryModel.plan.printTroncons();
    }

    public void printMatriceChemins() {
        System.out.println(pickupDeliveryModel.getMatriceChemins().toString());
    }




    public void computeShortestPaths() {
        if (pickupDeliveryModel.plan == null) {
            throw new IllegalStateException("Plan manquant : appelez createPlan(...) d'abord.");
        }
        if (pickupDeliveryModel.demandeDelivery == null) {
            throw new IllegalStateException("DemandeDelivery manquante : appelez createDeliveryFromXml(...) d'abord.");
        }

        CalculPlusCoursChemins calculPlusCoursChemins = new CalculPlusCoursChemins();
        // Cette méthode doit remplir : model.setVertexOrder(...); model.setMatriceCout(...);
        calculPlusCoursChemins.computeAstar(
                pickupDeliveryModel.plan,
                pickupDeliveryModel.demandeDelivery,
                pickupDeliveryModel
        );
    }

    public void createDeliveryFromXml(String deliveryPlanXml) throws Exception {
        pickupDeliveryModel.createDelivery(deliveryPlanXml);
        pickupDeliveryModel.demandeDelivery.printDeliveries();
    }

    public Tournee findBestPath() {
        var entrepot = pickupDeliveryModel.getEntrepot();
        if (entrepot == null) throw new IllegalStateException("Entrepôt manquant dans le modèle.");

        var mc = pickupDeliveryModel.getMatriceCout();
        if (mc == null) throw new IllegalStateException("matriceCout manquante : appelez computeShortestPaths() d'abord.");
        double[][] costMatrix = mc.getCostMatrix();

        List<Long> vertexOrder = pickupDeliveryModel.getVertexOrder();
        if (vertexOrder == null) throw new IllegalStateException("vertexOrder manquant dans le modèle.");
        if (vertexOrder.size() != costMatrix.length)
            throw new IllegalStateException("vertexOrder.size != costMatrix.length");

        long depotId = entrepot.getAdresse();
        if (!vertexOrder.contains(depotId))
            throw new IllegalStateException("L'ID dépôt n'est pas présent dans vertexOrder.");

        // --- Résolution avec TON TSP (sans contrainte interne) ---
        CalculTSP tsp = new CalculTSP(costMatrix, vertexOrder);
        tsp.solveFromId(depotId);

        List<Integer> pathIdx = tsp.getBestPathIndices();
        if (pathIdx.isEmpty())
            throw new IllegalStateException("Aucune tournée trouvée (chemins manquants ?)");

        // --- Récupérer la contrainte indices (construite dans computeAstar) ---
        int[] pickupOfDelivery = pickupDeliveryModel.getPickupOfDelivery();
        if (pickupOfDelivery != null) {
            int startIndex = vertexOrder.indexOf(depotId);
            pathIdx = enforcePrecedence(pathIdx, pickupOfDelivery, startIndex);
        }

        // Fermer le cycle pour l’affichage
        List<Integer> closed = new java.util.ArrayList<>(pathIdx);
        if (!closed.isEmpty()) closed.add(closed.get(0));

        // Construire la tournée (type/label + coût par tronçon et cumul)
        java.util.List<Tournee.Etape> etapes = new java.util.ArrayList<>();
        double cumul = 0.0;
        for (int k = 0; k < closed.size(); k++) {
            int idx = closed.get(k);
            long id = vertexOrder.get(idx);

            String type = resolveType(id);
            String label = buildLabel(type, id);

            double leg = 0.0;
            if (k > 0) {
                int prevIdx = closed.get(k - 1);
                leg = costMatrix[prevIdx][idx];
                cumul += leg;
            }
            etapes.add(new Tournee.Etape(id, type, label, leg, cumul));
        }

        // Recalcul du coût total sur la tournée réparée (pour cohérence d’affichage)
        double total = 0.0;
        for (int i = 1; i < closed.size(); i++) {
            total += costMatrix[closed.get(i-1)][closed.get(i)];
        }

        this.tournee = new Tournee(total, etapes);

        return new Tournee(total, etapes);
    }
    /**
     * Répare la séquence d’indices pour imposer "pickup -> delivery".
     * Si une livraison D apparaît avant son pickup P, on déplace P juste avant D.
     * On essaye de conserver le départ (startIdx) en tête.
     */
    private List<Integer> enforcePrecedence(List<Integer> pathIdx, int[] pickupOfDelivery, int startIdx) {
        List<Integer> seq = new java.util.ArrayList<>(pathIdx);

        // Mettre startIdx en tête (rotation), si présent
        int posStart = seq.indexOf(startIdx);
        if (posStart > 0) {
            java.util.Collections.rotate(seq, -posStart);
        }

        int n = seq.size();
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) pos[seq.get(i)] = i;

        boolean changed;
        int guard = 0;
        do {
            changed = false;
            for (int d = 0; d < n; d++) {
                int p = pickupOfDelivery[d];
                if (p == -1) continue; // d n'est pas une livraison
                int posD = pos[d];
                int posP = pos[p];
                if (posP > posD) {
                    // Déplacer le pickup P avant la livraison D
                    seq.remove(posP);
                    seq.add(posD, p);

                    // Recalcul des positions pour le segment affecté
                    int from = Math.min(posD, posP);
                    int to   = Math.max(posD, posP);
                    for (int i = from; i <= to; i++) pos[seq.get(i)] = i;

                    changed = true;
                }
            }
            guard++;
        } while (changed && guard < n); // sécurité

        // Remettre la rotation inverse si on avait déplacé le start en tête
        if (posStart > 0) {
            java.util.Collections.rotate(seq, posStart);
        }
        return seq;
    }


    public void getTrajetAvecTouteEtapes(){
        MatriceChemins matriceChemins = pickupDeliveryModel.getMatriceChemins();


    }

    public List<Long> buildFullPath() {
        MatriceChemins matrice = pickupDeliveryModel.getMatriceChemins();
        List<Long> fullPath = new ArrayList<>();
        tournee.getEtapes().get(0).getId();

        // Ajouter le premier point
        fullPath.add(tournee.getEtapes().get(0).getId());

        int size = tournee.getEtapes().size();
        // Pour chaque paire consécutive dans la tournée
        for (int i = 0; i < size - 1; i++) {
            Long from = tournee.getEtapes().get(i).getId();
            Long to = tournee.getEtapes().get(i+1).getId();

            Noeud fromNoeud = pickupDeliveryModel.plan.getNoeud(from);
            Noeud toNoeud = pickupDeliveryModel.plan.getNoeud(to);
            NodePair pair = new NodePair(fromNoeud, toNoeud);
            List<Noeud> partialPath = matrice.getCheminMatrix().get(pair);

            if (partialPath == null) {
                throw new RuntimeException("Pas de chemin trouvé entre " + from + " et " + to);
            }

            // Ajouter le chemin sauf le premier élément pour éviter les doublons
            for (int j = 1; j < partialPath.size(); j++) {
                fullPath.add(partialPath.get(j).getId());
            }
        }

        return fullPath;
    }

    private String resolveType(long id) {
        if (pickupDeliveryModel.getEntrepot() != null
                && pickupDeliveryModel.getEntrepot().getAdresse() == id) {
            return "DEPOT";
        }
        for (var d : pickupDeliveryModel.demandeDelivery.getDeliveries()) {
            if (d.getAdresseEnlevement() == id) return "PICKUP";
            if (d.getAdresseLivraison() == id) return "DELIVERY";
        }
        return "UNKNOWN";
    }

    private String buildLabel(String type, long id) {
        return switch (type) {
            case "DEPOT" -> "Dépôt";
            case "PICKUP" -> "Enlèvement " + id;
            case "DELIVERY" -> "Livraison " + id;
            default -> "Noeud " + id;
        };

    }

    public void solveTwoDriverTspExample() {
        double speed = 15000.0 / 3600.0;
        double maxDurationSec = 6000.0; // 1 heure

        TwoDriverTspSolver.TwoDriverSolution sol = TwoDriverTspSolver.solveForTwoDrivers(pickupDeliveryModel, maxDurationSec, speed);
        List<Long> driver1Route = sol.getDriver1PathIds();
        List<Long> driver2Route = sol.getDriver2PathIds();
        System.out.println("Driver 1 route: " + driver1Route);
        System.out.println("Driver 2 route: " + driver2Route);
        double t1Sec = sol.getDriver1DurationSeconds();
        double t2Sec = sol.getDriver2DurationSeconds();
    }
}
