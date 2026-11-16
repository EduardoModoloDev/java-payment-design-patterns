package io.github.eduardomodolodev.pagamentos.validation;

import io.github.eduardomodolodev.pagamentos.domain.CodigoRecusa;
import io.github.eduardomodolodev.pagamentos.domain.Conta;
import io.github.eduardomodolodev.pagamentos.domain.Pagamento;
import io.github.eduardomodolodev.pagamentos.domain.TipoPagamento;
import io.github.eduardomodolodev.pagamentos.repository.PagamentoRepositoryEmMemoria;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static io.github.eduardomodolodev.pagamentos.validation.Fixtures.pagamento;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CadeiaValidadoresTest {

    private final Conta conta = Fixtures.conta("800.00", "2000.00");

    /** Validador de teste que registra a própria execução e pode reprovar. */
    private static final class Espiao extends Validador {
        private final String nome;
        private final boolean reprova;
        private final List<String> chamadas;

        Espiao(String nome, boolean reprova, List<String> chamadas) {
            this.nome = nome;
            this.reprova = reprova;
            this.chamadas = chamadas;
        }

        @Override
        protected void verificar(Pagamento pagamento, Conta conta) {
            chamadas.add(nome);
            if (reprova) {
                throw new ValidacaoException("ERRO_" + nome, "reprovado por " + nome);
            }
        }
    }

    @Test
    void deveExecutarOsElosNaOrdemEmQueForamEncadeados() {
        List<String> chamadas = new ArrayList<>();
        Validador inicio = new Espiao("A", false, chamadas);
        inicio.proximo(new Espiao("B", false, chamadas)).proximo(new Espiao("C", false, chamadas));

        inicio.validar(pagamento(TipoPagamento.PIX, "10.00").build(), conta);

        assertThat(chamadas).containsExactly("A", "B", "C");
    }

    @Test
    void deveParaNaPrimeiraFalhaSemChamarOsSeguintes() {
        List<String> chamadas = new ArrayList<>();
        Validador inicio = new Espiao("A", false, chamadas);
        inicio.proximo(new Espiao("B", true, chamadas)).proximo(new Espiao("C", false, chamadas));

        assertThatThrownBy(() -> inicio.validar(pagamento(TipoPagamento.PIX, "10.00").build(), conta))
                .isInstanceOf(ValidacaoException.class)
                .hasMessage("reprovado por B");
        assertThat(chamadas).containsExactly("A", "B");
    }

    @Test
    void cadeiaPadraoDeveTerAOrdemDaEspecificacao() {
        List<Class<?>> ordem = new ArrayList<>();
        for (Validador elo = CadeiaValidadores.padrao(new PagamentoRepositoryEmMemoria(), Fixtures.relogioEm(Fixtures.MEIO_DIA));
             elo != null; elo = elo.getProximo()) {
            ordem.add(elo.getClass());
        }

        assertThat(ordem).containsExactly(
                ValidadorDadosBasicos.class,
                ValidadorHorario.class,
                ValidadorSaldoOuLimite.class,
                ValidadorLimiteDiario.class,
                ValidadorAntifraude.class);
    }

    @Test
    void cadeiaPadraoDeveAprovarPagamentoValido() {
        Validador cadeia = CadeiaValidadores.padrao(new PagamentoRepositoryEmMemoria(), Fixtures.relogioEm(Fixtures.MEIO_DIA));

        assertThatCode(() -> cadeia.validar(pagamento(TipoPagamento.PIX, "200.00").build(), conta))
                .doesNotThrowAnyException();
    }

    @Test
    void horarioDeveBarrarAntesDoSaldo() {
        Validador cadeia = CadeiaValidadores.padrao(new PagamentoRepositoryEmMemoria(),
                Fixtures.relogioEm(LocalDateTime.of(2026, 11, 16, 22, 0)));

        // Pix de 1.500 à noite com saldo de 800: as duas regras reprovariam,
        // mas o horário vem antes na cadeia.
        assertThatThrownBy(() -> cadeia.validar(pagamento(TipoPagamento.PIX, "1500.00").build(), conta))
                .extracting(e -> ((ValidacaoException) e).getCodigo())
                .isEqualTo(CodigoRecusa.LIMITE_NOTURNO_EXCEDIDO);
    }
}
