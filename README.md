# PickUp-Delivery

Application de gestion de livraisons avec optimisation de tournées (TSP) multi-coursiers.

## Architecture
- **Backend**: Spring Boot (Java) - Calcul des plus courts chemins et résolution TSP
- **Frontend**: React + TypeScript + Leaflet - Visualisation cartographique
- **API Python**: Solver TSP alternatif (optionnel)

## Correction Bug d'Indexation

### Problème
Les indices des enlèvements et livraisons n'étaient pas séquentiels :
- Au lieu de : `Enlèvement 1 → Livraison 1 → Enlèvement 2 → Livraison 2`
- On avait : `Enlèvement 1 → Enlèvement 3 → Livraison 4` (avec des sauts)

### Cause
Dans `Controller.buildLabel()`, on utilisait l'ID du noeud (qui peut être 4150019167, etc.) au lieu d'un indice séquentiel basé sur la position de la demande de livraison dans la liste.

### Solution
Modification de la méthode `buildLabel()` pour :
1. Parcourir la liste des `Delivery` du modèle
2. Trouver l'indice (position) de la demande correspondante
3. Utiliser cet indice (base 1) pour afficher : `"Enlèvement 1"`, `"Livraison 1"`, etc.

**Fichier modifié**: `backend/projet/src/main/java/com/agile/projet/controller/Controller.java`

```java
private String buildLabel(String type, long id) {
    if (type.equals("DEPOT")) {
        return "Dépôt";
    }
    
    // Trouver l'indice de la demande (base 1) pour ce noeud
    List<Delivery> deliveries = pickupDeliveryModel.demandeDelivery.getDeliveries();
    for (int i = 0; i < deliveries.size(); i++) {
        Delivery d = deliveries.get(i);
        if (type.equals("PICKUP") && d.getAdresseEnlevement() == id) {
            return "Enlèvement " + (i + 1);  // Indice séquentiel !
        }
        if (type.equals("DELIVERY") && d.getAdresseLivraison() == id) {
            return "Livraison " + (i + 1);   // Indice séquentiel !
        }
    }
    
    return "Noeud " + id;
}
```

## Lancement

### Backend
```bash
cd backend/projet
./mvnw spring-boot:run
```

### Frontend
```bash
cd frontend
npm install
npm run dev
```

L'application sera accessible sur `http://localhost:5173`
