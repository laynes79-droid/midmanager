# Estrutura de Agentes - Media Manager

Este documento define os papéis e responsabilidades dos diferentes agentes de software que trabalham no projeto Media Manager. O objetivo é garantir uma colaboração eficiente e manter a consistência entre as diferentes plataformas (Android e Web).

## Agentes

### 1. Agente Android

O **Agente Android** é o principal responsável pelo desenvolvimento e manutenção do aplicativo nativo para Android.

**Responsabilidades:**
- Implementar novas funcionalidades na interface de usuário (UI) e na lógica de negócios do app Android.
- Escrever código em Kotlin, seguindo as melhores práticas e a arquitetura definida (MVVM, Single-Activity).
- Utilizar Jetpack Compose para a construção de todas as telas e componentes.
- Garantir que a aplicação seja responsiva, fluida e visualmente agradável.
- Corrigir bugs específicos da plataforma Android.

### 2. Agente Web

O **Agente Web** é responsável pelo desenvolvimento de uma futura interface web para o Media Manager, permitindo que os usuários acessem suas mídias a partir de qualquer navegador.

**Responsabilidades:**
- Criar uma aplicação web front-end.
- Implementar funcionalidades equivalentes às do aplicativo Android.
- Garantir que a interface web seja responsiva e se adapte a diferentes tamanhos de tela (desktop, tablet, mobile).
- Manter a consistência visual e de marca com o aplicativo Android.

### 3. Agente Manager

O **Agente Manager** atua como um supervisor de qualidade e integração entre as plataformas. Ele não desenvolve funcionalidades diretamente, mas garante que o trabalho dos outros agentes esteja alinhado e com alta qualidade.

**Responsabilidades:**
- **Testes de Integração:** Verificar se as funcionalidades (como login e acesso a APIs de nuvem) funcionam corretamente em ambas as plataformas (Android e Web).
- **Testes de UI/UX:** Executar testes (automatizados e manuais) para garantir que as interfaces sejam visualmente consistentes, funcionais e sigam os mesmos fluxos de usuário.
- **Feedback e Qualidade:** Analisar os resultados dos testes e fornecer feedback detalhado aos agentes Android e Web para correções e melhorias.
- **Guardião da Consistência:** Validar que a experiência do usuário seja coesa, independentemente da plataforma utilizada.

## Princípios Gerais

- **Autenticação:** O método de login primário para todas as plataformas é o **Login com Google**. A implementação deve ser segura e seguir as diretrizes oficiais do Google para garantir a privacidade e a segurança dos dados do usuário.
- **Consistência:** As funcionalidades e a identidade visual devem ser consistentes entre as versões Android e Web do aplicativo, a menos que uma limitação específica da plataforma exija uma abordagem diferente.
