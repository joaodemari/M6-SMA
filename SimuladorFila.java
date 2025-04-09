import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

class SimuladorFila {
    private PriorityQueue<Evento> eventosAAcontecer;
    private PriorityQueue<Evento> eventosJaAconteceram;
    private double tempoAtual;
    private double chegadaInicial;
    private FilaSimulacao fila1;
    private FilaSimulacao fila2;

    public SimuladorFila(double chegadaInicial, FilaSimulacao fila1, FilaSimulacao fila2) {
        this.tempoAtual = 0;
        eventosAAcontecer = new PriorityQueue<>();
        eventosJaAconteceram = new PriorityQueue<>();
        this.chegadaInicial = chegadaInicial;
        this.fila1 = fila1;
        this.fila2 = fila2;
    }


    public void simular(int eventos) {
        processarChegada(Evento.criarChegada(chegadaInicial));

        for (int i = 1; i < eventos; i++) {
            Evento evento = eventosAAcontecer.poll();
            if (evento.tipo == Tipo.CHEGADA) {
                processarChegada(evento);
            } else if (evento.tipo == Tipo.SAIDA) {
                processarSaida(evento);
            }else if (evento.tipo == Tipo.PASSAGEM) {
                processarPassagem(evento);
            }
        }
        
        System.out.println("==========================================");
        System.out.println("Simulação Fila 1 (G/G/2/3):");
        System.out.println("chegada entre 1.0 e 4.0 - atendimento entre 3.0 e 4.0");
        System.out.println("----------------------------------------------");
        fila1.mostrarInformacoes(tempoAtual);
System.out.println();
        System.out.println("==========================================");
        System.out.println("Simulação Fila 2 (G/G/1/5):");
        System.out.println("atendimento entre 2.0 e 3.0");
        System.out.println("----------------------------------------------");
        fila2.mostrarInformacoes(tempoAtual);

        mostrarPrimeirosEventos();
    }

    
    public void processarChegada(Evento chegada) {
        acumulaTempo(chegada);
        if (fila1.Status() < fila1.Capacity()) {
            fila1.In();
            if (fila1.Status() <= fila1.Servers()) {
                eventosAAcontecer.add(Evento.criarPassagem(tempoAtual + fila1.gerarAtendimento()));
            }
        } else {
                fila1.Loss();
        }
        eventosAAcontecer.add(Evento.criarChegada(tempoAtual + fila1.gerarChegada()));
    }

    public void acumulaTempo(Evento passagem) {
        fila1.atualizarTempoTamanhoFila(fila1.Status(), passagem.tempo - tempoAtual);
        fila2.atualizarTempoTamanhoFila(fila2.Status(), passagem.tempo - tempoAtual);
        tempoAtual = passagem.tempo;
        eventosJaAconteceram.add(passagem);
    }


    public void processarPassagem(Evento passagem) {
        acumulaTempo(passagem);
        fila1.Out();
        if (fila1.Status() >= fila1.Servers()) {
            eventosAAcontecer.add(Evento.criarPassagem(tempoAtual + fila1.gerarAtendimento()));
        }
        if (fila2.Status() < fila2.Capacity()) {
            fila2.In();
            if (fila2.Status() <= fila2.Servers()) {
                eventosAAcontecer.add(Evento.criarSaida(tempoAtual + fila2.gerarAtendimento()));
            }
        } else {
            fila2.Loss();
        }
    }

    
    public void processarSaida(Evento saida) {
        acumulaTempo(saida);
        fila2.Out();
        if (fila2.Status() >= fila2.Servers()) {
            eventosAAcontecer.add(Evento.criarSaida(tempoAtual + fila2.gerarAtendimento()));
        }
    }

    


    public static void main(String[] args) {
        FilaSimulacao fila1 = new FilaSimulacao(3, 2, List.of(1.0, 4.0), List.of(3.0, 4.0));
        FilaSimulacao fila2 = new FilaSimulacao(5, 1, null, List.of(2.0, 3.0));

        SimuladorFila simulador = new SimuladorFila(1.5, fila1, fila2);

        simulador.simular(100000);
    }

 
    public void mostrarPrimeirosEventos() {
        System.out.println("\n20 primeiros eventos da simulação:");
        eventosJaAconteceram.stream().limit(20).forEach(System.out::println);
    }
}

class FilaSimulacao {
    private int capacity;
    private int customers;
    private int servers;
    private int loss;
    private HashMap<Integer, Double> tamanhoFilaETempo;
    private List<Double> intervaloAtendimento;
    private List<Double> intervaloChegada;

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

    public FilaSimulacao(int capacidadeFila, int servidores, List<Double> intervaloChegada , List<Double>  intervaloAtendimento) {
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
    }

    public void mostrarInformacoes(double tempoFinal) {
        System.out.println("Distribuição de probabilidades de tamanho de fila:");
        for (int i = 0; i <= capacity; i++) {
            double tempoNoEstado = tamanhoFilaETempo.get(i);
            System.out.println("Tamanho da fila: " + i + " Tempo da fila no estado: " + tempoNoEstado + " Probabilidade: " + (tempoNoEstado / tempoFinal) * 100 + "%");
        }
        System.out.println("--------------------------------------");

        System.out.println("Clientes perdidos: " + loss);
        System.out.println("Tempo final: " + tempoFinal);
    }

    public void atualizarTempoTamanhoFila(int tamanhoFila, double tempoDecorrido) {
        double tempoAnterior = tamanhoFilaETempo.get(tamanhoFila);
        tamanhoFilaETempo.put(tamanhoFila, tempoAnterior + tempoDecorrido);
    }


    public double gerarChegada() {
        if (intervaloChegada == null || intervaloChegada.size() != 2) {
            throw new IllegalArgumentException("Intervalo de chegada inválido.");
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


enum Tipo {
  CHEGADA, SAIDA, PASSAGEM
}

class Evento implements Comparable<Evento> {
    public double tempo;
    public Tipo tipo;

    
    private Evento(double tempo, Tipo tipo) {
        this.tempo = tempo;
        this.tipo = tipo;
    }

    public static Evento criarChegada(double tempo) {
        return new Evento(tempo, Tipo.CHEGADA);
    }

    public static Evento criarPassagem(double tempo) {
        return new Evento(tempo, Tipo.PASSAGEM);
    }

    public static Evento criarSaida(double tempo) {
        return new Evento(tempo, Tipo.SAIDA);
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

