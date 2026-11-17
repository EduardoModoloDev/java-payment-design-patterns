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
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = SimuladorApiApplication.class)
@AutoConfigureMockMvc
@Import(RelogioFixoConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class TratamentoErrosApiTest {

    @Autowired
    private MockMvc mvc;

    private ResultActions postar(String json) throws Exception {
        return mvc.perform(post("/api/pagamentos").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void pagamentoParaContaInexistenteRespondeNotFound() throws Exception {
        postar("""
                {"contaOrigemId": "CONTA-999", "tipo": "PIX", "valor": 10.00, "destinatario": "x"}
                """)
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Conta não encontrada"))
                .andExpect(jsonPath("$.contaId").value("CONTA-999"));
    }

    @Test
    void consultaDeContaInexistenteRespondeNotFound() throws Exception {
        mvc.perform(get("/api/contas/{id}", "CONTA-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.contaId").value("CONTA-999"));
        mvc.perform(get("/api/contas/{id}/pagamentos", "CONTA-999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void pagamentoInexistenteRespondeNotFoundComCorpo() throws Exception {
        mvc.perform(get("/api/pagamentos/{id}", "nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Pagamento não encontrado"))
                .andExpect(jsonPath("$.pagamentoId").value("nao-existe"));
        mvc.perform(post("/api/pagamentos/{id}/confirmar", "nao-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void camposAusentesRespondemBadRequestListandoOsCampos() throws Exception {
        postar("""
                {"contaOrigemId": "", "destinatario": "x"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requisição inválida"))
                .andExpect(jsonPath("$.campos.contaOrigemId").value("é obrigatório"))
                .andExpect(jsonPath("$.campos.tipo").value("é obrigatório"))
                .andExpect(jsonPath("$.campos.valor").value("é obrigatório"));
    }

    @Test
    void jsonMalFormadoRespondeBadRequest() throws Exception {
        postar("{ isto não é json")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("JSON inválido"));
    }

    @Test
    void tipoDePagamentoDesconhecidoRespondeBadRequest() throws Exception {
        postar("""
                {"contaOrigemId": "CONTA-001", "tipo": "CHEQUE", "valor": 10.00, "destinatario": "x"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("JSON inválido"));
    }

    @Test
    void confirmarBoletoQueNaoEstaPendenteRespondeConflict() throws Exception {
        String corpo = postar("""
                {"contaOrigemId": "CONTA-001", "tipo": "BOLETO", "valor": 100.00,
                 "destinatario": "Companhia de Energia", "dataVencimento": "2026-11-20"}
                """).andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(corpo, "$.pagamentoId");
        mvc.perform(post("/api/pagamentos/{id}/confirmar", id)).andExpect(status().isOk());

        mvc.perform(post("/api/pagamentos/{id}/confirmar", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("PENDENTE")));
    }

    @Test
    void recusaDeNegocioNaoEErroHttp() throws Exception {
        postar("""
                {"contaOrigemId": "CONTA-002", "tipo": "PIX", "valor": -5.00, "destinatario": "x"}
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("RECUSADO"))
                .andExpect(jsonPath("$.codigo").value("DADOS_INVALIDOS"));
    }
}
