#!/usr/bin/env python3
"""Valida os contratos do MUSI e roda os casos de busca compartilhados.

Chamado pelo job `contratos` do CI. Roda igual na sua maquina:

    pip install jsonschema && python contratos/validar.py

Tres verificacoes:
  1. Toda obra do acervo valida contra obra.schema.json
  2. O exemplo marcado invalido e rejeitado, o que confirma que o schema verifica
     de fato o que deveria
  3. Os casos de busca produzem o resultado esperado

O item 3 e a implementacao de referencia do filtro. As versoes em app (Kotlin),
api (Java) e services (Go) devem produzir os mesmos resultados. Havendo divergencia,
ha algo a corrigir em uma delas.
Ver docs/decisoes/0002-modelo-de-dominio.md.
"""
import json
import pathlib
import sys

from jsonschema import Draft202012Validator as Validador

AQUI = pathlib.Path(__file__).parent
falhas = []


def carregar(nome):
    return json.loads((AQUI / nome).read_text(encoding="utf-8"))


def satisfaz(obra, filtro):
    """Implementacao de referencia. Vale comparar com as versoes Kotlin, Java e Go."""
    tipo = filtro["tipo"]
    if tipo == "tem":
        return any(f["dimensao"] == filtro["dimensao"] and f["valor"] == filtro["valor"]
                   for f in obra["facetas"])
    if tipo == "ou":
        return any(satisfaz(obra, f) for f in filtro["opcoes"])
    if tipo == "e":
        return all(satisfaz(obra, f) for f in filtro["exigencias"])
    if tipo == "exceto":
        return not satisfaz(obra, filtro["filtro"])
    if tipo == "ate":
        return obra["ano"] <= filtro["ano"]
    raise ValueError(f"filtro nao tratado: {tipo}")


def main():
    obra_v = Validador(carregar("obra.schema.json"))
    filtro_v = Validador(carregar("filtro.schema.json"))

    # 1. acervo
    acervo = carregar("exemplos/acervo.json")
    if not acervo.get("exemploDidatico"):
        falhas.append("acervo.json sem marca exemploDidatico (ADR-0002)")
    for obra in acervo["obras"]:
        for erro in obra_v.iter_errors(obra):
            falhas.append(f"{obra['id']}: {erro.message}")
    print(f"ok   acervo: {len(acervo['obras'])} obras validas")

    # 2. o invalido precisa ser rejeitado
    for caminho in sorted((AQUI / "exemplos").glob("invalido-*.json")):
        dado = json.loads(caminho.read_text(encoding="utf-8"))
        dado.pop("exemploDidatico", None)
        dado.pop("_nota", None)
        if not list(obra_v.iter_errors(dado)):
            falhas.append(f"{caminho.name} deveria ser rejeitado pelo schema, mas passou")
        else:
            print(f"ok   {caminho.name} rejeitado, como esperado")

    # 3. casos de busca
    casos = carregar("exemplos/casos-de-busca.json")["casos"]
    for caso in casos:
        for erro in filtro_v.iter_errors(caso["filtro"]):
            falhas.append(f"{caso['nome']}: filtro invalido - {erro.message}")
        obtido = [o["id"] for o in acervo["obras"] if satisfaz(o, caso["filtro"])]
        if obtido != caso["esperado"]:
            falhas.append(f"{caso['nome']}: esperava {caso['esperado']}, obteve {obtido}")
        else:
            print(f"ok   {caso['nome']}: {obtido}")

    if falhas:
        print("\nFALHAS:", file=sys.stderr)
        for f in falhas:
            print(f"  - {f}", file=sys.stderr)
        return 1
    print(f"\nTudo certo: {len(casos)} casos de busca.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
