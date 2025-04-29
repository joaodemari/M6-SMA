import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

class SimuladorFilaM6 {
    private PriorityQueue<EventoM6> eventosAAcontecer;
    private PriorityQueue<EventoM6> eventosJaAconteceram;
    private double tempoAtual;
    private double chegadaInicial;
    private FilaSimulacaoM6 fila1;
    private FilaSimulacaoM6 fila2;

    public SimuladorFilaM6(double chegadaInicial, FilaSimulacaoM6 fila1, FilaSimulacaoM6 fila2) {
        this.tempoAtual = 0;
        eventosAAcontecer = new PriorityQueue<>();
        eventosJaAconteceram = new PriorityQueue<>();
        this.chegadaInicial = chegadaInicial;
        this.fila1 = fila1;
        this.fila2 = fila2;
    }


    public void simular(int eventos) {
        processarChegada(EventoM6.criarChegada(chegadaInicial));

        for (int i = 1; i < eventos; i++) {
            EventoM6 evento = eventosAAcontecer.poll();
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

    
    public void processarChegada(EventoM6 chegada) {
        acumulaTempo(chegada);
        if (fila1.Status() < fila1.Capacity()) {
            fila1.In();
            if (fila1.Status() <= fila1.Servers()) {
                eventosAAcontecer.add(EventoM6.criarPassagem(tempoAtual + fila1.gerarAtendimento()));
            }
        } else {
                fila1.Loss();
        }
        double tempoChegada = fila1.gerarChegada();
        EventoM6 eventoM6 = EventoM6.criarChegada(tempoAtual + tempoChegada);
        eventosAAcontecer.add(eventoM6);
    }

    public void acumulaTempo(EventoM6 passagem) {
        fila1.atualizarTempoTamanhoFila(fila1.Status(), passagem.tempo - tempoAtual);
        fila2.atualizarTempoTamanhoFila(fila2.Status(), passagem.tempo - tempoAtual);
        tempoAtual = passagem.tempo;
        eventosJaAconteceram.add(passagem);
    }


    public void processarPassagem(EventoM6 passagem) {
        acumulaTempo(passagem);
        fila1.Out();
        if (fila1.Status() >= fila1.Servers()) {
            eventosAAcontecer.add(EventoM6.criarPassagem(tempoAtual + fila1.gerarAtendimento()));
        }
        if (fila2.Status() < fila2.Capacity()) {
            fila2.In();
            if (fila2.Status() <= fila2.Servers()) {
                eventosAAcontecer.add(EventoM6.criarSaida(tempoAtual + fila2.gerarAtendimento()));
            }
        } else {
            fila2.Loss();
        }
    }

    
    public void processarSaida(EventoM6 saida) {
        acumulaTempo(saida);
        fila2.Out();
        if (fila2.Status() >= fila2.Servers()) {
            eventosAAcontecer.add(EventoM6.criarSaida(tempoAtual + fila2.gerarAtendimento()));
        }
    }

    


    public static void main(String[] args) {
        FilaSimulacaoM6 fila1 = new FilaSimulacaoM6(3, 2, List.of(1.0, 4.0), List.of(3.0, 4.0));
        FilaSimulacaoM6 fila2 = new FilaSimulacaoM6(5, 1, null, List.of(2.0, 3.0));

        SimuladorFilaM6 simulador = new SimuladorFilaM6(1.5, fila1, fila2);

        simulador.simular(100000);
    }

 
    public void mostrarPrimeirosEventos() {
        System.out.println("\n20 primeiros eventos da simulação:");
        eventosJaAconteceram.stream().limit(20).forEach(System.out::println);
    }
}

class FilaSimulacaoM6 {
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

    public FilaSimulacaoM6(int capacidadeFila, int servidores, List<Double> intervaloChegada , List<Double>  intervaloAtendimento) {
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
        double soma = 0.0;
        for (int i = 0; i <= capacity; i++) {
            soma += tamanhoFilaETempo.get(i);
        }
        System.out.println("Soma dos tempos: " + soma);
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

class RandomMCLM6 {
    private static final int AM6 = 13223;
    private static final int CM6 = 34267;
    private static final int MM6 = 99923;
    private static int seedM6 = 13213;

    private static double gerarAleatorio() {
        seedM6 = (AM6 * seedM6 + CM6) % MM6;
        double aleatorio = (double) seedM6 / MM6;
        return aleatorio;  
    }


    public static double gerarEntre(double minimo, double maximo) {
        return minimo + gerarAleatorio() * (maximo - minimo);
    }
}


enum TipoM6 {
  CHEGADA, SAIDA, PASSAGEM
}

class EventoM6 implements Comparable<EventoM6> {
    public double tempo;
    public Tipo tipo;

    
    private EventoM6(double tempo, Tipo tipo) {
        this.tempo = tempo;
        this.tipo = tipo;
    }

    public static EventoM6 criarChegada(double tempo) {
        return new EventoM6(tempo, Tipo.CHEGADA);
    }

    public static EventoM6 criarPassagem(double tempo) {
        return new EventoM6(tempo, Tipo.PASSAGEM);
    }

    public static EventoM6 criarSaida(double tempo) {
        return new EventoM6(tempo, Tipo.SAIDA);
    }
    
    @Override
    public String toString() {
        return "EventoM6 [tempo=" + tempo + ", tipo=" + tipo + "]";
    }

    @Override
    public int compareTo(EventoM6 outro) {
        return Double.compare(tempo, outro.tempo);
    }
}

