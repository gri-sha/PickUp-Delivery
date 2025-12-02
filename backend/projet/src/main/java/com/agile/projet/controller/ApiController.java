package com.agile.projet.controller;




import com.agile.projet.model.PickupDeliveryModel;
import com.agile.projet.utils.XmlPlanParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController  //telling java that this class is used for internet and requests trafic
@CrossOrigin(origins = "http://localhost:5173")
public class ApiController {

    @Value("${spring.application.name}")
    private String appName;

    @Autowired
    Controller controller;


    @GetMapping("/") //if a request goes to the root of our website, it will be called (argument "/")
    public void index() throws Exception {
        controller.createPlan("petitPlan.xml");
        controller.createDeliveryFromXml("demandeMoyen5.xml");
        controller.computeShortestPaths();

        System.out.println("Hello World");


        return ;
    }

    private String getViewName(){
        return "index";
    }
}