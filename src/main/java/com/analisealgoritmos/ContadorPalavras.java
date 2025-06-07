package com.analisealgoritmos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

// Importações para JOCL
import org.jocl.CL;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;
import org.jocl.cl_command_queue;
import org.jocl.cl_program;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.Sizeof;
import org.jocl.Pointer;

public class ContadorPalavras {

    // Método para contagem serial na CPU
    public ResultadoContagem contarSerialCPU(String caminhoArquivo, String palavraBuscada) throws IOException {
        long inicioTempo = System.currentTimeMillis();
        String conteudo = new String(Files.readAllBytes(Paths.get(caminhoArquivo)));
        String[] palavras = conteudo.split("\\s+");
        long contagem = 0;
        for (String palavra : palavras) {
            if (palavra.equalsIgnoreCase(palavraBuscada)) {
                contagem++;
            }
        }
        long fimTempo = System.currentTimeMillis();
        return new ResultadoContagem(contagem, fimTempo - inicioTempo);
    }

    // Método para contagem paralela na CPU
    public ResultadoContagem contarParallelCPU(String caminhoArquivo, String palavraBuscada) throws IOException, InterruptedException {
        long inicioTempo = System.currentTimeMillis();
        String conteudo = new String(Files.readAllBytes(Paths.get(caminhoArquivo)));
        String[] palavras = conteudo.split("\\s+");

        int numNucleos = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numNucleos);
        AtomicLong contagemTotal = new AtomicLong(0);

        int tamanhoBloco = palavras.length / numNucleos;
        if (tamanhoBloco == 0) tamanhoBloco = 1; // Garante que cada thread tenha pelo menos uma palavra

        for (int i = 0; i < numNucleos; i++) {
            final int inicio = i * tamanhoBloco;
            final int fim = (i == numNucleos - 1) ? palavras.length : (i + 1) * tamanhoBloco;

            executor.submit(() -> {
                long contagemLocal = 0;
                for (int j = inicio; j < fim; j++) {
                    if (palavras[j].equalsIgnoreCase(palavraBuscada)) {
                        contagemLocal++;
                    }
                }
                contagemTotal.addAndGet(contagemLocal);
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        long fimTempo = System.currentTimeMillis();
        return new ResultadoContagem(contagemTotal.get(), fimTempo - inicioTempo);
    }

    // Método para contagem paralela na GPU
    public ResultadoContagem contarParallelGPU(String caminhoArquivo, String palavraBuscada) throws IOException {
        long inicioTempo = System.currentTimeMillis();
        String conteudo = new String(Files.readAllBytes(Paths.get(caminhoArquivo)));
        String[] palavras = conteudo.split("\\s+");

        // Converte as palavras para um formato que pode ser processado pela GPU (por exemplo, um único string grande)
        StringBuilder sb = new StringBuilder();
        for (String palavra : palavras) {
            sb.append(palavra).append(" ");
        }
        String textoParaGPU = sb.toString();

        // Kernel OpenCL para contagem de palavras
        String kernelSource = "" +
            "__kernel void contarPalavras(__global const char* texto, \"" +
            "                                __global const char* palavraBuscada, \"" +
            "                                __global int* resultado, \"" +
            "                                int tamanhoTexto, \"" +
            "                                int tamanhoPalavraBuscada) \"" +
            "{\n" +
            "    int gid = get_global_id(0);\n" +
            "    int contagemLocal = 0;\n" +
            "    for (int i = gid; i < tamanhoTexto - tamanhoPalavraBuscada + 1; i += get_global_size(0))\n" +
            "    {\n" +
            "        bool match = true;\n" +
            "        for (int j = 0; j < tamanhoPalavraBuscada; j++)\n" +
            "        {\n" +
            "            if (tolower(texto[i+j]) != tolower(palavraBuscada[j]))\n" +
            "            {\n" +
            "                match = false;\n" +
            "                break;\n" +
            "            }\n" +
            "        }\n" +
            "        if (match)\n" +
            "        {\n" +
            "            contagemLocal++;\n" +
            "        }\n" +
            "    }\n" +
            "    atomic_add(resultado, contagemLocal);\n" +
            "}";

        // Inicializa OpenCL
        final int platformIndex = 0;
        final long deviceType = CL.CL_DEVICE_TYPE_ALL;
        final int deviceIndex = 0;

        // Habilita o tratamento de erros do JOCL
        CL.setExceptionsEnabled(true);

        // Obtém a plataforma e o dispositivo
        cl_platform_id[] platforms = new cl_platform_id[1];
        CL.clGetPlatformIDs(1, platforms, null);
        cl_platform_id platform = platforms[platformIndex];

        cl_device_id[] devices = new cl_device_id[1];
        CL.clGetDeviceIDs(platform, deviceType, 1, devices, null);
        cl_device_id device = devices[deviceIndex];

        // Cria o contexto OpenCL
        cl_context_properties contextProperties = new cl_context_properties();
        contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);
        cl_context context = CL.clCreateContext(contextProperties, 1, new cl_device_id[]{device}, null, null, null);

        // Cria a fila de comandos
        cl_command_queue commandQueue = CL.clCreateCommandQueue(context, device, 0, null);

        // Cria o programa a partir do código fonte do kernel
        cl_program program = CL.clCreateProgramWithSource(context, 1, new String[]{ kernelSource }, null, null);

        // Compila o programa
        CL.clBuildProgram(program, 0, null, "-cl-std=CL1.2", null, null);

        // Cria o kernel
        cl_kernel kernel = CL.clCreateKernel(program, "contarPalavras", null);

        // Aloca memória para os buffers de entrada e saída na GPU
        byte[] textoBytes = textoParaGPU.getBytes();
        byte[] palavraBuscadaBytes = palavraBuscada.getBytes();

        cl_mem memTexto = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, (long)textoBytes.length * Sizeof.cl_char, Pointer.to(textoBytes), null);
        cl_mem memPalavraBuscada = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, (long)palavraBuscadaBytes.length * Sizeof.cl_char, Pointer.to(palavraBuscadaBytes), null);
        cl_mem memResultado = CL.clCreateBuffer(context, CL.CL_MEM_READ_WRITE, Sizeof.cl_int, null, null);

