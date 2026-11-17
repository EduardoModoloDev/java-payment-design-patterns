package io.github.eduardomodolodev.pagamentos.api;

import io.github.eduardomodolodev.pagamentos.config.SimuladorApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SimuladorApiApplication.class)
@AutoConfigureMockMvc
class DocumentacaoApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void deveExporOContratoOpenApiComTodosOsEndpoints() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Simulador de Pagamentos"))
                .andExpect(jsonPath("$.paths['/api/pagamentos'].post").exists())
                .andExpect(jsonPath("$.paths['/api/pagamentos/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/pagamentos/{id}/confirmar'].post").exists())
                .andExpect(jsonPath("$.paths['/api/contas/{id}'].get").exists())
                .andExpect(jsonPath("$.paths['/api/contas/{id}/pagamentos'].get").exists());
    }

    @Test
    void swaggerUiDeveEstarDisponivel() throws Exception {
        mvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/swagger-ui/index.html"));
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .string(containsString("Swagger UI")));
    }
}
