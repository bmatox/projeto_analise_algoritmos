package com.analisealgoritmos;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import com.analisealgoritmos.ContadorPalavras.ResultadoContagem;

public class Main {

    private static final String DIRETORIO_AMOSTRAS = "/home/ubuntu/projeto_analise_algoritmos/amostras/";
    private static final String DIRETORIO_RESULTADOS = "/home/ubuntu/projeto_analise_algoritmos/resultados/";

    public static void main(String[] args) throws IOException, InterruptedException {
        // Cria o diretório de resultados se não existir
        new java.io.File(DIRETORIO_RESULTADOS).mkdirs();

        ContadorPalavras contador = new ContadorPalavras();

        List<String> arquivosAmostra = Arrays.asList(
            "DonQuixote-388208.txt",
            "Dracula-165307.txt",
            "MobyDick-217452.txt"
        );

        String palavraBuscada = "the"; // Palavra a ser buscada

        // Cabeçalho do CSV
        try (PrintWriter writer = new PrintWriter(new FileWriter(DIRETORIO_RESULTADOS + "resultados_analise.csv"))) {
            writer.println("Algoritmo,Arquivo,Palavra,Contagem,TempoExecucaoMs");

            for (String nomeArquivo : arquivosAmostra) {
                String caminhoCompletoArquivo = DIRETORIO_AMOSTRAS + nomeArquivo;
                System.out.println("\nAnalisando arquivo: " + nomeArquivo + " para a palavra: " + palavraBuscada);

                // Execução Serial CPU
                for (int i = 0; i < 3; i++) {
                    ResultadoContagem resultadoSerial = contador.contarSerialCPU(caminhoCompletoArquivo, palavraBuscada);
                    System.out.println(String.format("SerialCPU (%d): %d ocorrências em %d ms", i + 1, resultadoSerial.contagem, resultadoSerial.tempoExecucaoMs));
                    writer.println(String.format("SerialCPU,%s,%s,%d,%d", nomeArquivo, palavraBuscada, resultadoSerial.contagem, resultadoSerial.tempoExecucaoMs));
                }

                // Execução Paralela CPU
                for (int i = 0; i < 3; i++) {
                    ResultadoContagem resultadoParallelCPU = contador.contarParallelCPU(caminhoCompletoArquivo, palavraBuscada);
                    System.out.println(String.format("ParallelCPU (%d): %d ocorrências em %d ms", i + 1, resultadoParallelCPU.contagem, resultadoParallelCPU.tempoExecucaoMs));
                    writer.println(String.format("ParallelCPU,%s,%s,%d,%d", nomeArquivo, palavraBuscada, resultadoParallelCPU.contagem, resultadoParallelCPU.tempoExecucaoMs));
                }

                // Execução Paralela GPU
                // Nota: A implementação atual do kernel GPU pode não ser robusta para contagem de palavras complexas.
                // Pode ser necessário ajustar o kernel OpenCL para lidar com tokenização e correspondência de palavras de forma mais precisa.
                for (int i = 0; i < 3; i++) {
                    ResultadoContagem resultadoParallelGPU = contador.contarParallelGPU(caminhoCompletoArquivo, palavraBuscada);
                    System.out.println(String.format("ParallelGPU (%d): %d ocorrências em %d ms", i + 1, resultadoParallelGPU.contagem, resultadoParallelGPU.tempoExecucaoMs));
                    writer.println(String.format("ParallelGPU,%s,%s,%d,%d", nomeArquivo, palavraBuscada, resultadoParallelGPU.contagem, resultadoParallelGPU.tempoExecucaoMs));
                }
            }
            System.out.println("\nResultados salvos em: " + DIRETORIO_RESULTADOS + "resultados_analise.csv");
        }
    }
}