        // Define os argumentos do kernel
        CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memTexto));
        CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memPalavraBuscada));
        CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memResultado));
        CL.clSetKernelArg(kernel, 3, Sizeof.cl_int, Pointer.to(new int[]{textoBytes.length}));
        CL.clSetKernelArg(kernel, 4, Sizeof.cl_int, Pointer.to(new int[]{palavraBuscadaBytes.length}));

        // Define o tamanho do trabalho global e local
        long[] globalWorkSize = new long[]{ 1024 }; // Exemplo: 1024 threads
        long[] localWorkSize = new long[]{ 64 };  // Exemplo: 64 threads por grupo

        // Executa o kernel
        CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, null);

        // Lê o resultado de volta para a CPU
        int[] resultado = new int[1];
        CL.clEnqueueReadBuffer(commandQueue, memResultado, CL.CL_TRUE, 0, (long)Sizeof.cl_int, Pointer.to(resultado), 0, null, null);

        // Libera os recursos do OpenCL
        CL.clReleaseMemObject(memTexto);
        CL.clReleaseMemObject(memPalavraBuscada);
        CL.clReleaseMemObject(memResultado);
        CL.clReleaseKernel(kernel);
        CL.clReleaseProgram(program);
        CL.clReleaseCommandQueue(commandQueue);
        CL.clReleaseContext(context);

        long fimTempo = System.currentTimeMillis();
        return new ResultadoContagem(resultado[0], fimTempo - inicioTempo);
    }

    // Classe interna para armazenar o resultado da contagem e o tempo de execução
    public static class ResultadoContagem {
        public final long contagem;
        public final long tempoExecucaoMs;

        public ResultadoContagem(long contagem, long tempoExecucaoMs) {
            this.contagem = contagem;
            this.tempoExecucaoMs = tempoExecucaoMs;
        }
    }
}


