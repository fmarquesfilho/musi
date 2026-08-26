#!/usr/bin/env python3
"""Verifica que links internos entre arquivos markdown apontam para algo que existe.

Chamado pelo job `docs` do CI - interessa a DIM0510.

Apenas links relativos sao verificados. Links http dependeriam de rede, e um
pipeline que falha por indisponibilidade de um site externo reduz a confianca no CI.
"""
import pathlib
import re
import sys

RAIZ = pathlib.Path(__file__).parent.parent
LINK = re.compile(r"\[[^\]]*\]\(([^)#]+)(#[^)]*)?\)")

falhas = []
verificados = 0

for md in sorted(RAIZ.rglob("*.md")):
    if ".git" in md.parts:
        continue
    for alvo, _ancora in LINK.findall(md.read_text(encoding="utf-8")):
        alvo = alvo.strip()
        if alvo.startswith(("http://", "https://", "mailto:")):
            continue
        verificados += 1
        if not (md.parent / alvo).resolve().exists():
            falhas.append(f"{md.relative_to(RAIZ)} -> {alvo}")

print(f"Verificados {verificados} links internos.")

if falhas:
    print("\nLINKS QUEBRADOS:", file=sys.stderr)
    for f in falhas:
        print(f"  - {f}", file=sys.stderr)
    sys.exit(1)

print("Nenhum link quebrado.")
