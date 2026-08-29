// Servico de busca do MUSI - DIM0547.
//
// Expoe POST /buscar, recebendo a arvore de filtro de contratos/filtro.schema.json.
// Sem dependencia externa: so a biblioteca padrao.
package main

import (
	"encoding/json"
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/fmarquesfilho/musi/services/busca/dominio"
)

// filtroDTO existe porque `dominio.Filtro` e uma interface, e encoding/json nao
// sabe instanciar interfaces. E o mesmo problema que o FiltroDto resolve do lado
// Java - com a diferenca de que la o compilador exige a exaustividade. Ver ADR-0002.
type filtroDTO struct {
	Tipo       string      `json:"tipo"`
	Dimensao   string      `json:"dimensao,omitempty"`
	Valor      string      `json:"valor,omitempty"`
	Opcoes     []filtroDTO `json:"opcoes,omitempty"`
	Exigencias []filtroDTO `json:"exigencias,omitempty"`
	Filtro     *filtroDTO  `json:"filtro,omitempty"`
	Ano        int         `json:"ano,omitempty"`
}

func (d filtroDTO) paraDominio() dominio.Filtro {
	switch d.Tipo {
	case "tem":
		return dominio.Tem{Dimensao: d.Dimensao, Valor: d.Valor}
	case "ou":
		fs := make([]dominio.Filtro, len(d.Opcoes))
		for i, o := range d.Opcoes {
			fs[i] = o.paraDominio()
		}
		return dominio.Ou{Opcoes: fs}
	case "e":
		fs := make([]dominio.Filtro, len(d.Exigencias))
		for i, e := range d.Exigencias {
			fs[i] = e.paraDominio()
		}
		return dominio.E{Exigencias: fs}
	case "exceto":
		return dominio.Exceto{Filtro: d.Filtro.paraDominio()}
	case "ate":
		return dominio.Ate{Ano: d.Ano}
	default:
		return nil
	}
}

// acervo de exemplo, em memoria. Sprint 2: vem do Postgres no Neon.
// Dados inventados para o curso - ver ADR-0002.
var acervo = []dominio.Obra{
	{ID: "obra-01", Titulo: "Ponteio", Artista: "Edu Lobo", Ano: 1967, Facetas: []dominio.Faceta{
		{Dimensao: "genero", Valor: "mpb"}, {Dimensao: "ritmo", Valor: "ponteio"},
		{Dimensao: "movimento", Valor: "festivais-da-cancao"}},
		Mbid:           "11111111-1111-4111-8111-111111111111",
		MbidComposicao: "22222222-2222-4222-8222-222222222222"},
	{ID: "obra-02", Titulo: "Beira Mar", Artista: "Gilberto Gil", Ano: 1969, Facetas: []dominio.Faceta{
		{Dimensao: "genero", Valor: "mpb"}, {Dimensao: "ritmo", Valor: "ijexa"},
		{Dimensao: "movimento", Valor: "tropicalia"}}},
	{ID: "obra-03", Titulo: "Asa Branca", Artista: "Luiz Gonzaga", Ano: 1947, Facetas: []dominio.Faceta{
		{Dimensao: "genero", Valor: "forro"}, {Dimensao: "ritmo", Valor: "baiao"},
		{Dimensao: "instrumentacao", Valor: "sanfona"}}},
	{ID: "obra-04", Titulo: "Rio Grande", Artista: "Chico Science & Nacao Zumbi", Ano: 1994, Facetas: []dominio.Faceta{
		{Dimensao: "ritmo", Valor: "maracatu"}, {Dimensao: "movimento", Valor: "manguebeat"},
		{Dimensao: "regiao", Valor: "recife"}}},
	{ID: "obra-05", Titulo: "Refazenda", Artista: "Gilberto Gil", Ano: 1975, Facetas: []dominio.Faceta{
		{Dimensao: "genero", Valor: "mpb"}, {Dimensao: "ritmo", Valor: "baiao"}}},
}

func buscar(w http.ResponseWriter, r *http.Request) {
	if r.Method != http.MethodPost {
		problema(w, http.StatusMethodNotAllowed, "metodo-nao-permitido", "Use POST.")
		return
	}

	var dto filtroDTO
	if err := json.NewDecoder(r.Body).Decode(&dto); err != nil {
		problema(w, http.StatusBadRequest, "json-invalido", "O corpo nao e um JSON valido.")
		return
	}

	f := dto.paraDominio()
	if f == nil {
		problema(w, http.StatusUnprocessableEntity, "tipo-de-filtro-desconhecido",
			"O campo `tipo` nao corresponde a nenhum construtor de filtro.")
		return
	}

	w.Header().Set("Content-Type", "application/json")
	w.Header().Set("Cache-Control", "public, max-age=60")
	_ = json.NewEncoder(w).Encode(dominio.Buscar(acervo, f))
}

// Erro no formato da RFC 9457, como o discutido em aula.
func problema(w http.ResponseWriter, status int, tipo, detalhe string) {
	w.Header().Set("Content-Type", "application/problem+json")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(map[string]any{
		"type": "https://musi.ufrn.br/erros/" + tipo, "status": status, "detail": detalhe,
	})
}

func main() {
	porta := os.Getenv("PORT")
	if porta == "" {
		porta = "9090"
	}

	mux := http.NewServeMux()
	mux.HandleFunc("/buscar", buscar)
	mux.HandleFunc("/health", func(w http.ResponseWriter, _ *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write([]byte(`{"status":"UP"}`))
	})

	srv := &http.Server{
		Addr:              ":" + porta,
		Handler:           mux,
		ReadHeaderTimeout: 5 * time.Second,
		// Go 1.27: limita a quantidade de valores por cabecalho, contra
		// cabecalhos abusivos. Mesma ideia do ReadHeaderTimeout: o servidor
		// declara seus limites.
		MaxHeaderValueCount: 100,
	}

	slog.Info("servico de busca no ar", "porta", porta, "obras", len(acervo))
	if err := srv.ListenAndServe(); err != nil {
		slog.Error("servidor encerrou", "erro", err)
		os.Exit(1)
	}
}
