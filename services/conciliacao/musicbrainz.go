package conciliacao

import (
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"time"
)

// ClienteMusicBrainz consulta a API WS/2 do MusicBrainz, respeitando o limite de
// cerca de uma requisicao por segundo - ADR-0003. Sem dependencia externa: net/http
// e um intervalo minimo entre chamadas.
//
// Nao e seguro para uso concorrente: e feito para um lote sequencial.
type ClienteMusicBrainz struct {
	http      *http.Client
	base      string
	intervalo time.Duration
	proxima   time.Time
}

// NovoClienteMusicBrainz cria o cliente com o limite de taxa padrao (1 req/s).
func NovoClienteMusicBrainz() *ClienteMusicBrainz {
	return &ClienteMusicBrainz{
		http:      &http.Client{Timeout: 10 * time.Second},
		base:      "https://musicbrainz.org/ws/2",
		intervalo: time.Second,
	}
}

// respostaMB e o subconjunto da resposta do MusicBrainz que nos interessa.
type respostaMB struct {
	Recordings []struct {
		ID             string `json:"id"`
		Title          string `json:"title"`
		Disambiguation string `json:"disambiguation"`
		ArtistCredit   []struct {
			Name string `json:"name"`
		} `json:"artist-credit"`
	} `json:"recordings"`
}

// BuscarCandidatos consulta gravacoes por titulo e artista. Implementa CatalogoExterno.
func (c *ClienteMusicBrainz) BuscarCandidatos(ctx context.Context, titulo, artista string) ([]Candidato, error) {
	c.aguardarLimite()

	consulta := fmt.Sprintf(`recording:"%s" AND artist:"%s"`, titulo, artista)
	endereco := fmt.Sprintf("%s/recording?query=%s&fmt=json&limit=5", c.base, url.QueryEscape(consulta))

	req, err := http.NewRequestWithContext(ctx, http.MethodGet, endereco, nil)
	if err != nil {
		return nil, err
	}
	// O MusicBrainz exige um User-Agent identificavel.
	req.Header.Set("User-Agent", "MUSI/0.1 (https://github.com/fmarquesfilho/musi)")

	resp, err := c.http.Do(req)
	if err != nil {
		return nil, err
	}
	defer func() { _ = resp.Body.Close() }()
	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("musicbrainz respondeu %d", resp.StatusCode)
	}

	var dados respostaMB
	if err := json.NewDecoder(resp.Body).Decode(&dados); err != nil {
		return nil, err
	}

	candidatos := make([]Candidato, 0, len(dados.Recordings))
	for _, r := range dados.Recordings {
		nome := ""
		if len(r.ArtistCredit) > 0 {
			nome = r.ArtistCredit[0].Name
		}
		candidatos = append(candidatos, Candidato{
			Mbid:          r.ID,
			Titulo:        r.Title,
			Artista:       nome,
			Desambiguacao: r.Disambiguation,
		})
	}
	return candidatos, nil
}

// aguardarLimite garante o intervalo minimo entre requisicoes.
func (c *ClienteMusicBrainz) aguardarLimite() {
	if agora := time.Now(); agora.Before(c.proxima) {
		time.Sleep(c.proxima.Sub(agora))
	}
	c.proxima = time.Now().Add(c.intervalo)
}
