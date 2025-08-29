# Plano de Teste de Aceitação do Usuário (UAT) - Media Manager

## 1. Introdução

Obrigado por participar do teste do aplicativo Media Manager! O objetivo deste documento é guiá-lo através das funcionalidades atuais do aplicativo para que você possa validá-las e fornecer seu feedback.

Sua opinião é crucial para garantir que estamos construindo o produto certo.

## 2. Configuração Inicial (Setup)

Antes de começar, por favor, certifique-se de que você conseguiu compilar a versão mais recente do aplicativo e instalá-la em seu dispositivo Android ou emulador.

As instruções detalhadas para isso estão na seção **"Como Compilar e Usar"** do arquivo `README.md`.

## 3. Casos de Teste

Por favor, siga os cenários abaixo e marque-os conforme você os completa. Anote qualquer comportamento inesperado, erro ou sugestão de melhoria.

---

### ☐ **Teste 1: Primeiro Uso e Permissões**
1. Abra o aplicativo pela primeira vez.
2. **Verificação:** O aplicativo deve solicitar permissão para acessar suas mídias.
3. Clique em "Conceder Permissões".
4. **Verificação:** A caixa de diálogo de permissão do sistema Android deve aparecer. Conceda as permissões.
5. **Verificação:** O aplicativo deve avançar para a tela principal com as abas "Imagens", "Vídeos" e "Áudio".

---

### ☐ **Teste 2: Navegação e Visualização de Mídia**
1. Navegue entre as abas "Imagens", "Vídeos" e "Áudio".
2. **Verificação:** Cada aba deve exibir uma grade com os respectivos arquivos de mídia do seu dispositivo. Se você não tiver algum tipo de mídia (ex: nenhum áudio), a tela deve mostrar a mensagem "Nenhum item encontrado."
3. Role a tela para cima e para baixo em uma grade com muitos itens.
4. **Verificação:** A rolagem deve ser fluida.

---

### ☐ **Teste 3: Busca e Ordenação**
1. Na tela principal, use a barra de busca e digite parte do nome de um arquivo que você conhece.
2. **Verificação:** A grade deve ser filtrada em tempo real para mostrar apenas os arquivos que correspondem à sua busca.
3. Apague o texto da busca.
4. **Verificação:** A lista deve voltar a exibir todos os itens.
5. Na tela principal, clique nos botões de ordenação: "Data", "Nome" e "Tamanho".
6. **Verificação:** A ordem dos itens na grade deve mudar de acordo com a opção selecionada. A animação de reordenação deve ser sutil e fluida.

---

### ☐ **Teste 4: Tela de Detalhes e Player**
1. Toque em uma **imagem** na grade.
2. **Verificação:** A imagem deve abrir em tela cheia. Um botão de "voltar" deve estar visível no topo.
3. Volte para a tela principal.
4. Toque em um **vídeo** na grade.
5. **Verificação:** Um player de vídeo deve aparecer e começar a reproduzir o vídeo. Os controles de play, pause e a barra de progresso devem funcionar.
6. Volte para a tela principal.
7. Toque em um arquivo de **áudio**.
8. **Verificação:** Um player de áudio deve aparecer e começar a reproduzir o som.

---

### ☐ **Teste 5: Sistema de Tags**
1. Navegue para a tela de detalhes de qualquer arquivo (imagem, vídeo ou áudio).
2. Na parte de baixo, digite uma nova tag (ex: "teste" ou "favorito") no campo de texto e clique em "Add".
3. **Verificação:** A nova tag deve aparecer na lista de tags acima do campo de texto.
4. Adicione mais uma ou duas tags ao mesmo arquivo.
5. Volte para a tela principal.
6. Uma nova seção com os filtros de tag deve ter aparecido abaixo dos botões de ordenação.
7. Clique na tag que você acabou de criar.
8. **Verificação:** A grade deve ser filtrada para mostrar **apenas** o item (ou itens) que você marcou com aquela tag.
9. Clique na mesma tag novamente para desmarcar o filtro.
10. **Verificação:** A lista deve voltar a exibir todos os itens.

## 4. Feedback

Por favor, anote qualquer problema, bug, ou sugestão que você tiver durante os testes. Informações sobre o que você gostou e o que não gostou são extremamente valiosas.

Muito obrigado!
