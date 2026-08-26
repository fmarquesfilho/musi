package dominio

import (
	"reflect"
	"testing"
)

// Os mesmos casos de contratos/exemplos/casos-de-busca.json.
// app (Kotlin) e api (Java) rodam estes mesmos casos e devem concordar.

var acervo = []Obra{
	{ID: "obra-01", Titulo: "Ponteio", Artista: "Edu Lobo", Ano: 1967, Facetas: []Faceta{
		{"genero", "mpb"}, {"ritmo", "ponteio"}, {"movimento", "festivais-da-cancao"}}},
	{ID: "obra-02", Titulo: "Beira Mar", Artista: "Gilberto Gil", Ano: 1969, Facetas: []Faceta{
		{"genero", "mpb"}, {"ritmo", "ijexa"}, {"movimento", "tropicalia"}}},
	{ID: "obra-03", Titulo: "Asa Branca", Artista: "Luiz Gonzaga", Ano: 1947, Facetas: []Faceta{
		{"genero", "forro"}, {"ritmo", "baiao"}, {"instrumentacao", "sanfona"}}},
	{ID: "obra-04", Titulo: "Rio Grande", Artista: "Chico Science & Nacao Zumbi", Ano: 1994, Facetas: []Faceta{
		{"ritmo", "maracatu"}, {"movimento", "manguebeat"}, {"regiao", "recife"}}},
	{ID: "obra-05", Titulo: "Refazenda", Artista: "Gilberto Gil", Ano: 1975, Facetas: []Faceta{
		{"genero", "mpb"}, {"ritmo", "baiao"}}},
}

func ids(f Filtro) []string {
	resultado := []string{}
	for _, o := range Buscar(acervo, f) {
		resultado = append(resultado, o.ID)
	}
	return resultado
}

func TestCasosCompartilhados(t *testing.T) {
	casos := []struct {
		nome     string
		filtro   Filtro
		esperado []string
	}{
		{
			"faceta simples",
			Tem{"ritmo", "baiao"},
			[]string{"obra-03", "obra-05"},
		},
		{
			"disjuncao de ritmos",
			Ou{[]Filtro{Tem{"ritmo", "ijexa"}, Tem{"ritmo", "maracatu"}}},
			[]string{"obra-02", "obra-04"},
		},
		{
			"conjuncao com exclusao",
			E{[]Filtro{Tem{"genero", "mpb"}, Exceto{Tem{"ritmo", "ijexa"}}}},
			[]string{"obra-01", "obra-05"},
		},
		{
			"tres niveis com corte por ano",
			E{[]Filtro{
				Ou{[]Filtro{Tem{"movimento", "tropicalia"}, Tem{"movimento", "festivais-da-cancao"}}},
				Ate{1968},
				Exceto{Tem{"ritmo", "ijexa"}},
			}},
			[]string{"obra-01"},
		},
		{
			"disjuncao vazia nao aceita nada",
			Ou{[]Filtro{}},
			[]string{},
		},
		{
			"conjuncao vazia aceita tudo",
			E{[]Filtro{}},
			[]string{"obra-01", "obra-02", "obra-03", "obra-04", "obra-05"},
		},
		{
			"dimensao inexistente nao casa",
			Tem{"afinacao", "aberta"},
			[]string{},
		},
	}

	for _, c := range casos {
		t.Run(c.nome, func(t *testing.T) {
			obtido := ids(c.filtro)
			if !reflect.DeepEqual(obtido, c.esperado) {
				t.Errorf("esperava %v, obteve %v", c.esperado, obtido)
			}
		})
	}
}

func TestDescreverAninhado(t *testing.T) {
	f := E{[]Filtro{
		Ou{[]Filtro{Tem{"ritmo", "ijexa"}, Tem{"ritmo", "baiao"}}},
		Exceto{Tem{"genero", "forro"}},
	}}
	esperado := "((ritmo=ijexa ou ritmo=baiao) e nao genero=forro)"
	if obtido := Descrever(f); obtido != esperado {
		t.Errorf("esperava %q, obteve %q", esperado, obtido)
	}
}
