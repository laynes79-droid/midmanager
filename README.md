# Media Manager

## Sobre o Projeto

Media Manager é um aplicativo Android nativo projetado para ser um gerenciador de mídias completo e elegante. O objetivo é fornecer uma interface fluida e poderosa para que os usuários possam navegar, gerenciar e filtrar todas as suas mídias, tanto as que estão no dispositivo local quanto as de serviços de nuvem.

Este projeto está sendo construído de forma incremental, com foco em uma arquitetura moderna e nas melhores práticas de desenvolvimento Android.

## Funcionalidades Atuais

Até o momento, o aplicativo funciona como uma galeria local avançada, com as seguintes funcionalidades:

- **Visualização de Mídias Locais:** Navegue por todas as suas imagens, vídeos e áudios locais em abas separadas.
- **Interface Moderna:** A UI utiliza um layout de grade e um tema escuro customizado para uma experiência de uso agradável.
- **Player Integrado:** Toque em qualquer mídia para visualizá-la em tela cheia. Imagens são exibidas diretamente, e vídeos e áudios são reproduzidos usando um player integrado (ExoPlayer).
- **Busca por Nome:** Encontre arquivos rapidamente usando a barra de busca para filtrar por nome.
- **Ordenação Avançada:** Organize suas mídias por Data, Nome ou Tamanho.
- **Animações Fluidas:** A interface conta com animações sutis ao reordenar os itens na grade, tornando a experiência mais dinâmica.

## Como Compilar e Usar

Para compilar e executar este projeto em seu próprio ambiente, siga os passos abaixo.

### Pré-requisitos

- **Java Development Kit (JDK) 17:** O projeto requer o JDK 17. Recomendamos o [OpenJDK da Adoptium](https://adoptium.net/temurin/releases/?version=17).
- **Android Studio:** A forma mais fácil de obter o Android SDK e as ferramentas de build necessárias. Baixe em [developer.android.com/studio](https://developer.android.com/studio).
- **Git:** Para clonar o repositório.

### Passos para Compilação (Windows)

1.  **Clone o Repositório:**
    ```bash
    git clone <URL_DO_REPOSITORIO>
    cd media-manager
    ```

2.  **Configure o Android SDK:**
    - Após instalar o Android Studio, ele irá baixar o Android SDK. A localização padrão é `C:\Users\<SeuUsuario>\AppData\Local\Android\Sdk`.
    - Na raiz do projeto, crie um arquivo chamado `local.properties`.
    - Adicione a seguinte linha a este arquivo, substituindo `<SeuUsuario>` pelo seu nome de usuário do Windows e usando barras duplas:
      ```properties
      sdk.dir=C:\\Users\\<SeuUsuario>\\AppData\\Local\\Android\\Sdk
      ```

3.  **Configure o JAVA_HOME (se necessário):**
    - Se o comando `java -version` não funcionar no seu terminal, siga as instruções [neste guia](https://www.java.com/pt-BR/download/help/path.html) para configurar a variável de ambiente `JAVA_HOME`.

4.  **Compile o Projeto:**
    - Abra um terminal na pasta raiz do projeto.
    - Execute o seguinte comando para gerar o APK de depuração:
      ```bash
      gradlew.bat assembleDebug
      ```

5.  **Encontre e Instale o APK:**
    - Após a compilação bem-sucedida, o arquivo APK estará localizado em:
      `app\build\outputs\apk\debug\app-debug.apk`
    - Você pode copiar este arquivo para um dispositivo Android e instalá-lo.

## Arquitetura e Documentação Técnica

O projeto segue uma arquitetura moderna baseada em UI declarativa e um padrão MVVM (Model-View-ViewModel) simplificado.

- **Linguagem:** 100% [Kotlin](https://kotlinlang.org/), incluindo Coroutines para operações assíncronas.
- **UI:** A interface é construída inteiramente com [Jetpack Compose](https://developer.android.com/jetpack/compose), o toolkit moderno do Android para UI declarativa.
- **Arquitetura:**
  - **Single-Activity:** O app usa um modelo de atividade única (`MainActivity.kt`) que hospeda todos os composables.
  - **ViewModel (`MediaViewModel.kt`):** Centraliza a lógica de negócios, como a busca de mídias e o gerenciamento dos estados de filtro e ordenação.
  - **Navegação:** Utiliza o [Navigation Compose](https://developer.android.com/jetpack/compose/navigation) para gerenciar a transição entre as telas (Permissões, Tela Principal, Tela de Detalhes).

### Principais Bibliotecas

- **Jetpack Compose:** O coração da UI.
- **Coil:** Para carregamento e cache de imagens de forma eficiente e simples.
- **ExoPlayer (Media3):** Para a reprodução de vídeo e áudio.
- **Navigation Compose:** Para a navegação entre as telas do app.
- **Gradle:** Como sistema de automação de build.
