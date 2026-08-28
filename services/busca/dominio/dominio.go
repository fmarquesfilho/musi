// Package dominio implementa o modelo do MUSI em Go.
//
// DIM0547 - Desenvolvimento de Sistemas Web II.
//
// Esta e a camada de domínio: nao importa gRPC, banco nem HTTP.
// A regra de dependencia e verificada por arch-go no CI - ver docs/STACK.md.
//
// Vale comparar com as versoes em Kotlin e Java. A principal diferenca esta no
// tipo Filtro: Go nao tem uniao etiquetada, entao o compilador nao verifica se
// todos os casos foram tratados. Ver o comentario em Satisfaz(). Essa comparacao
// faz parte do conteudo da disciplina - ADR-0002.
package dominio

import (
	"fmt"
	"strings"
)

// Faceta e um par (dimensao, valor). A lista de dimensoes nao e fixa - ADR-0002.
type Faceta struct {
	Dimensao string `json:"dimensao"`
	Valor    string `json:"valor"`
}

type Obra struct {
	ID      string   `json:"id"`
	Titulo  string   `json:"titulo"`
	Artista string   `json:"artista"`
	Ano     int      `json:"ano"`
	Facetas []Faceta `json:"facetas"`

	// Identidade global, preenchida na conciliacao com o MusicBrainz - ADR-0003.
	// Vazio ate a obra ser conciliada; a busca nao depende deles.
	Mbid           string `json:"mbid,omitempty"`           // a gravacao (recording)
	MbidComposicao string `json:"mbidComposicao,omitempty"` // a composicao (work)
}

// Filtro e a arvore de criterios - ADR-0002.
//
// Em Kotlin isto e `sealed interface`; em Java 21, `sealed interface` + records.
// Em Go, o idioma mais proximo e uma interface com um metodo nao exportado:
// so tipos deste pacote podem implementa-la, o que fecha a hierarquia.
//
// O que nao se obtem aqui e a exaustividade. Um `switch` sobre Filtro que deixe
// um caso de fora compila normalmente e falha em tempo de execucao. O recurso
// disponivel e um `default` que sinaliza o caso nao tratado, e ele so atua
// quando o codigo roda.
//
// Esse e o compromisso: Go oferece simplicidade e compilacao rapida; Kotlin e
// Java oferecem verificacao. As duas abordagens sao validas.
type Filtro interface {
	isFiltro()
}

type Tem struct {
	Dimensao string
	Valor    string
}

type Ou struct {
	Opcoes []Filtro
}

type E struct {
	Exigencias []Filtro
}

type Exceto struct {
	Filtro Filtro
}

type Ate struct {
	Ano int
}

func (Tem) isFiltro()    {}
func (Ou) isFiltro()     {}
func (E) isFiltro()      {}
func (Exceto) isFiltro() {}
func (Ate) isFiltro()    {}

// Satisfaz avalia o filtro contra uma obra.
//
// O ramo `default` existe porque o compilador de Go nao verifica exaustividade.
// Nas versoes Kotlin e Java ele nao aparece, ja que o compilador cobre todos os
// casos.
func Satisfaz(o Obra, f Filtro) bool {
	switch t := f.(type) {
	case Tem:
		for _, faceta := range o.Facetas {
			if faceta.Dimensao == t.Dimensao && faceta.Valor == t.Valor {
				return true
			}
		}
		return false
	case Ou:
		for _, opcao := range t.Opcoes {
			if Satisfaz(o, opcao) {
				return true
			}
		}
		return false
	case E:
		for _, exigencia := range t.Exigencias {
			if !Satisfaz(o, exigencia) {
				return false
			}
		}
		return true
	case Exceto:
		return !Satisfaz(o, t.Filtro)
	case Ate:
		return o.Ano <= t.Ano
	default:
		// Alcancavel caso um construtor novo seja acrescentado sem atualizar este switch.
		panic(fmt.Sprintf("filtro nao tratado: %T", f))
	}
}

// Descrever produz uma representacao legivel do filtro.
func Descrever(f Filtro) string {
	switch t := f.(type) {
	case Tem:
		return t.Dimensao + "=" + t.Valor
	case Ou:
		return juntar(t.Opcoes, " ou ")
	case E:
		return juntar(t.Exigencias, " e ")
	case Exceto:
		return "nao " + Descrever(t.Filtro)
	case Ate:
		return fmt.Sprintf("ano<=%d", t.Ano)
	default:
		panic(fmt.Sprintf("filtro nao tratado: %T", f))
	}
}

func juntar(filtros []Filtro, separador string) string {
	partes := make([]string, len(filtros))
	for i, f := range filtros {
		partes[i] = Descrever(f)
	}
	return "(" + strings.Join(partes, separador) + ")"
}

// A ordenacao e sempre declarada por quem consulta - ADR-0002.
// Nao ha um valor OrdenacaoRelevancia, e essa ausencia e deliberada.
type Ordenacao int

const (
	OrdenacaoTitulo Ordenacao = iota
	OrdenacaoArtista
	OrdenacaoAnoCrescente
	OrdenacaoAnoDecrescente
)

// Buscar filtra o acervo. A ordenacao fica a cargo de quem chama.
func Buscar(acervo []Obra, f Filtro) []Obra {
	resultado := make([]Obra, 0, len(acervo))
	for _, o := range acervo {
		if Satisfaz(o, f) {
			resultado = append(resultado, o)
		}
	}
	return resultado
}
