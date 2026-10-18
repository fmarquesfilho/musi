package conciliacao

import "testing"

// Demonstração da aula de 19/10 de Processos de Software: um teste que falha de
// propósito, para mostrar o portão da main. Este PR não deve ser integrado.
func TestDemonstracaoDoPortao(t *testing.T) {
	esperado, obtido := 2, 1+2
	if obtido != esperado {
		t.Fatalf("teste quebrado de propósito: esperado %d, obtido %d", esperado, obtido)
	}
}
