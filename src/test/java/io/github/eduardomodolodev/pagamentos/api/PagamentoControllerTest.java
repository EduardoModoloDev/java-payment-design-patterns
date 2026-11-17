package io.github.eduardomodolodev.pagamentos.api;

import com.jayway.jsonpath.JsonPath;
import io.github.eduardomodolodev.pagamentos.config.SimuladorApiApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SimuladorApiApplication.class)
@AutoConfigureMockMvc
@Import(RelogioFixoConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PagamentoControllerTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postar(String json) throws Exception {
        return mvc.perform(post("/api/pagamentos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String criarEObterId(String json) throws Exception {
        String corpo = postar(json).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.pagamentoId");
    }

    @Test
    void deveProcessarCartaoEm6xComOExemploDaEspecificacao() throws Exception {
        postar("""
                {
                  "contaOrigemId": "CONTA-001",
                  "tipo": "CARTAO_CREDITO",
                  "valor": 1200.00,
                  "destinatario": "Loja do Zé",
                  "parcelas": 6
                }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.codigo").value(nullValue()))
                .andExpect(jsonPath("$.mensagem").value("Pagamento no cartão aprovado em 6x"))
                .andExpect(jsonPath("$.valor").value(1200.00))
                .andExpect(jsonPath("$.taxa").value(84.00))
                .andExpect(jsonPath("$.total").value(1284.00))
                .andExpect(jsonPath("$.cashback").value(12.00))
                .andExpect(jsonPath("$.processadoEm").value("2026-11-17T14:00:00"));
    }

    @Test
    void deveDevolverLocationApontandoParaOPagamentoCriado() throws Exception {
        MvcResult resposta = postar("""
                {"contaOrigemId": "CONTA-001", "tipo": "PIX", "valor": 200.00, "destinatario": "joao@pix.com"}
                """)
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(resposta.getResponse().getContentAsString(), "$.pagamentoId");

        assertThat(resposta.getResponse().getHeader("Location")).endsWith("/api/pagamentos/" + id);
        mvc.perform(get(resposta.getResponse().getHeader("Location")))
                .andExpect(status().isOk());
    }

    @Test
    void recusaDeNegocioTambemRespondeCreatedComOCodigo() throws Exception {
        postar("""
                {
                  "contaOrigemId": "CONTA-001",
                  "tipo": "CARTAO_CREDITO",
                  "valor": 900.00,
                  "destinatario": "Loja do Zé",
                  "parcelas": 13
                }
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.codigo").value("PARCELAS_INVALIDAS"));
    }

    @Test
    void deveConsultarUmPagamento() throws Exception {
        String id = criarEObterId("""
                {"contaOrigemId": "CONTA-001", "tipo": "PIX", "valor": 200.00, "destinatario": "joao@pix.com"}
                """);

        mvc.perform(get("/api/pagamentos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.contaOrigemId").value("CONTA-001"))
                .andExpect(jsonPath("$.tipo").value("PIX"))
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.parcelas").value(1))
                .andExpect(jsonPath("$.criadoEm").value("2026-11-17T14:00:00"));
    }

    @Test
    void consultaDePagamentoInexistenteRespondeNotFound() throws Exception {
        mvc.perform(get("/api/pagamentos/{id}", "nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deveConfirmarUmBoletoPendente() throws Exception {
        String id = criarEObterId("""
                {
                  "contaOrigemId": "CONTA-001",
                  "tipo": "BOLETO",
                  "valor": 350.00,
                  "destinatario": "Companhia de Energia",
                  "dataVencimento": "2026-11-27"
                }
                """);

        mvc.perform(post("/api/pagamentos/{id}/confirmar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADO"))
                .andExpect(jsonPath("$.taxa").value(3.50))
                .andExpect(jsonPath("$.total").value(353.50));

        mvc.perform(get("/api/pagamentos/{id}", id))
                .andExpect(jsonPath("$.status").value("APROVADO"));
    }

    @Test
    void campoObrigatorioAusenteRespondeBadRequest() throws Exception {
        postar("""
                {"contaOrigemId": "CONTA-001", "tipo": "PIX", "destinatario": "joao@pix.com"}
                """)
                .andExpect(status().isBadRequest());
    }
}
