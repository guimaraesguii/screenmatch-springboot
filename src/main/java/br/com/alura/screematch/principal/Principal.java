package br.com.alura.screematch.principal;

import br.com.alura.screematch.model.*;
import br.com.alura.screematch.service.ConsumoAPI;
import br.com.alura.screematch.service.ConverteDados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);

    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";

    private ConsumoAPI consumoAPI = new ConsumoAPI();

    private ConverteDados conversor = new ConverteDados();

    public void exibeMenu() {
        System.out.println("Digite o nome da série que você deseja buscar: ");
        var nomeSerie = leitura.nextLine();
        var json = consumoAPI.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        System.out.println(dados);

        List<DadosTemporada> temporadas = new ArrayList<>();

        for (int i = 1; i <= dados.totalTemporadas(); i++) {
            json = consumoAPI.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + "&season=" + i + API_KEY);

            DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
            temporadas.add(dadosTemporada);
        }
        temporadas.forEach(System.out::println);

        for(int i = 0; i < dados.totalTemporadas(); i++){
            List<DadosEpisodio> episodiosTemporada = temporadas.get(i).episodios();
            for(int j = 0; j< episodiosTemporada.size(); j++){
                System.out.println(episodiosTemporada.get(j).titulo());
            }
        }

       temporadas.forEach(t -> t.episodios().forEach(e -> System.out.println(e.titulo())));

        List<DadosEpisodio> dadosEpisodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream())
                .collect(Collectors.toList());

        System.out.println("Top 10 episodios: ");

        dadosEpisodios.stream().filter(e -> !e.avaliacao().equalsIgnoreCase("N/A"))
                .peek(e -> System.out.println("Primeiro filtro: (N/A) " + e ))
                .sorted(Comparator.comparing(DadosEpisodio ::avaliacao).reversed())
                .peek(e -> System.out.println("Ordenando por avaliação: " + e ))
                .limit(10)
                .peek(e -> System.out.println("Limite: " + e ))
                .map(e -> e.titulo().toUpperCase())
                .peek(e -> System.out.println("Mapeamento: " + e ))
                .forEach(System.out::println);

        List<Episodio> episodios = temporadas.stream()
                .flatMap(t -> t.episodios().stream()
                        .map(d -> new Episodio(t.numero(), d))
                ).collect(Collectors.toList());

        episodios.forEach(System.out::println);

        System.out.println("Digite o nome do episodio: ");
        var trechoDoTitulo = leitura.nextLine();
        Optional<Episodio> episodioBuscavel = episodios.stream()
                .filter(e -> e.getTitulo().toUpperCase().contains(trechoDoTitulo.toUpperCase()))
                .findFirst();
        if(episodioBuscavel.isPresent()){
            System.out.println("Episodio encontrado!");
            System.out.println("Temporada: " + episodioBuscavel.get().getTemporada());
        }else {
            System.out.println("Episódio não encontrado");
        }
        System.out.println("A partir de qual ano você deseja ver os episodios? ");
        var ano = leitura.nextInt();
        leitura.nextLine();

        LocalDate dataBusca = LocalDate.of(ano,1,1);

        DateTimeFormatter formatador = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        episodios.stream()
                .filter(e -> e.getDataDeLancamento() != null && e.getDataDeLancamento().isAfter(dataBusca))
                .forEach(e -> System.out.println(
                        "Temporada: " + e.getTemporada() +
                                "Episódio: " + e.getTitulo() +
                                "Data de lançamento: " + e.getDataDeLancamento().format(formatador)
                ));

        Map<Integer, Double> avaliacaoTemporada = episodios.stream()
                .filter(e -> e.getAvaliacao() > 0.0)
                .collect(Collectors.groupingBy(Episodio::getTemporada,
                        Collectors.averagingDouble(Episodio::getAvaliacao)));

        System.out.println("Avaliação por temporada: " + avaliacaoTemporada);

        DoubleSummaryStatistics est = episodios.stream()
                .filter(e -> e.getAvaliacao() > 0.0)
                .collect(Collectors.summarizingDouble(Episodio::getAvaliacao));

        System.out.println("Média: " + est.getAverage());
        System.out.println("Melhor episodio: " + est.getMax());
        System.out.println("Pior episodio: " + est.getMin());
        System.out.println("Episodios analisados: " + est.getCount());

    }
}
