package com.agile.projet.controller;

import com.agile.projet.model.Tournee;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ApiController.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private Controller controller;

    @Test
    @DisplayName("GET / returns 200 and triggers Controller methods")
    void rootIndex_ok() throws Exception {
        // Stub methods used by index()
        Mockito.doNothing().when(controller).createPlan("petitPlan.xml");
        Mockito.doNothing().when(controller).createDeliveryFromXml("demandePetit1.xml");
        Mockito.doNothing().when(controller).computeShortestPaths();
        Mockito.when(controller.findBestPath()).thenReturn(new Tournee(0.0, List.of()));

        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        Mockito.verify(controller, times(1)).createPlan("petitPlan.xml");
        Mockito.verify(controller, times(1)).createDeliveryFromXml("demandePetit1.xml");
        Mockito.verify(controller, times(1)).computeShortestPaths();
        Mockito.verify(controller, times(2)).findBestPath();
    }

    @Test
    @DisplayName("GET /get-tsp returns path list")
    void getTsp_ok() throws Exception {
        Mockito.doNothing().when(controller).createPlan("petitPlan.xml");
        Mockito.doNothing().when(controller).createDeliveryFromXml("demandePetit1.xml");
        Mockito.doNothing().when(controller).computeShortestPaths();
        Mockito.when(controller.findBestPath()).thenReturn(new Tournee(0.0, List.of()));
        Mockito.when(controller.buildFullPath()).thenReturn(List.of(1L, 2L, 3L));

        mockMvc.perform(get("/get-tsp"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0]", is(1)))
                .andExpect(jsonPath("$[1]", is(2)))
                .andExpect(jsonPath("$[2]", is(3)));

        Mockito.verify(controller, times(1)).createPlan("petitPlan.xml");
        Mockito.verify(controller, times(1)).createDeliveryFromXml("demandePetit1.xml");
        Mockito.verify(controller, times(1)).computeShortestPaths();
        Mockito.verify(controller, times(2)).findBestPath();
        Mockito.verify(controller, times(1)).buildFullPath();
    }

    @Test
    @DisplayName("GET /plan-names returns array of filenames or empty")
    void planNames_ok() throws Exception {
        mockMvc.perform(get("/plan-names"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /request-names returns array of filenames or empty")
    void requestNames_ok() throws Exception {
        mockMvc.perform(get("/request-names"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("GET /load-plan/{planName} delegates to controller.createPlan")
    void loadPlan_ok() throws Exception {
        Mockito.doNothing().when(controller).createPlan(anyString());
        mockMvc.perform(get("/load-plan/grandPlan.xml"))
                .andExpect(status().isOk());
        Mockito.verify(controller, times(1)).createPlan("grandPlan.xml");
    }

    @Test
    @DisplayName("GET /plans/{filename} returns 200 or 404 from classpath")
    void getPlanFile_ok_or_404() throws Exception {
        // The controller reads from ClassPathResource(filename). We'll just hit endpoint.
        mockMvc.perform(get("/plans/grandPlan.xml"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /requests/{filename} returns 200 or 404 from classpath")
    void getRequestFile_ok_or_404() throws Exception {
        mockMvc.perform(get("/requests/demandePetit1.xml"))
                .andExpect(status().isOk());
    }
}
