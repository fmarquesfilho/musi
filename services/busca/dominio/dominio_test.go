package dominio

import (
	"encoding/json"
	"os"
	"path/filepath"
	"reflect"
	"testing"
)

// Os casos de busca vêm de contratos/exemplos/, CARREGADOS (não transcritos):
// app (Kotlin) e api (Java) carregam os mesmos arquivos, então um caso novo alcança
// as três implementações de uma vez. Havendo divergência, uma delas está errada.
// Ver ADR-0002 e contratos/exemplos/casos-de-busca.json.

// exemplos localiza contratos/exemplos/<nome> subindo a partir do diretório do teste,
// para funcionar tanto rodando `go test ./...` quanto a partir da raiz.
func exemplos(t *testing.T, nome string) []byte {
	t.Helper()
	dir, err := os.Getwd()
	if err != nil {
		t.Fatal(err)
	}
	for {
		caminho := filepath.Join(dir, "contratos", "exemplos", nome)
		if _, err := os.Stat(caminho); err == nil {
			dados, err := os.ReadFile(caminho)
			if err != nil {
				t.Fatal(err)
			}
			return dados
		}
		pai := filepath.Dir(dir)
		if pai == dir {
			t.Fatalf("não encontrei contratos/exemplos/%s subindo de %s", nome, dir)
		}
		dir = pai
	}
}

func carregarAcervo(t *testing.T) []Obra {
	var arquivo struct {
		Obras []Obra `json:"obras"`
	}
	if err := json.Unmarshal(exemplos(t, "acervo.json"), &arquivo); err != nil {
		t.Fatal(err)
	}
	return arquivo.Obras
}

// filtroDeJSON converte a árvore JSON em Filtro. Vive no TESTE, não no domínio:
// converter JSON é trabalho de adaptador, e o domínio não conhece o formato.
func filtroDeJSON(t *testing.T, m map[string]any) Filtro {
	t.Helper()
	switch m["tipo"] {
	case "tem":
		return Tem{Dimensao: texto(m["dimensao"]), Valor: texto(m["valor"])}
	case "ou":
		return Ou{Opcoes: filtrosDeJSON(t, m["opcoes"])}
	case "e":
		return E{Exigencias: filtrosDeJSON(t, m["exigencias"])}
	case "exceto":
		return Exceto{Filtro: filtroDeJSON(t, m["filtro"].(map[string]any))}
	case "ate":
		return Ate{Ano: int(m["ano"].(float64))}
	default:
		t.Fatalf("tipo de filtro desconhecido: %v", m["tipo"])
		return nil
	}
}

func filtrosDeJSON(t *testing.T, v any) []Filtro {
	itens, _ := v.([]any)
	fs := make([]Filtro, len(itens))
	for i, it := range itens {
		fs[i] = filtroDeJSON(t, it.(map[string]any))
	}
	return fs
}

func texto(v any) string {
	if s, ok := v.(string); ok {
		return s
	}
	return ""
}

func TestCasosCompartilhados(t *testing.T) {
	acervo := carregarAcervo(t)

	var arquivo struct {
		Casos []struct {
			Nome     string         `json:"nome"`
			Filtro   map[string]any `json:"filtro"`
			Esperado []string       `json:"esperado"`
		} `json:"casos"`
	}
	if err := json.Unmarshal(exemplos(t, "casos-de-busca.json"), &arquivo); err != nil {
		t.Fatal(err)
	}
	if len(arquivo.Casos) == 0 {
		t.Fatal("nenhum caso carregado de casos-de-busca.json")
	}

	for _, caso := range arquivo.Casos {
		t.Run(caso.Nome, func(t *testing.T) {
			obtido := make([]string, 0)
			for _, o := range Buscar(acervo, filtroDeJSON(t, caso.Filtro)) {
				obtido = append(obtido, o.ID)
			}
			if !reflect.DeepEqual(obtido, caso.Esperado) {
				t.Errorf("esperava %v, obteve %v", caso.Esperado, obtido)
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
