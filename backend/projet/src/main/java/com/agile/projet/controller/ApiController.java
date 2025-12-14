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
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController  //telling java that this class is used for internet and requests trafic
@CrossOrigin(origins = "http://localhost:5173")
public class ApiController {
    private static final Logger log = LoggerFactory.getLogger(ApiController.class);

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    Controller controller;


    @GetMapping("/") //if a request goes to the root of our web site, it will be called (argument "/")
    public void index() throws Exception {
        controller.createPlan("moyenPlan.xml");
        controller.createDeliveryFromXml("demandeMoyen5.xml");
        controller.computeShortestPaths();
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

    // New endpoint: accepts uploaded XML files (plan + request) and returns full TSP path
    @PostMapping(path = "/get-tsp", consumes = MediaType.MULTIPART_FORM_DATA_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> getTspFromFiles(
            @RequestPart("plan") MultipartFile planXml,
            @RequestPart("request") MultipartFile requestXml
    ) throws Exception {
        System.out.println("getTspFromFiles called");
        log.info("Received plan file: {}", planXml != null ? planXml.getOriginalFilename() : "<null>");
        log.info("Received request file: {}", requestXml != null ? requestXml.getOriginalFilename() : "<null>");
        if (planXml == null || planXml.isEmpty()) {
            throw new IllegalArgumentException("Le fichier de plan XML est requis.");
        }
        if (requestXml == null || requestXml.isEmpty()) {
            throw new IllegalArgumentException("Le fichier de demande XML est requis.");
        }

        // Save under target/classes/plans and target/classes/requests so classpath lookups can find them
        Path classpathRoot = Paths.get("target/classes");
        Path plansDir = classpathRoot.resolve("plans");
        Path requestsDir = classpathRoot.resolve("requests");
        if (!Files.exists(plansDir)) Files.createDirectories(plansDir);
        if (!Files.exists(requestsDir)) Files.createDirectories(requestsDir);

        Path planPath = plansDir.resolve(planXml.getOriginalFilename());
        Path requestPath = requestsDir.resolve(requestXml.getOriginalFilename());
        Files.copy(planXml.getInputStream(), planPath, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(requestXml.getInputStream(), requestPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Saved plan to {}", planPath.toAbsolutePath());
        log.info("Saved request to {}", requestPath.toAbsolutePath());

        try {
            // Use filenames as in existing controller API (resolved from classpath root)
            String planName = "plans/" + planPath.getFileName().toString();
            String requestName = "requests/" + requestPath.getFileName().toString();

            controller.createPlan(planName);
            controller.createDeliveryFromXml(requestName);
            controller.computeShortestPaths();

            int nbDeliveries = controller.pickupDeliveryModel.demandeDelivery.getDeliveries().size();

            // Use new method that automatically calculates optimal courier count
            List<Tournee> tournees = controller.findOptimalBalancedPaths(4.0, 3600);
            List<List<Long>> paths = controller.buildFullPathNTournées(tournees);

            log.info("TSP computation completed successfully.");

            Map<String, Object> response = new HashMap<>();
            response.put("paths", paths);
            response.put("nbCouriers", paths.size()); // Actual number of couriers used
            response.put("nbDeliveries", nbDeliveries);

            return response;
        } catch (Exception e) {
            log.error("TSP computation failed: {}", e.getMessage(), e);
            throw new IllegalArgumentException("Impossible de calculer le TSP: " + e.getMessage(), e);
        }
    }

    // Simple GET endpoint that accepts plan and request names as parameters
    @GetMapping("/get-tsp")
    public ResponseEntity<Map<String, Object>> getTspSimple(
            @RequestParam(required = false) String planName,
            @RequestParam(required = false) String requestName
    ) {
        try {
            log.info("GET /get-tsp called with planName={}, requestName={}", planName, requestName);

            // If parameters provided, load them
            if (planName != null && !planName.isEmpty()) {
                try {
                    controller.createPlan("plans/" + planName);
                    log.info("Plan loaded: {}", planName);
                } catch (Exception e) {
                    log.error("Failed to load plan {}: {}", planName, e.getMessage(), e);
                    return ResponseEntity.badRequest().body(null);
                }
            }

            if (requestName != null && !requestName.isEmpty()) {
                try {
                    controller.createDeliveryFromXml("requests/" + requestName);
                    log.info("Delivery request loaded: {}", requestName);
                } catch (Exception e) {
                    log.error("Failed to load delivery request {}: {}", requestName, e.getMessage(), e);
                    return ResponseEntity.badRequest().body(null);
                }
            }

            // Check if plan and delivery are loaded
            if (controller.pickupDeliveryModel.plan == null) {
                log.warn("No plan loaded - cannot compute TSP");
                return ResponseEntity.badRequest().body(null);
            }
            if (controller.pickupDeliveryModel.demandeDelivery == null) {
                log.warn("No delivery request loaded - cannot compute TSP");
                return ResponseEntity.badRequest().body(null);
            }

            log.info("Computing shortest paths...");
            controller.computeShortestPaths();

            int nbDeliveries = controller.pickupDeliveryModel.demandeDelivery.getDeliveries().size();

            // Use new method that automatically calculates optimal courier count
            List<Tournee> tournees =  controller.findBalancedPathsForNDrivers(2, 4.0, 1 * 3600);
            List<List<Long>> paths = controller.buildFullPathNTournées(tournees);

            // Build response with actual paths generated
            Map<String, Object> response = new HashMap<>();
            response.put("paths", paths);
            response.put("nbCouriers", paths.size()); // Actual number of couriers used
            response.put("nbDeliveries", nbDeliveries);

            log.info("TSP computed successfully: {} couriers, {} paths", paths.size(), paths.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to compute TSP: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @GetMapping("/get-tsp2") //if a request goes to the root of our web site, it will be called (argument "/")
    public List<Long>  getTsp() throws Exception {
        controller.createPlan("grandPlan.xml");
        controller.createDeliveryFromXml("demandeGrand7.xml");
        controller.computeShortestPaths();
        //controller.findBestPath();


        //controller.solveTwoDriverTspExample();
        List<Tournee> tournees = controller.findBestPathsForTwoDrivers();
        List<Long> tournee1 = controller.buildFullPathArgument(tournees.get(0));
        List<Long> tournee2 = controller.buildFullPathArgument(tournees.get(1));
        System.out.println("Tournee 1: " + tournee1);
        System.out.println("Tournee 2: " + tournee2);

        return tournee1;




    }

    @GetMapping("/get-tsp3") //if a request goes to the root of our web site, it will be called (argument "/")
    public List<List<Long>>  getTsp3() throws Exception {
        controller.createPlan("petitPlan.xml");
        controller.createDeliveryFromXml("demandePetit1.xml");
        controller.computeShortestPaths();
        //controller.findBestPath();


        //controller.solveTwoDriverTspExample();
        List<Tournee> tournees =  controller.findBestPathsForNDrivers(4);
        //List<Long> tournee1 = controller.buildFullPathArgument(tournees.get(0));
        //List<Long> tournee2 = controller.buildFullPathArgument(tournees.get(1));
        //System.out.println("Tournee 1: " + tournee1);
        //System.out.println("Tournee 2: " + tournee2);

        return controller.buildFullPathNTournées(tournees);

    }

    @GetMapping("/get-tsp4") //if a request goes to the root of our web site, it will be called (argument "/")
    public List<List<Long>>  getTsp4() throws Exception {
        controller.createPlan("grandPlan.xml");
        controller.createDeliveryFromXml("demandeGrand7.xml");
        controller.computeShortestPaths();
        //controller.findBestPath();


        //controller.solveTwoDriverTspExample();
        List<Tournee> tournees =  controller.findBalancedPathsForNDrivers(2, 4.0, 1 * 3600);
        //List<Long> tournee1 = controller.buildFullPathArgument(tournees.get(0));
        //List<Long> tournee2 = controller.buildFullPathArgument(tournees.get(1));
        //System.out.println("Tournee 1: " + tournee1);
        //System.out.println("Tournee 2: " + tournee2);

        return controller.buildFullPathNTournées(tournees);


    }




    @GetMapping("/plan-names")
    public String[] getPlanNames() throws IOException {
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
        log.info("Fetching plan file: {}", filename);

        // Spring Boot way : read from src/main/resources/plans/
        ClassPathResource resource = new ClassPathResource("plans/" + filename);

        if (!resource.exists()) {
            log.error("File not found in resources/plans/: {}", filename);
            return ResponseEntity.notFound().build();
        }

        String content = new String(resource.getInputStream().readAllBytes());

        return ResponseEntity.ok(content);
    }

    @GetMapping("/requests/{filename}")
    public ResponseEntity<String> getRequestFile(@PathVariable String filename) throws IOException {
        log.info("Fetching request file: {}", filename);

        // Spring Boot way : read from src/main/resources/requests/
        ClassPathResource resource = new ClassPathResource("requests/" + filename);

        if (!resource.exists()) {
            log.error("File not found in resources/requests/: {}", filename);
            return ResponseEntity.notFound().build();
        }

        String content = new String(resource.getInputStream().readAllBytes());

        return ResponseEntity.ok(content);
    }

    @PostMapping(path = "/upload-request", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadRequest(@RequestPart("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                log.error("No file provided for upload");
                return ResponseEntity.badRequest().build();
            }

            log.info("Uploading request file: {}", file.getOriginalFilename());

            String filename = file.getOriginalFilename();

            // Save to src/main/resources/requests/ (source)
            Path srcRequestsDir = Paths.get("src/main/resources/requests");
            if (!Files.exists(srcRequestsDir)) {
                Files.createDirectories(srcRequestsDir);
            }
            Path srcTargetPath = srcRequestsDir.resolve(filename);
            Files.copy(file.getInputStream(), srcTargetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Request file saved to source: {}", srcTargetPath.toAbsolutePath());

            // Also save to target/classes/requests/ (so it's immediately available)
            Path targetRequestsDir = Paths.get("target/classes/requests");
            if (!Files.exists(targetRequestsDir)) {
                Files.createDirectories(targetRequestsDir);
            }
            Path targetPath = targetRequestsDir.resolve(filename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Request file saved to target: {}", targetPath.toAbsolutePath());

            Map<String, String> response = new HashMap<>();
            response.put("filename", filename);
            response.put("path", srcTargetPath.toAbsolutePath().toString());
            response.put("targetPath", targetPath.toAbsolutePath().toString());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to upload request file: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }



    
}