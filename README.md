# Simulador de Pagamentos — Design Patterns em Java

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Maven](https://img.shields.io/badge/build-Maven-C71A36?logo=apachemaven)
![JUnit 5](https://img.shields.io/badge/testes-JUnit%205-25A162?logo=junit5)

Simulador de processamento de pagamentos (**Pix**, **cartão de crédito** e **boleto**) de uma conta bancária fictícia,
escrito em Java 21 puro. O foco é a **arquitetura**: cada padrão de projeto existe porque resolve um problema concreto
do domínio.

> Projeto final do desafio de **Padrões de Projeto** do curso de Java (Itaú).

## Padrões de projeto (planejados)

| Padrão | Categoria | Onde é usado |
|---|---|---|
| Strategy | Comportamental | `ProcessadorPagamento` e suas implementações |
| Factory Method (com registro) | Criacional | `ProcessadorFactory` |
| Chain of Responsibility | Comportamental | Cadeia de `Validador` |
| Decorator | Estrutural | `TaxaDecorator`, `CashbackDecorator`, `LogDecorator` |
| Observer | Comportamental | `PagamentoListener` e implementações |
| Builder | Criacional | `Pagamento.Builder` |
| Facade | Estrutural | `PagamentoService` |

## Como executar

Pré-requisito: JDK 21 ou superior. O Maven Wrapper já está incluído, não é preciso instalar o Maven.

```bash
# compila e roda os testes
./mvnw clean test

# executa a demonstração (Main)
./mvnw -q compile exec:java
```

No Windows, use `mvnw.cmd` no lugar de `./mvnw`.

A demonstração da Fase 1 cria contas em memória e roda três cenários: Pix aprovado, cartão em 6x
(debitando o limite) e boleto gerado como `PENDENTE`.

## Estrutura

```
src/main/java/io/github/eduardomodolodev/pagamentos/
├── Main.java        → demonstração em Java puro
├── domain/          → Conta, Pagamento (+Builder), ResultadoPagamento, enums
├── repository/      → interfaces + implementações em memória
├── strategy/        → ProcessadorPagamento e Pix/Cartao/BoletoProcessador
├── factory/         → ProcessadorFactory (registro por tipo)
└── service/         → PagamentoService (Facade)
```

## Roadmap

- [x] Fase 1 — Núcleo em Java puro (Strategy + Factory + Builder) — `v0.1.0`
- [ ] Fase 2 — Validações e Decorators (Chain of Responsibility + Decorator)
- [ ] Fase 3 — Observer, confirmação de boleto e testes
- [ ] Fase 4 — API REST com Spring Boot (opcional)

A especificação completa está em [docs/ESPECIFICACAO.md](docs/ESPECIFICACAO.md).

## Autor

Eduardo Modolo — [GitHub](https://github.com/EduardoModoloDev)
