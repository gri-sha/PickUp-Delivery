package com.agile.projet.controller;




import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

@RestController  //telling java that this class is used for internet and requests trafic
@CrossOrigin(origins = "http://localhost:5173")
public class MainController {

    @Value("${spring.application.name}")
    private String appName;


    @GetMapping("/") //if a request goes to the root of our web site, it will be called (argument "/")
    public void index(){

        System.out.println("Hello World");


        return ;
    }

    private String getViewName(){
        return "index";
    }
}