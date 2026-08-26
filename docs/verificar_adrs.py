#!/usr/bin/env python3
"""Verifica que toda ADR aparece no indice e tem as secoes obrigatorias.

Chamado pelo job `docs` do CI - interessa a DIM0510.

A ideia e que uma ADR ausente do indice seja tratada como defeito, detectado no
pipeline em vez de na leitura de alguem meses depois.

Duas secoes sao obrigatorias, e sao as que costumam ficar de fora:
  - "Alternativas consideradas": sem ela, a ADR descreve um fato consumado
  - "Consequencias": deve incluir tambem os custos da decisao
"""
import pathlib
import re
import sys

AQUI = pathlib.Path(__file__).parent / "decisoes"
OBRIGATORIAS = ["## Contexto", "## Decisão", "## Alternativas consideradas", "## Consequências"]

falhas = []
indice = (AQUI / "README.md").read_text(encoding="utf-8")

adrs = sorted(p for p in AQUI.glob("[0-9][0-9][0-9][0-9]-*.md")
              if not p.name.startswith("0000-"))

for adr in adrs:
    if adr.name not in indice:
        falhas.append(f"{adr.name} nao aparece em decisoes/README.md")

    texto = adr.read_text(encoding="utf-8")

    if not re.search(r"^\*\*Estado:\*\*", texto, re.M):
        falhas.append(f"{adr.name} sem campo Estado")

    for secao in OBRIGATORIAS:
        if secao not in texto:
            falhas.append(f"{adr.name} sem secao '{secao}'")

print(f"Verificadas {len(adrs)} ADRs.")

if falhas:
    print("\nFALHAS:", file=sys.stderr)
    for f in falhas:
        print(f"  - {f}", file=sys.stderr)
    sys.exit(1)

print("Todas no indice e com as secoes obrigatorias.")
