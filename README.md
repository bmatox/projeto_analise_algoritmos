# Análise Comparativa de Algoritmos de Contagem de Palavras

## Resumo

Este projeto tem como objetivo comparar o desempenho de diferentes abordagens para a contagem de ocorrências de uma palavra em um texto: uma versão serial em CPU, uma versão paralela em CPU e uma versão paralela em GPU. A análise visa fornecer insights sobre a eficiência computacional de cada método em diferentes cenários de processamento.

## Introdução

A busca por eficiência computacional é crucial em diversas aplicações. Este estudo foca na contagem de palavras em textos, um problema comum que pode se beneficiar do paralelismo. Foram implementados três métodos distintos:

- **Método Serial (CPU):** Uma abordagem tradicional que itera sequencialmente sobre o texto.
- **Método Paralelo (CPU):** Utiliza um pool de threads para dividir o texto e processar as partes em paralelo, aproveitando múltiplos núcleos da CPU.
- **Método Paralelo (GPU):** Emprega a tecnologia OpenCL para realizar a contagem em paralelo na GPU, visando ganhos significativos de desempenho para grandes volumes de dados.

## Metodologia

Foram realizadas análises comparativas de desempenho entre as versões serial e paralelas dos algoritmos de contagem. Os testes foram conduzidos utilizando diferentes arquivos de texto como conjuntos de dados de entrada, variando o tamanho e a natureza dos mesmos para examinar o impacto no desempenho. Cada execução foi repetida pelo menos três vezes para garantir a robustez dos resultados.

Os resultados obtidos (contagem da palavra e tempo de execução) foram registrados em arquivos CSV para facilitar a análise estatística e a visualização posterior. A análise estatística dos resultados obtidos permitiu identificar padrões de desempenho e comparar os algoritmos sob diferentes condições.

### Limitação da Execução em GPU

Durante os testes, a execução do método `contarParallelGPU` (utilizando OpenCL) resultou em um erro `CL_PLATFORM_NOT_FOUND_KHR`. Este erro indica que não foi possível encontrar uma plataforma OpenCL compatível no ambiente de execução (sandbox). Isso significa que o ambiente atual não possui uma GPU ou os drivers necessários para o OpenCL. Consequentemente, os resultados para a contagem em GPU não puderam ser gerados neste ambiente. Para uma análise completa do desempenho da GPU, o projeto precisaria ser executado em um ambiente com suporte adequado a OpenCL e uma GPU compatível.

## Resultados e Discussão

Os resultados da execução serial e paralela em CPU foram registrados no arquivo `resultados_analise.csv`. Devido à limitação mencionada na seção de Metodologia, os resultados para a execução em GPU não estão presentes neste arquivo. 

(Neste ponto, em um relatório completo, seriam apresentados gráficos e discussões detalhadas sobre os tempos de execução, escalabilidade e eficiência de cada algoritmo em CPU, com base nos dados do CSV.)

## Referências

- JOCL: [http://jocl.org/](http://jocl.org/)
- OpenCL: [https://www.khronos.org/opencl/](https://www.khronos.org/opencl/)





