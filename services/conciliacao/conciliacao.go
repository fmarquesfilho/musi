// Package conciliacao casa obras locais com identidades do MusicBrainz - ADR-0003.
//
// A conciliacao acontece na IMPORTACAO, nunca na leitura, e a escolha entre
// candidatos e HUMANA: este pacote apenas COLETA candidatos para revisao, sem
// atribuir MBID a nenhuma obra. O dominio (services/busca/dominio) nao conhece o
// MusicBrainz; o acesso externo mora atras da porta CatalogoExterno.
package conciliacao

import (
	"context"

	"github.com/fmarquesfilho/musi/services/busca/dominio"
)

// Candidato e uma identidade sugerida pelo MusicBrainz para uma obra local.
type Candidato struct {
	Mbid           string `json:"mbid"`                     // a gravacao (recording)
	MbidComposicao string `json:"mbidComposicao,omitempty"` // a composicao (work)
	Titulo         string `json:"titulo"`
	Artista        string `json:"artista"`
	Desambiguacao  string `json:"desambiguacao,omitempty"` // distingue homonimos
}

// CatalogoExterno e a porta para a base de autoridade. O dominio nao a conhece;
// so a importacao. Uma implementacao em memoria mantem os testes sem rede - ADR-0003.
type CatalogoExterno interface {
	BuscarCandidatos(ctx context.Context, titulo, artista string) ([]Candidato, error)
}

// Pendencia e uma obra ainda nao conciliada, com os candidatos encontrados,
// aguardando a escolha de um curador.
type Pendencia struct {
	Obra       dominio.Obra `json:"obra"`
	Candidatos []Candidato  `json:"candidatos"`
}

// Conciliar percorre as obras SEM mbid e, para cada uma, pede candidatos ao
// catalogo. Nao atribui nada: a escolha e humana - ADR-0003.
//
// Respeita o cancelamento do contexto, porque a coleta pode ser longa: o
// MusicBrainz limita o consumo a cerca de uma requisicao por segundo.
func Conciliar(ctx context.Context, obras []dominio.Obra, catalogo CatalogoExterno) ([]Pendencia, error) {
	pendencias := make([]Pendencia, 0)
	for _, o := range obras {
		if o.Mbid != "" {
			continue // ja conciliada
		}
		select {
		case <-ctx.Done():
			return pendencias, ctx.Err()
		default:
		}
		candidatos, err := catalogo.BuscarCandidatos(ctx, o.Titulo, o.Artista)
		if err != nil {
			return pendencias, err
		}
		pendencias = append(pendencias, Pendencia{Obra: o, Candidatos: candidatos})
	}
	return pendencias, nil
}
