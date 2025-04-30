import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import org.yaml.snakeyaml.Yaml;
import java.io.FileInputStream;
import java.io.InputStream;

class SimuladorFila {
    private PriorityQueue<Evento> eventosAAcontecer;
    private PriorityQueue<Evento> eventosJaAconteceram;
    private double tempoAtual;
    private double chegadaInicial;
    public ArrayList<FilaSimulacao> filas;

    public SimuladorFila(double chegadaInicial) {
        this.tempoAtual = 0;
        eventosAAcontecer = new PriorityQueue<>();
        eventosJaAconteceram = new PriorityQueue<>();
        this.chegadaInicial = chegadaInicial;
        this.filas = new ArrayList<>();
    }

    public void simular(int eventos) {
        processarChegada(Evento.criarChegadaInicial(chegadaInicial, filas.get(0)));

        for (int i = 1; i < eventos; i++) {
            Evento evento = eventosAAcontecer.poll();
            if (evento == null) break;
            if (evento.tipo == Tipo.CHEGADA) {
                processarChegada(evento);
            } else if (evento.tipo == Tipo.SAIDA) {
                processarSaida(evento);
            } else if (evento.tipo == Tipo.PASSAGEM) {
                processarPassagem(evento);
            }
        }
        
        for (FilaSimulacao f : filas) {
            f.mostrarInformacoes(tempoAtual);
        }

        mostrarPrimeirosEventos();
    }

    public void processarChegada(Evento chegada) {
        acumulaTempo(chegada);
        FilaSimulacao fila1 = chegada.destino;
        if (fila1 == null) {
            return;
        }
        if (fila1.Status() < fila1.Capacity() || fila1.Capacity() < 0) {
            fila1.In();
            if (fila1.Status() <= fila1.Servers()) {
                eventosAAcontecer.add(Evento.criarPassagemOuSaida(tempoAtual + fila1.gerarAtendimento(), fila1, fila1.getNextDestino()));
            }
        } else {
            fila1.Loss();
        }
        eventosAAcontecer.add(Evento.criarChegada(tempoAtual + fila1.gerarChegada(), chegada.origem, chegada.destino));
    }

    public void acumulaTempo(Evento passagem) {
        for(FilaSimulacao f: filas){
            f.atualizarTempoTamanhoFila(f.Status(), passagem.tempo - tempoAtual);
        }
        tempoAtual = passagem.tempo;
        eventosJaAconteceram.add(passagem);
    }

    public void processarPassagem(Evento passagem) {
        acumulaTempo(passagem);
        FilaSimulacao fila1 = passagem.origem;
        FilaSimulacao fila2 = passagem.destino;
        fila1.Out();
        if (fila1.Status() >= fila1.Servers()) {
            eventosAAcontecer.add(Evento.criarPassagemOuSaida(tempoAtual + fila1.gerarAtendimento(), fila1, fila2));
        }
        if (fila2.Status() < fila2.Capacity() || fila2.Capacity() < 0) {
            fila2.In();
            if (fila2.Status() <= fila2.Servers()) {
                eventosAAcontecer.add(Evento.criarPassagemOuSaida(tempoAtual + fila2.gerarAtendimento(), fila2, fila2.getNextDestino()));
            }
        } else {
            fila2.Loss();
        }
    }

    public void processarSaida(Evento saida) {
        acumulaTempo(saida);
        FilaSimulacao fila2 = saida.origem;
        fila2.Out();
        if (fila2.Status() >= fila2.Servers()) {
            eventosAAcontecer.add(Evento.criarSaida(tempoAtual + fila2.gerarAtendimento(), fila2, fila2.getNextDestino()));
        }
    }

