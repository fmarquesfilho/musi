// Comando de conciliacao em lote - ADR-0003.
//
// Percorre um acervo, pede candidatos ao MusicBrainz (uma requisicao por segundo)
// e imprime as pendencias para revisao HUMANA. Nao grava nada: a escolha do MBID
// e do curador. Em producao o acervo viria do banco; aqui, um exemplo minimo.
//
//	go run ./cmd/conciliar
package main

import (
	"context"
	"encoding/json"
	"log/slog"
	"os"

	"github.com/fmarquesfilho/musi/services/busca/dominio"
	"github.com/fmarquesfilho/musi/services/conciliacao"
)

func main() {
	acervo := []dominio.Obra{
		{ID: "obra-03", Titulo: "Asa Branca", Artista: "Luiz Gonzaga", Ano: 1947},
		{ID: "obra-05", Titulo: "Refazenda", Artista: "Gilberto Gil", Ano: 1975},
	}

	pendencias, err := conciliacao.Conciliar(
		context.Background(), acervo, conciliacao.NovoClienteMusicBrainz())
	if err != nil {
		slog.Error("conciliacao interrompida", "erro", err)
		os.Exit(1)
	}

	enc := json.NewEncoder(os.Stdout)
	enc.SetIndent("", "  ")
	if err := enc.Encode(pendencias); err != nil {
		slog.Error("falha ao escrever pendencias", "erro", err)
		os.Exit(1)
	}
}
