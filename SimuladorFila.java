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
    public ArrayList<FilaSimulacao> filas;
    public RandomMCL randomMCL;

    public SimuladorFila(RandomMCL randomMCL) {
        this.randomMCL = randomMCL;
        this.tempoAtual = 0;
        eventosAAcontecer = new PriorityQueue<>();
        eventosJaAconteceram = new PriorityQueue<>();
        this.filas = new ArrayList<>();
    }

    public void simular(Evento chegadaInicial, int eventos) {
        processarChegada(chegadaInicial);

        for (int i = 1; i < eventos; i++) {
            Evento evento = eventosAAcontecer.poll();
            if (evento == null)
                break;
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
                eventosAAcontecer.add(Evento.criarPassagemOuSaida(tempoAtual + fila1.gerarAtendimento(randomMCL), fila1,
                        fila1.getNextDestino()));
            }
        } else {
            fila1.Loss();
        }
        eventosAAcontecer
                .add(Evento.criarChegada(tempoAtual + fila1.gerarChegada(randomMCL), chegada.origem, chegada.destino));
    }

    public void acumulaTempo(Evento passagem) {
        for (FilaSimulacao f : filas) {
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
            eventosAAcontecer
                    .add(Evento.criarPassagemOuSaida(tempoAtual + fila1.gerarAtendimento(randomMCL), fila1, fila2));
        }
        if (fila2.Status() < fila2.Capacity() || fila2.Capacity() < 0) {
            fila2.In();
            if (fila2.Status() <= fila2.Servers()) {
                eventosAAcontecer.add(Evento.criarPassagemOuSaida(tempoAtual + fila2.gerarAtendimento(randomMCL), fila2,
                        fila2.getNextDestino()));
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
            eventosAAcontecer
                    .add(Evento.criarSaida(tempoAtual + fila2.gerarAtendimento(randomMCL), fila2,
                            fila2.getNextDestino()));
        }
    }

    public static void main(String[] args) {
        try {
            Yaml yaml = new Yaml();

            if (args.length == 0) {
                System.out.println("Por favor, forneça o caminho do arquivo config.yml como argumento.");
                return;
            }

            try (InputStream inputStream = new FileInputStream(args[0])) {
                Map<String, Object> config = yaml.load(inputStream);

                if (config == null) {
                    throw new RuntimeException("Arquivo config.yml está vazio ou inválido!");
                }

                
                System.out.println("=== Configuração YAML ===");
                System.out.println("Chegadas Iniciais:");
                
                List<Integer> seeds = (List<Integer>) config.get("seeds");
                if (seeds == null || seeds.isEmpty()) {
                    throw new RuntimeException("Nenhuma seed especificada no arquivo config.yml!");
                }
                int seed = seeds.get(0);
                RandomMCL random = new RandomMCL(seed);
                SimuladorFila simulador = new SimuladorFila(random);
                
                Map<String, Map<String, Object>> queues = (Map<String, Map<String, Object>>) config.get("queues");
                Map<String, FilaSimulacao> mapaFilas = new HashMap<>();
                for (Map.Entry<String, Map<String, Object>> entry : queues.entrySet()) {
                    String id = entry.getKey();
                    Map<String, Object> filaConfig = entry.getValue();
                    
                    int servidores = (int) filaConfig.get("servers");
                    int capacidade = (int) filaConfig.get("capacity");
                    
                    List<Double> chegada = new ArrayList<>();
                    if (filaConfig.containsKey("minArrival")) {
                        chegada.add(((Number) filaConfig.get("minArrival")).doubleValue());
                        chegada.add(((Number) filaConfig.get("maxArrival")).doubleValue());
                    }
                    
                    List<Double> atendimento = new ArrayList<>();
                    atendimento.add(((Number) filaConfig.get("minService")).doubleValue());
                    atendimento.add(((Number) filaConfig.get("maxService")).doubleValue());
                    
                    Map<Double, FilaSimulacao> probEDestinoMap = new HashMap<>();
                    FilaSimulacao fila = new FilaSimulacao(capacidade, servidores, chegada, atendimento,
                    probEDestinoMap);
                    fila.setId(id);
                    mapaFilas.put(id, fila);
                }
                
                Map<String, Double> arrivals = (Map<String, Double>) config.get("arrivals");
                
                Evento chegadaInicial = null;
                
                if (arrivals == null || arrivals.isEmpty()) {
                    throw new RuntimeException("Nenhuma chegada inicial especificada no arquivo config.yml!");
                }
                
                for (Map.Entry<String, Double> entry : arrivals.entrySet()) {
                    String id = entry.getKey();
                    double tempoChegada = entry.getValue();
                    chegadaInicial = Evento.criarChegadaInicial(tempoChegada, mapaFilas.get(id));
                }
                
                if (chegadaInicial == null) {
                    throw new RuntimeException("Nenhuma chegada inicial válida encontrada!");
                }
                
                List<Map<String, Object>> network = (List<Map<String, Object>>) config.get("network");
                for (Map<String, Object> conn : network) {
                    String origemId = (String) conn.get("source");
                    String destinoId = (String) conn.get("target");
                    double probabilidade = ((Number) conn.get("probability")).doubleValue();

                    FilaSimulacao origem = mapaFilas.get(origemId);
                    FilaSimulacao destino = mapaFilas.get(destinoId);

                    origem.probEDestinoMap.put(probabilidade, destino);
                }

                simulador.filas.addAll(mapaFilas.values());

                Integer rndNumbersPerSeed = (Integer) config.get("rndnumbersPerSeed");
                simulador.simular(chegadaInicial, rndNumbersPerSeed);
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

    public FilaSimulacao getNextDestino() {
        Double choice = RandomMCLNext.gerarEntre(0, 1);
        Double Sum = 0.0;
        FilaSimulacao destino = null;
        for (Map.Entry<Double, FilaSimulacao> probEDestino : probEDestinoMap.entrySet()) {
            double prob = probEDestino.getKey();
            Sum = Sum + prob;
            if (choice < Sum) {
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

    public void In() {
        customers++;
    }

    public void Out() {
        customers--;
    }

    public FilaSimulacao(int capacidadeFila, int servidores, List<Double> intervaloChegada,
            List<Double> intervaloAtendimento, Map<Double, FilaSimulacao> probEDestinoMap) {
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
            System.out.printf("Tamanho da fila: %d Tempo da fila no estado: %.2f Probabilidade: %.2f%% \n", i,
                    tempoNoEstado, (tempoNoEstado / tempoFinal) * 100);
        }
        System.out.println("--------------------------------------");

        System.out.println("Clientes perdidos: " + loss);
        System.out.println("Tempo total de simulação: " + tempoFinal);
    }

    public void atualizarTempoTamanhoFila(int tamanhoFila, double tempoDecorrido) {
        double tempoAnterior = tamanhoFilaETempo.getOrDefault(tamanhoFila, 0.0);
        tamanhoFilaETempo.put(tamanhoFila, tempoAnterior + tempoDecorrido);
    }

    public double gerarChegada(RandomMCL random) {
        if (intervaloChegada == null || intervaloChegada.size() != 2) {
            return 0; // Retorna 0 se não houver intervalo de chegada (como na fila2)
        }
        double chegadaMinima = intervaloChegada.get(0);
        double chegadaMaxima = intervaloChegada.get(1);
        return random.gerarEntre(chegadaMinima, chegadaMaxima);
    }

    public double gerarAtendimento(RandomMCL random) {
        if (intervaloAtendimento == null || intervaloAtendimento.size() != 2) {
            throw new IllegalArgumentException("Intervalo de atendimento inválido.");
        }
        double atendimentoMinimo = intervaloAtendimento.get(0);
        double atendimentoMaximo = intervaloAtendimento.get(1);
        return random.gerarEntre(atendimentoMinimo, atendimentoMaximo);
    }

    @Override
    public String toString() {
        return "[" + "capacity" + capacity + "intervalo Chegada" + this.intervaloChegada + "]";
    }
}

class RandomMCL {
    private static final int A = 13223;
    private static final int C = 34267;
    private static final int M = 99923;
    private int seed;

    public RandomMCL(int seed) {
        this.seed = seed;
    }

    private double gerarAleatorio() {
        seed = (A * seed + C) % M;
        return (double) seed / M;
    }

    public double gerarEntre(double minimo, double maximo) {
        return minimo + gerarAleatorio() * (maximo - minimo);
    }
}

class RandomMCLNext {
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
        if (destino == null)
            return Evento.criarSaida(tempo, origem, destino);
        return new Evento(tempo, Tipo.PASSAGEM, origem, destino);
    }

    public static Evento criarSaida(double tempo, FilaSimulacao origem, FilaSimulacao destino) {
        return new Evento(tempo, Tipo.SAIDA, origem, destino);
    }

    @Override
    public String toString() {
        return "Evento [tempo=" + tempo + ", tipo=" + tipo + "]";
    }

    @Override
    public int compareTo(Evento outro) {
        return Double.compare(tempo, outro.tempo);
    }
}