    public static void main(String[] args) {
        try {
            Yaml yaml = new Yaml();
            
            try (InputStream inputStream = new FileInputStream("config.yml")) {
                Map<String, Object> config = yaml.load(inputStream);
                
                if (config == null) {
                    throw new RuntimeException("Arquivo config.yml está vazio ou inválido!");
                }
    
                Double chegadaInicial = (Double) config.get("chegadaInicial");
                Integer eventos = (Integer) config.get("eventos");
                List<Map<String, Object>> filasConfig = (List<Map<String, Object>>) config.get("filas");
    
                System.out.println("=== Valores lidos do config.yml ===");
                System.out.println("Chegada Inicial: " + chegadaInicial);
                System.out.println("Número de Eventos: " + eventos);
                System.out.println("Filas Configuradas:");
                for (Map<String, Object> filaConfig : filasConfig) {
                    System.out.println("  ID: " + filaConfig.get("id"));
                    System.out.println("  Capacidade: " + filaConfig.get("capacidade"));
                    System.out.println("  Servidores: " + filaConfig.get("servidores"));
                    System.out.println("  Chegada: " + filaConfig.get("chegada"));
                    System.out.println("  Atendimento: " + filaConfig.get("atendimento"));
                    System.out.println("  Destinos: " + filaConfig.get("destinos"));
                }
                System.out.println("==================================");
    
                SimuladorFila simulador = new SimuladorFila(chegadaInicial);
    
                Map<String, FilaSimulacao> mapaFilas = new HashMap<>();
    
                for (Map<String, Object> filaConfig : filasConfig) {
                    String id = (String) filaConfig.get("id");
                    Integer capacidade = (Integer) filaConfig.get("capacidade");
                    Integer servidores = (Integer) filaConfig.get("servidores");
                    List<Double> chegada = (List<Double>) filaConfig.get("chegada");
                    List<Double> atendimento = (List<Double>) filaConfig.get("atendimento");
                    Map<Double, String> destinos = (Map<Double, String>) filaConfig.get("destinos");
    
                    Map<Double, FilaSimulacao> probEDestinoMap = new HashMap<>();
                    FilaSimulacao fila = new FilaSimulacao(capacidade, servidores, chegada, atendimento, probEDestinoMap);
                    fila.setId(id);
                    mapaFilas.put(id, fila);
                }
    
                for (Map<String, Object> filaConfig : filasConfig) {
                    String id = (String) filaConfig.get("id");
                    Map<Double, String> destinos = (Map<Double, String>) filaConfig.get("destinos");
                    FilaSimulacao origem = mapaFilas.get(id);
    
                    for (Map.Entry<Double, String> entrada : destinos.entrySet()) {
                        Double probabilidade = entrada.getKey();
                        String destinoId = entrada.getValue();
                        FilaSimulacao destino = (destinoId == null) ? null : mapaFilas.get(destinoId);
                        origem.probEDestinoMap.put(probabilidade, destino);
                    }
                    simulador.filas.add(origem);
                }
    
                simulador.simular(eventos);
            }
        } catch (Exception e) {
            System.err.println("Erro ao ler o arquivo config.yml: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void mostrarPrimeirosEventos() {
        System.out.println("\n20 primeiros eventos da simulação:");
        eventosJaAconteceram.stream().limit(20).forEach(System.out::println);
    }
}

class FilaSimulacao {
    private String id;
    private int capacity;
    private int customers;
    private int servers;
    private int loss;
    private HashMap<Integer, Double> tamanhoFilaETempo;
    private List<Double> intervaloAtendimento;
    private List<Double> intervaloChegada;
    public Map<Double, FilaSimulacao> probEDestinoMap;
    
    public FilaSimulacao getNextDestino(){
        Double choice = RandomMCLNextDestino.gerarEntre(0,1);
        Double Sum = 0.0;
        FilaSimulacao destino = null;
        for(Map.Entry<Double, FilaSimulacao> probEDestino : probEDestinoMap.entrySet()){
            double prob = probEDestino.getKey();
            Sum = Sum + prob;
            if(choice < Sum){
                destino = probEDestino.getValue();
                break;
            }
        }
        return destino;
    }

    public int Status() {
        return customers;
    }

    public int Capacity() {
        return capacity;
    }

    public int Servers() {
        return servers;
    }

    public void Loss() {
        loss++;
    }

    public void In(){
        customers++;
    }

    public void Out(){
        customers--;
    }

    public FilaSimulacao(int capacidadeFila, int servidores, List<Double> intervaloChegada , List<Double> intervaloAtendimento, Map<Double, FilaSimulacao> probEDestinoMap) {
        this.capacity = capacidadeFila;
        this.servers = servidores;
        this.customers = 0;
        this.loss = 0;
        tamanhoFilaETempo = new HashMap<>();
            for (int i = 0; i <= capacidadeFila; i++) {
                tamanhoFilaETempo.put(i, 0.0);
            }
        this.intervaloChegada = intervaloChegada;
        this.intervaloAtendimento = intervaloAtendimento;
        this.probEDestinoMap = probEDestinoMap;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void mostrarInformacoes(double tempoFinal) {
        System.out.println("======================================");
        System.out.println("Fila: " + id);
        System.out.println("--------------------------------------");
        System.out.println("Distribuição de probabilidades de tamanho de fila:");
        for (int i = 0; i < tamanhoFilaETempo.entrySet().size(); i++) {
            double tempoNoEstado = tamanhoFilaETempo.get(i);
            System.out.println("Tamanho da fila: " + i + " Tempo da fila no estado: " + tempoNoEstado + " Probabilidade: " + (tempoNoEstado / tempoFinal) * 100 + "%");
        }
        System.out.println("--------------------------------------");

        System.out.println("Clientes perdidos: " + loss);
        System.out.println("Tempo total de simulação: " + tempoFinal);
    }

    public void atualizarTempoTamanhoFila(int tamanhoFila, double tempoDecorrido) {
        double tempoAnterior = tamanhoFilaETempo.getOrDefault(tamanhoFila, 0.0);
        tamanhoFilaETempo.put(tamanhoFila, tempoAnterior + tempoDecorrido);
    }

    public double gerarChegada() {
        if (intervaloChegada == null || intervaloChegada.size() != 2) {
            return 0; // Retorna 0 se não houver intervalo de chegada (como na fila2)
        }
        double chegadaMinima = intervaloChegada.get(0);
        double chegadaMaxima = intervaloChegada.get(1);
        return RandomMCL.gerarEntre(chegadaMinima, chegadaMaxima);
    }

    public double gerarAtendimento() {
        if (intervaloAtendimento == null || intervaloAtendimento.size() != 2) {
            throw new IllegalArgumentException("Intervalo de atendimento inválido.");
        }
        double atendimentoMinimo = intervaloAtendimento.get(0);
        double atendimentoMaximo = intervaloAtendimento.get(1);
        return RandomMCL.gerarEntre(atendimentoMinimo, atendimentoMaximo);
    }

    @Override
    public String toString(){
        return "["+ "capacity" +  capacity + "intervalo Chegada" + this.intervaloChegada + "]" ;
    }
}

class RandomMCL {
    private static final int A = 13223;
    private static final int C = 34267;
    private static final int M = 99923;
    private static int seed = 13213;

    private static double gerarAleatorio() {
        seed = (A * seed + C) % M;
        return (double) seed / M;
    }

    public static double gerarEntre(double minimo, double maximo) {
        return minimo + gerarAleatorio() * (maximo - minimo);
    }
}


class RandomMCLNextDestino {
    private static final int A = 13223;
    private static final int C = 34267;
    private static final int M = 99923;
    private static int seed = 13213;

    private static double gerarAleatorio() {
        seed = (A * seed + C) % M;
        double aleatorio = (double) seed / M;
        return aleatorio;
    }


    public static double gerarEntre(double minimo, double maximo) {
        return minimo + gerarAleatorio() * (maximo - minimo);
    }
}

enum Tipo {
    CHEGADA, SAIDA, PASSAGEM
}

class Evento implements Comparable<Evento> {
    public double tempo;
    public Tipo tipo;
    public FilaSimulacao origem;
    public FilaSimulacao destino;

    private Evento(double tempo, Tipo tipo, FilaSimulacao origem, FilaSimulacao destino) {
        this.tempo = tempo;
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
    }

    public static Evento criarChegada(double tempo, FilaSimulacao origem, FilaSimulacao destino) {
        return new Evento(tempo, Tipo.CHEGADA, origem, destino);
    }

    public static Evento criarChegadaInicial(double tempo, FilaSimulacao destino) {
        return new Evento(tempo, Tipo.CHEGADA, null, destino);
    }

    public static Evento criarPassagemOuSaida(double tempo, FilaSimulacao origem, FilaSimulacao destino) {
        if (destino == null) return Evento.criarSaida(tempo, origem, destino);
        return new Evento(tempo, Tipo.PASSAGEM, origem, destino);
    }

    public static Evento criarSaida(double tempo, FilaSimulacao origem, FilaSimulacao destino) {
        return new Evento(tempo, Tipo.SAIDA, origem, destino);
    }
    
    @Override
    public String toString() {
        return "Evento [tempo=" + tempo + ", tipo=" + tipo +"]";
    }

    @Override
    public int compareTo(Evento outro) {
        return Double.compare(tempo, outro.tempo);
    }
}