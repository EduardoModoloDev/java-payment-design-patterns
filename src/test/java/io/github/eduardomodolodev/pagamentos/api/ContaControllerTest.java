package io.github.eduardomodolodev.pagamentos.api;

import io.github.eduardomodolodev.pagamentos.config.SimuladorApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SimuladorApiApplication.class)
@AutoConfigureMockMvc
@Import(RelogioFixoConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class ContaControllerTest {

    @Autowired
    private MockMvc mvc;

    private void postar(String json) throws Exception {
        mvc.perform(post("/api/pagamentos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated());
    }

    @Test
    void deveConsultarSaldoELimitesDaConta() throws Exception {
        mvc.perform(get("/api/contas/{id}", "CONTA-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("CONTA-001"))
                .andExpect(jsonPath("$.titular").value("Maria Silva"))
                .andExpect(jsonPath("$.saldo").value(3000.00))
                .andExpect(jsonPath("$.limiteCartaoDisponivel").value(5000.00))
                .andExpect(jsonPath("$.limiteDiario").value(5000.00));
    }

    @Test
    void saldoELimiteDevemRefletirOsPagamentosAprovados() throws Exception {
        postar("""
                {"contaOrigemId": "CONTA-001", "tipo": "PIX", "valor": 200.00, "destinatario": "joao@pix.com"}
                """);
        postar("""
                {"contaOrigemId": "CONTA-001", "tipo": "CARTAO_CREDITO", "valor": 1200.00,
                 "destinatario": "Loja do Zé", "parcelas": 6}
                """);

        mvc.perform(get("/api/contas/{id}", "CONTA-001"))
                .andExpect(jsonPath("$.saldo").value(2800.00))
                .andExpect(jsonPath("$.limiteCartaoDisponivel").value(3716.00));
    }

    @Test
    void deveListarOHistoricoDaContaNaOrdemIncluindoRecusados() throws Exception {
        postar("""
                {"contaOrigemId": "CONTA-002", "tipo": "PIX", "valor": 100.00, "destinatario": "ana@pix.com"}
                """);
        postar("""
                {"contaOrigemId": "CONTA-002", "tipo": "PIX", "valor": 5000.00, "destinatario": "ana@pix.com"}
                """);
        postar("""
                {"contaOrigemId": "CONTA-001", "tipo": "PIX", "valor": 10.00, "destinatario": "outra conta"}
                """);

        mvc.perform(get("/api/contas/{id}/pagamentos", "CONTA-002"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].valor").value(100.00))
                .andExpect(jsonPath("$[0].status").value("APROVADO"))
                .andExpect(jsonPath("$[1].valor").value(5000.00))
                .andExpect(jsonPath("$[1].status").value("RECUSADO"));
    }

    @Test
    void historicoDeContaSemPagamentosEUmaListaVazia() throws Exception {
        mvc.perform(get("/api/contas/{id}/pagamentos", "CONTA-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
