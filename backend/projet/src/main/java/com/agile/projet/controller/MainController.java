package com.agile.projet.controller;




import com.agile.projet.utils.XmlPlanParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

@RestController  //telling java that this class is used for internet and requests trafic
@CrossOrigin(origins = "http://localhost:5173")
public class MainController {

    @Value("${spring.application.name}")
    private String appName;


    private XmlPlanParser xmlPlanParser;


    @GetMapping("/") //if a request goes to the root of our web site, it will be called (argument "/")
    public void index()  {
        xmlPlanParser = new XmlPlanParser();


        System.out.println("Hello World ccaccaca");
        xmlPlanParser.parsePlan("petitPlan.xml");


        return ;
    }

    private String getViewName(){
        return "index";
    }
}