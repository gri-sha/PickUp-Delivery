package com.agile.projet.utils;

import com.agile.projet.model.DemandeDelivery;
import com.agile.projet.model.Delivery;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class XmlDeliveryParser {

    public void parse(String xmlFilePath, DemandeDelivery demande) throws Exception {
        InputStream is = getClass().getClassLoader().getResourceAsStream(xmlFilePath);
        if (is == null) {
            throw new FileNotFoundException("plan.xml not found");
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(is);

        doc.getDocumentElement().normalize();

        // Lire tous les noeuds <livraison>
        NodeList livraisonNodes = doc.getElementsByTagName("livraison");

        for (int i = 0; i < livraisonNodes.getLength(); i++) {
            Node node = livraisonNodes.item(i);

            if (node.getNodeType() == Node.ELEMENT_NODE) {
                Element e = (Element) node;

                // Lire les attributs de la livraison
                String adresseEnlevement = e.getAttribute("adresseEnlevement");
                String adresseLivraison  = e.getAttribute("adresseLivraison");
                String dureeEnlevement      = e.getAttribute("dureeEnlevement");
                String dureeLivraison       = e.getAttribute("dureeLivraison");

                // Créer un objet Delivery
                Delivery d = new Delivery(
                        Long.parseLong(adresseEnlevement),
                        Long.parseLong(adresseLivraison),
                        Long.parseLong(dureeEnlevement),
                        Long.parseLong(dureeLivraison)
                );

                // Ajouter à DemandeDelivery
                demande.addDelivery(d);
            }
        }
    }
}
