package com.agile.projet.controller;




import com.agile.projet.model.Noeud;
import com.agile.projet.model.PickupDeliveryModel;
import com.agile.projet.model.Tournee;
import com.agile.projet.utils.XmlPlanParser;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController  //telling java that this class is used for internet and requests trafic
@CrossOrigin(origins = "http://localhost:5173")
public class ApiController {

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    Controller controller;


    @GetMapping("/") //if a request goes to the root of our web site, it will be called (argument "/")
    public void index() throws Exception {
        controller.createPlan("petitPlan.xml");
        controller.createDeliveryFromXml("demandePetit1.xml");
        controller.computeShortestPaths();
        controller.findBestPath();
        var tournee = controller.findBestPath();

        // Print lisible en console
        System.out.println("=== Tournée optimale ===");
        System.out.println("Coût total : " + tournee.getTotalCost());
        int i = 1;
        for (var etape : tournee.getEtapes()) {
            System.out.printf("%2d. [%s] %-20s  leg=%.2f  cumul=%.2f (id=%d)%n",
                    i++, etape.getType(), etape.getLabel(), etape.getLegCost(), etape.getCumulativeCost(), etape.getId());
        }
        System.out.println("Hello World");


        return ;
    }

    
   @GetMapping("/plan-names")
public String[] getPlanNames() throws IOException {
    System.err.println("Getting plan names");

    Path plansDir = Paths.get("src/main/resources/plans");
    if (!Files.exists(plansDir) || !Files.isDirectory(plansDir)) {
        System.err.println("Plans directory not found: " + plansDir.toAbsolutePath());
        return new String[0];
    }

    return Files.list(plansDir)
            .filter(Files::isRegularFile)
            .map(path -> path.getFileName().toString())
            .toArray(String[]::new);
}

    @GetMapping("/request-names")
    public String[] getRequestNames() throws IOException {
        System.err.println("Getting request names");
        Path requestsDir = Paths.get("src/main/resources/requests");
        if (!Files.exists(requestsDir) || !Files.isDirectory(requestsDir)) {
            System.err.println("Requests directory not found: " + requestsDir.toAbsolutePath());
            return new String[0];
        }

        return Files.list(requestsDir)
                .filter(Files::isRegularFile)
                .map(path -> path.getFileName().toString())
                .toArray(String[]::new);
    }


    @GetMapping("/load-plan/{planName}")
    public void loadPlan(@PathVariable String planName) throws Exception {
        controller.createPlan(planName);
    }
    @GetMapping("/plans/{filename}")
    public ResponseEntity<String> getPlanFile(@PathVariable String filename) throws IOException {
        System.err.println("Fetching plan file: " + filename);

    // Spring Boot way : read from src/main/resources/plans/
        ClassPathResource resource = new ClassPathResource(filename);


    if (!resource.exists()) {
        System.err.println("File not found in resources/plans/");
        return ResponseEntity.notFound().build();
    }

    String content = new String(resource.getInputStream().readAllBytes());
    return ResponseEntity.ok(content);
    }
    @GetMapping("/requests/{filename}")
    public ResponseEntity<String> getRequestFile(@PathVariable String filename) throws IOException {
        System.err.println("Fetching request file: " + filename);
    // Spring Boot way : read from src/main/resources/requests/
        ClassPathResource resource = new ClassPathResource("requests/" + filename); 
    if (!resource.exists()) {
        System.err.println("File not found in resources/requests/");
        return ResponseEntity.notFound().build();
    }
    String content = new String(resource.getInputStream().readAllBytes());
    return ResponseEntity.ok(content);  
}
    


    
}