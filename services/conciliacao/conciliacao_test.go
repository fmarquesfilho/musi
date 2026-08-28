package conciliacao

import (
	"context"
	"testing"

	"github.com/fmarquesfilho/musi/services/busca/dominio"
)

// catalogoFake e uma implementacao em memoria de CatalogoExterno: mantem os testes
// sem rede, como pede a ADR-0003.
type catalogoFake struct {
	porTitulo map[string][]Candidato
	chamadas  int
}

func (f *catalogoFake) BuscarCandidatos(_ context.Context, titulo, _ string) ([]Candidato, error) {
	f.chamadas++
	return f.porTitulo[titulo], nil
}

func TestConciliarPulaObrasJaConciliadas(t *testing.T) {
	obras := []dominio.Obra{
		{ID: "obra-01", Titulo: "Ponteio", Artista: "Edu Lobo", Mbid: "ja-conciliada"},
		{ID: "obra-03", Titulo: "Asa Branca", Artista: "Luiz Gonzaga"},
	}
	fake := &catalogoFake{porTitulo: map[string][]Candidato{
		"Asa Branca": {{Mbid: "mb-asa", Titulo: "Asa Branca", Artista: "Luiz Gonzaga"}},
	}}

	pend, err := Conciliar(context.Background(), obras, fake)
	if err != nil {
		t.Fatalf("erro inesperado: %v", err)
	}
	if fake.chamadas != 1 {
		t.Errorf("esperava 1 chamada (so a obra sem mbid), obteve %d", fake.chamadas)
	}
	if len(pend) != 1 || pend[0].Obra.ID != "obra-03" {
		t.Fatalf("esperava pendencia so para obra-03, obteve %+v", pend)
	}
	if len(pend[0].Candidatos) != 1 || pend[0].Candidatos[0].Mbid != "mb-asa" {
		t.Errorf("candidato inesperado: %+v", pend[0].Candidatos)
	}
}

func TestConciliarRespeitaCancelamento(t *testing.T) {
	obras := []dominio.Obra{{ID: "obra-03", Titulo: "Asa Branca", Artista: "Luiz Gonzaga"}}
	ctx, cancelar := context.WithCancel(context.Background())
	cancelar()

	if _, err := Conciliar(ctx, obras, &catalogoFake{}); err == nil {
		t.Error("esperava erro de contexto cancelado")
	}
}
