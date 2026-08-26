# Definição de Preparado e de Pronto

São duas listas curtas, e a brevidade é intencional: listas longas tendem a não ser
consultadas, e uma definição de pronto que não se consulta não cumpre sua função.

## Definição de Preparado (DoR)

Um item só entra em sprint se:

- [ ] Está escrito como **resultado**, não como atividade
- [ ] Tem critério de aceitação verificável por alguém que não o escreveu
- [ ] Tem `Tamanho` P ou M. Item G volta para ser quebrado
- [ ] Se toca uma decisão de projeto, a ADR existe e está `Aceita`
- [ ] Se `Risco: Alto`, declara **o que se aprende se falhar**

> Por exemplo: *"a pessoa leitora encontra obras combinando ritmo e movimento"*, em vez de
> *"implementar busca"*.

O último ponto é o que costuma passar despercebido. Um item arriscado com a hipótese
declarada permite aprender algo mesmo quando o resultado é negativo.

## Definição de Pronto (DoD)

Varia por tipo, porque um defeito e uma decisão não terminam da mesma forma.

### História ou Tarefa de código

- [ ] Critérios de aceitação satisfeitos, verificados por outra pessoa
- [ ] CI verde no job do componente
- [ ] Teste automatizado que **falharia** sem a mudança
- [ ] Se mudou `contratos/`, os três componentes atualizados **no mesmo PR**
- [ ] PR revisado por alguém que não escreveu o código
- [ ] Uso de IA declarado no PR

### Defeito

- [ ] Teste que reproduz o defeito, escrito **antes** da correção
- [ ] Causa raiz descrita na issue, em uma frase
- [ ] Se a causa raiz foi decisão de projeto, ADR revisada ou nova ADR aberta

### Decisão

- [ ] ADR escrita, com a seção de alternativas **preenchida**
- [ ] Consequências negativas listadas; uma ADR sem custos declarados volta para revisão
- [ ] Estado `Aceita` e índice atualizado
- [ ] `python docs/verificar_adrs.py` passa

### Débito técnico

- [ ] Descreve o custo de não resolver, e não apenas o que se deseja melhorar
- [ ] Tem gatilho: *"resolver quando X acontecer"*

> Sem um gatilho definido, um débito tende a permanecer indefinidamente no backlog.

## Sobre uso de inteligência artificial

O MUSI usa IA generativa e declara isso. A mesma regra vale para as entregas:

- Declarar **onde** foi usada, no corpo do PR
- A autoria e a responsabilidade são de quem submete
- Código assistido por IA passa pela mesma revisão. Não há via expressa
- Não usar `Co-Authored-By:` para ferramentas, já que a coautoria é uma afirmação de
  titularidade e de responsabilidade

> A orientação não tem caráter restritivo. Ela existe porque a manutenção de um projeto
> depende de se saber a origem e a intenção de cada parte do código.
