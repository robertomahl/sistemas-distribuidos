## Peer-to-peer

- Surgiram para suportar compartilhamento de dados em grande escala
- Plataformas de middleware
    - Garantia de integridade via funções de resumo segura para gerar os GUIDs
    - Garantia de disponibilidade via replicação em vários nós e algoritmos de roteamento tolerante a falhas
- Arquivos cifrados podem ajudar a alegar desconhecimento de conteúdo

Vantagens:
- Exploração de recursos ociosos
- Escalabidade 
- Excelente harmonização de carga nos enlaces de rede e recursos computacionais
- Custos de suporte independentes dos números de clientes e nós implantados

Deficiências:
- Menos útil para armazenamento de dados mutáveis
- Falta de garantia de anonimato de clientes e nós

### 1ª Geração
- Índices centralizados e replicados
- Gargalos: descoberta e endereçamento de objetos
### 2ª Geração
- Rede descentralizada
- Grande proteção do anonimato
### 3ª Geração
- Rapidez de acesso aos recursos - replicação objetiva
- Disponibilidade de recursos


### Sobreposição de roteamento

Principal tecnologia: GUIDs. Usados para identificar nós e objetos sem revelar nada sobre a localização dos objetos a que se referem

Servidor de localização mantém o conhecimento da localização de todas as réplicas disponíveis e distribui as requisições para o nó ativo mais próximo que tenha uma cópia do objeto relevante. Faz o roteamento de cada requisição por uma sequência de nós, explorando o conhecimento existente em cada um deles para localizar o objeto de destino.

Além disso, faz:
- Inserção e remoção de objetos
- Adição e remoção de nós

Os GUIDs são calculados a partir de funções de resumo (hashes). A partir disso, os clientes podem assegurar sua validade aplicando a função hash aos dados recebidos e comparando com o GUID.


#### Resumo resposta

No processo de sobreposição de roteamento, um servidor de localização utiliza um Globally Unique IDentifier, obtido a partir da aplicação de uma função hash sobre o objeto ou parte dele, para indicar o nodo ativo mais próximo que possua uma réplica válida do objeto buscado. Assim, pode-se traçar uma rota até o IP de destino com base nas informações de caminho disponíveis nos nós. Serviços de indexação como o Distributed Hash Table podem auxiliar a associar nomes legíveis por humanos aos respectivos GUIDs.

## NTP

- Network Time Protocol
- Permite escalabilidade
- Define arquitetura para um serviço de tempo e um protocolo para distribuir informações de tempo pela **internet**
- Não garante sincronia precisa com UTC
- Usa servidores e caminhos **redundantes** = serviço confiável
- Usa técnicas de **autenticação** para verificar se os dados de temporização são originários das fontes confiáveis conhecidas
- **Mensagens** são enviadas de maneira **não confiável** (UDP)
- Nenhum limite é garantido para a diferença de tempo entre relógios porque a comunicação ainda é passível de atrasos ou falhas (UDP), ainda que haja tentativas de correção de delay

## Relógios lógicos

- Sem relação com relógios físicos
- Relógios físicos precisariam ser sincronizados para assegurar a relação de ordem parcial
- Um contador por nodo
    - Cada processo envia seu estado junto à mensagem
    - Ao receber uma mensagem, seu estado passa a ser 1 + max(estado_atual, estado_recebido)

## Acordo distribuído

Coordenador: Diminui significativamente a complexidade da eleição pois o coordenador faz um multicast para todos nós, que respondem apenas para ele. Assim, diminui-se a quantidade de mensagens necessárias por acordo.

## Exclusão mútua e eleição

Função: escolher um processo que deverá liderar ou coordenar um algoritmo distribuído

- Líder: não defeituoso e com maior id
- Um processo pi não convoca mais de uma eleição por vez, mas dois processos pi e pj podem convocar concorrentemente

Eficiência:
    - Número de mensagens enviadas
    - Tempo do ciclo do algoritmo

### Chang e Roberts

- Baseado em anel
- Considera o maior id
- Modelo:
    - **Não ocorrem falhas**
    - **Assíncrono**
    - O processo apenas recebe do antecessor e envia para o sucessor
    - O processo não conhece o id dos outros processos
    - Sucessor: p(i+1) mod n
    - O maior id é passado adiante
1. Início de uma eleição, com o processo convocante se definindo participante e enviando o seu id para o sucessor
2. Ao receber uma eleição, verifica se o id recebido é maior ou menor que o seu
3. Comunica o eleito para os sucessores

**3N - 1** mensagens no pior caso sem concorrência:
- N mensagens ELEICAO para chegar ao futuro coordenador, que não
dispara a eleição por que ainda não completou o anel.
- N-1 mensagens ELEICAO para percorrer o anel e chegar ao coord.
- N mensagens ELEITO para concluir a eleição 

A ausência de concorrência indica que apenas um processo dispara uma eleição.

### Garcia-Molina - Valentão - Bully

- Modelo:
    - **Processos podem falhar**
    - Comunicação confiável
    - **Síncrono** (usa timeouts)
    - O processo conhece o id dos outros processos
    - O processo pode se comunicar com qualquer processo

1. Quando um processo percebe que o coordenador não está respondendo às requisições, ele inicia uma eleição comunicando todos processos com id maior que o seu
2. Os processos que receberam a solicitação de eleição respondem com OK
3. Caso um processo não responda, então não é válido
4. Dentre os que receberam, continuam a eleição até que reste apenas um
5. O novo eleito comunica que é o novo coordenador a todos os nós

## Cortes

Um corte C é consistente se, para cada evento que ele contém, ele também contém todos os eventos que aconteceram antes desse evento:

## Acordo distribuído

Acordo bizantino: um processo inicia o valor e um único valor é acordado
Consenso: todos os processos iniciam um valor e u único valor é acordado
Consistência iterativa: todos os processos iniciam um valor e um vetor de vlaores é acordado

### Acordo bizantino

Acordo:
- Todos processos não falhos concordam sobre o mesmo valor v
Validade:
- Se o processo fonte é correto, então todos os processos corretos devem concordar sobre o valor v proposto pelo processo fonte

### Consenso

Acordo: 
- Todos processos não falhos concordam sobre o mesmo valor v
Validade: 
- Se o valor inicial de todo processo não falho é v, então todos os processos corretos devem concordar sobre o valor v

### Consistência iterativa

Acordo:
- Todos processos não falhos concordam sobre o mesmo vetor v = (v1, v2, ..., vn)
Validade:
- Se o valor inicial do i-ésimo processo correto é vi, então todos processos corretos devem concordar sobre o valor vi

**Não há qualquer protocolo que garanta acordo entre dois processos na presença de perda de mensagens.**

Qualquer algoritmo de acordo tem uma probabilidade não nula de falhar.
