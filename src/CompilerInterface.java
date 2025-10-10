import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Highlighter;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CompilerInterface extends JFrame {

    private static final Color COR_FUNDO_JANELA = new Color(220, 229, 242);
    private static final Color COR_BORDA_PAINEL = new Color(184, 198, 215);
    private static final Color COR_FUNDO_PAINEL = new Color(236, 242, 249);
    private static final Color COR_FUNDO_BARRA = new Color(209, 219, 233);
    private static final Color COR_FUNDO_LINHAS = new Color(229, 236, 245);
    private static final Color COR_TEXTO_LINHAS = new Color(120, 134, 150);
    private static final Color COR_DESTAQUE_LINHA = new Color(213, 226, 241);

    private JTextArea areaEdicao;
    private JTextArea areaMensagens;
    private JLabel rotuloStatus;
    private JLabel rotuloPosicao;
    private JFileChooser seletorArquivos;
    private String caminhoArquivoAtual;
    private boolean arquivoModificado = false;
    private boolean novoArquivo = true;
    private Object marcaLinhaAtual;
    private final Highlighter.HighlightPainter pincelLinhaAtual =
            new DefaultHighlighter.DefaultHighlightPainter(new Color(235, 242, 252));

    public CompilerInterface() {
        configurarJanela();
        configurarEditor();
        configurarSeletorArquivos();
        configurarMenus();
        configurarBarraFerramentas();
        configurarBarraStatus();
        adicionarOuvinteFechamento();
        atualizarTituloJanela(null);
    }

    private void configurarJanela() {
        setTitle("Compilador");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        getContentPane().setBackground(COR_FUNDO_JANELA);
    }

    private void configurarEditor() {
        areaEdicao = new JTextArea();
        areaEdicao.setFont(new Font("Consolas", Font.PLAIN, 14));
        areaEdicao.setBackground(Color.WHITE);
        areaEdicao.setForeground(new Color(20, 20, 20));
        areaEdicao.setCaretColor(new Color(0, 120, 215));
        areaEdicao.setSelectionColor(new Color(197, 222, 255));
        areaEdicao.setMargin(new Insets(8, 12, 8, 12));
        areaEdicao.setTabSize(4);
        areaEdicao.getDocument().addDocumentListener(new OuvinteDocumentoSimples(() -> {
            arquivoModificado = true;
            atualizarStatus("Editando...");
        }));

        areaMensagens = new JTextArea();
        areaMensagens.setEditable(false);
        areaMensagens.setFont(new Font("Consolas", Font.PLAIN, 12));
        areaMensagens.setBackground(Color.WHITE);
        areaMensagens.setForeground(new Color(40, 40, 40));
        areaMensagens.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        JScrollPane rolagemEdicao = new JScrollPane(areaEdicao);
        NumeracaoLinhas numeracaoLinhas = new NumeracaoLinhas(areaEdicao);
        numeracaoLinhas.setBackground(COR_FUNDO_LINHAS);
        numeracaoLinhas.setForeground(COR_TEXTO_LINHAS);
        rolagemEdicao.setRowHeaderView(numeracaoLinhas);
        rolagemEdicao.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(206, 214, 224)));

        JPanel painelEditor = new JPanel(new BorderLayout());
        painelEditor.setBackground(COR_FUNDO_PAINEL);
        painelEditor.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, COR_BORDA_PAINEL),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        JLabel rotuloEditor = new JLabel("Área para Edição");
        rotuloEditor.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rotuloEditor.setForeground(new Color(70, 86, 104));
        painelEditor.add(rotuloEditor, BorderLayout.NORTH);
        painelEditor.add(rolagemEdicao, BorderLayout.CENTER);

        JScrollPane rolagemMensagens = new JScrollPane(areaMensagens);
        rolagemMensagens.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, new Color(206, 214, 224)));
        rolagemMensagens.getViewport().setBackground(Color.WHITE);

        JPanel painelMensagens = new JPanel(new BorderLayout());
        painelMensagens.setBackground(COR_FUNDO_PAINEL);
        painelMensagens.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 1, 1, COR_BORDA_PAINEL),
                BorderFactory.createEmptyBorder(4, 4, 4, 4)
        ));
        JLabel rotuloMensagens = new JLabel("Área para Mensagens");
        rotuloMensagens.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rotuloMensagens.setForeground(new Color(70, 86, 104));
        painelMensagens.add(rotuloMensagens, BorderLayout.NORTH);
        painelMensagens.add(rolagemMensagens, BorderLayout.CENTER);

        JSplitPane painelDividido = new JSplitPane(JSplitPane.VERTICAL_SPLIT, painelEditor, painelMensagens);
        painelDividido.setResizeWeight(0.72);
        painelDividido.setDividerSize(10);
        painelDividido.setOneTouchExpandable(true);
        painelDividido.setBorder(BorderFactory.createEmptyBorder());

        JPanel envoltorioCentral = new JPanel(new BorderLayout());
        envoltorioCentral.setBackground(COR_FUNDO_JANELA);
        envoltorioCentral.setBorder(BorderFactory.createEmptyBorder(12, 14, 12, 14));
        envoltorioCentral.add(painelDividido, BorderLayout.CENTER);

        add(envoltorioCentral, BorderLayout.CENTER);

        areaEdicao.addCaretListener(e -> {
            atualizarPosicaoCursor();
            aplicarDestaqueLinhaAtual();
            numeracaoLinhas.repaint();
        });
        aplicarDestaqueLinhaAtual();
    }

    private void configurarMenus() {
        JMenuBar barraMenu = new JMenuBar();

        JMenu menuArquivo = new JMenu("Arquivo");
        menuArquivo.setMnemonic(KeyEvent.VK_A);
        JMenuItem itemNovo = criarItemMenu("Novo", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        JMenuItem itemAbrir = criarItemMenu("Abrir", KeyEvent.VK_B, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        JMenuItem itemSalvar = criarItemMenu("Salvar", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        JMenuItem itemSalvarComo = criarItemMenu("Salvar como...", KeyEvent.VK_M, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        JMenuItem itemSair = criarItemMenu("Sair", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));
        menuArquivo.add(itemNovo);
        menuArquivo.add(itemAbrir);
        menuArquivo.add(itemSalvar);
        menuArquivo.add(itemSalvarComo);
        menuArquivo.addSeparator();
        menuArquivo.add(itemSair);

        JMenu menuEdicao = new JMenu("Edição");
        menuEdicao.setMnemonic(KeyEvent.VK_E);
        JMenuItem itemRecortar = criarItemMenu("Recortar", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        JMenuItem itemCopiar = criarItemMenu("Copiar", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        JMenuItem itemColar = criarItemMenu("Colar", KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        JMenuItem itemLimparConsole = criarItemMenu("Limpar console", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        menuEdicao.add(itemRecortar);
        menuEdicao.add(itemCopiar);
        menuEdicao.add(itemColar);
        menuEdicao.addSeparator();
        menuEdicao.add(itemLimparConsole);

        JMenu menuCompilacao = new JMenu("Compilação");
        menuCompilacao.setMnemonic(KeyEvent.VK_C);
        JMenuItem itemCompilar = criarItemMenu("Compilar", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        JMenuItem itemExecutar = criarItemMenu("Executar", KeyEvent.VK_E, KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));
        menuCompilacao.add(itemCompilar);
        menuCompilacao.add(itemExecutar);

        barraMenu.add(menuArquivo);
        barraMenu.add(menuEdicao);
        barraMenu.add(menuCompilacao);
        setJMenuBar(barraMenu);

        itemNovo.addActionListener(e -> acaoNovoArquivo());
        itemAbrir.addActionListener(e -> acaoAbrirArquivo());
        itemSalvar.addActionListener(e -> acaoSalvarArquivo());
        itemSalvarComo.addActionListener(e -> acaoSalvarComo());
        itemSair.addActionListener(e -> acaoSair());
        itemRecortar.addActionListener(e -> areaEdicao.cut());
        itemCopiar.addActionListener(e -> areaEdicao.copy());
        itemColar.addActionListener(e -> areaEdicao.paste());
        itemLimparConsole.addActionListener(e -> limparConsole());
        itemCompilar.addActionListener(e -> acaoCompilar());
        itemExecutar.addActionListener(e -> acaoExecutar());
    }

    private JMenuItem criarItemMenu(String texto, int mnemonico, KeyStroke atalho) {
        JMenuItem item = new JMenuItem(texto, mnemonico);
        item.setAccelerator(atalho);
        return item;
    }

    private void configurarBarraFerramentas() {
        JToolBar barraFerramentas = new JToolBar();
        barraFerramentas.setFloatable(false);
        barraFerramentas.setRollover(true);
        barraFerramentas.setOpaque(true);
        barraFerramentas.setBackground(COR_FUNDO_BARRA);
        barraFerramentas.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, COR_BORDA_PAINEL),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)
        ));

        JButton botaoNovo = criarBotaoBarra("Novo arquivo (Ctrl+N)", criarIconeNovoArquivo());
        JButton botaoAbrir = criarBotaoBarra("Abrir arquivo (Ctrl+O)", criarIconeAbrirArquivo());
        JButton botaoSalvar = criarBotaoBarra("Salvar arquivo (Ctrl+S)", criarIconeSalvarArquivo());
        JButton botaoRecortar = criarBotaoBarra("Recortar (Ctrl+X)", criarIconeTesoura());
        JButton botaoCopiar = criarBotaoBarra("Copiar (Ctrl+C)", criarIconePrancheta());
        JButton botaoColar = criarBotaoBarra("Colar (Ctrl+V)", criarIconeColar());
        JButton botaoCompilar = criarBotaoBarra("Compilar (F9)", criarIconeMartelo());
        JButton botaoExecutar = criarBotaoBarra("Executar (F10)", criarIconeExecutar());
        JButton botaoLimpar = criarBotaoBarra("Limpar console", criarIconeBoia());

        botaoNovo.addActionListener(e -> acaoNovoArquivo());
        botaoAbrir.addActionListener(e -> acaoAbrirArquivo());
        botaoSalvar.addActionListener(e -> acaoSalvarArquivo());
        botaoRecortar.addActionListener(e -> areaEdicao.cut());
        botaoCopiar.addActionListener(e -> areaEdicao.copy());
        botaoColar.addActionListener(e -> areaEdicao.paste());
        botaoCompilar.addActionListener(e -> acaoCompilar());
        botaoExecutar.addActionListener(e -> acaoExecutar());
        botaoLimpar.addActionListener(e -> limparConsole());

        barraFerramentas.add(botaoNovo);
        barraFerramentas.add(botaoAbrir);
        barraFerramentas.add(botaoSalvar);
        barraFerramentas.addSeparator(new Dimension(12, 0));
        barraFerramentas.add(botaoRecortar);
        barraFerramentas.add(botaoCopiar);
        barraFerramentas.add(botaoColar);
        barraFerramentas.addSeparator(new Dimension(12, 0));
        barraFerramentas.add(botaoCompilar);
        barraFerramentas.add(botaoExecutar);
        barraFerramentas.add(Box.createHorizontalGlue());
        barraFerramentas.add(botaoLimpar);

        add(barraFerramentas, BorderLayout.NORTH);
    }

    private JButton criarBotaoBarra(String dica, Icon icone) {
        JButton botao = new JButton(icone);
        botao.setToolTipText(dica);
        botao.setFocusPainted(false);
        botao.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        botao.setContentAreaFilled(false);
        botao.setOpaque(false);
        botao.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        botao.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                botao.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(143, 164, 190), 1, true),
                        BorderFactory.createEmptyBorder(1, 1, 1, 1)
                ));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                botao.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
            }
        });
        return botao;
    }

    private void configurarBarraStatus() {
        JPanel painelStatus = new JPanel(new BorderLayout());
        painelStatus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, COR_BORDA_PAINEL),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)
        ));
        painelStatus.setBackground(COR_FUNDO_BARRA);

        rotuloStatus = new JLabel("Pronto");
        rotuloStatus.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rotuloStatus.setForeground(new Color(80, 96, 115));

        rotuloPosicao = new JLabel("Linha: 1, Coluna: 1");
        rotuloPosicao.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        rotuloPosicao.setForeground(new Color(80, 96, 115));

        painelStatus.add(rotuloStatus, BorderLayout.WEST);
        painelStatus.add(rotuloPosicao, BorderLayout.EAST);

        add(painelStatus, BorderLayout.SOUTH);
    }

    private void configurarSeletorArquivos() {
        seletorArquivos = new JFileChooser(System.getProperty("user.dir"));
        seletorArquivos.setFileFilter(new FiltroArquivosTexto());
        seletorArquivos.setAcceptAllFileFilterUsed(true);
    }

    private void adicionarOuvinteFechamento() {
        addWindowListener(new AdaptadorFechamentoJanela());
    }

    private void atualizarPosicaoCursor() {
        try {
            int posicao = areaEdicao.getCaretPosition();
            int linha = areaEdicao.getLineOfOffset(posicao) + 1;
            int coluna = posicao - areaEdicao.getLineStartOffset(linha - 1) + 1;
            rotuloPosicao.setText(String.format("Linha: %d, Coluna: %d", linha, coluna));
        } catch (Exception e) {
            rotuloPosicao.setText("Linha: 1, Coluna: 1");
        }
    }

    private void atualizarStatus(String mensagem) {
        if (rotuloStatus != null) {
            rotuloStatus.setText(mensagem);
        }
    }

    private void acaoNovoArquivo() {
        if (confirmarSalvamentoSeNecessario()) {
            limparInterface();
            atualizarStatus("Novo arquivo");
        }
    }

    private void acaoAbrirArquivo() {
        if (!confirmarSalvamentoSeNecessario()) {
            return;
        }
        if (seletorArquivos.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File arquivo = seletorArquivos.getSelectedFile();
            try {
                String conteudo = new String(Files.readAllBytes(arquivo.toPath()));
                areaEdicao.setText(conteudo);
                areaEdicao.setCaretPosition(0);
                caminhoArquivoAtual = arquivo.getAbsolutePath();
                novoArquivo = false;
                arquivoModificado = false;
                atualizarTituloJanela(caminhoArquivoAtual);
                areaMensagens.setText("Arquivo aberto: " + arquivo.getName() + "\n");
                atualizarStatus("Arquivo carregado");
                aplicarDestaqueLinhaAtual();
                areaEdicao.repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir o arquivo: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
                atualizarStatus("Erro ao abrir");
            }
        }
    }

    private void acaoSalvarArquivo() {
        if (novoArquivo || caminhoArquivoAtual == null) {
            acaoSalvarComo();
        } else {
            salvarArquivo(caminhoArquivoAtual);
        }
    }

    private void acaoSalvarComo() {
        if (seletorArquivos.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File arquivo = seletorArquivos.getSelectedFile();
            if (arquivo.exists()) {
                int opcao = JOptionPane.showConfirmDialog(this,
                        "O arquivo já existe. Deseja sobrescrevê-lo?",
                        "Confirmar sobrescrita", JOptionPane.YES_NO_OPTION);
                if (opcao != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            salvarArquivo(arquivo.getAbsolutePath());
        }
    }

    private void acaoSair() {
        if (confirmarSalvamentoSeNecessario()) {
            System.exit(0);
        }
    }

    private boolean confirmarSalvamentoSeNecessario() {
        if (!arquivoModificado) {
            return true;
        }
        int resposta = JOptionPane.showConfirmDialog(this,
                "O arquivo foi modificado. Deseja salvá-lo?",
                "Salvar alterações",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (resposta == JOptionPane.CANCEL_OPTION) {
            return false;
        }
        if (resposta == JOptionPane.YES_OPTION) {
            acaoSalvarArquivo();
            return !arquivoModificado;
        }
        return true;
    }

    private void limparInterface() {
        areaEdicao.setText("");
        areaMensagens.setText("");
        caminhoArquivoAtual = null;
        novoArquivo = true;
        arquivoModificado = false;
        atualizarTituloJanela(null);
        atualizarPosicaoCursor();
        aplicarDestaqueLinhaAtual();
    }

    private void limparConsole() {
        areaMensagens.setText("");
        atualizarStatus("Console limpo");
    }

    private void salvarArquivo(String caminho) {
        try {
            Files.write(Paths.get(caminho), areaEdicao.getText().getBytes());
            caminhoArquivoAtual = caminho;
            novoArquivo = false;
            arquivoModificado = false;
            atualizarTituloJanela(caminho);
            areaMensagens.setText("Arquivo salvo em: " + caminho + "\n");
            atualizarStatus("Arquivo salvo");
            aplicarDestaqueLinhaAtual();
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            atualizarStatus("Erro ao salvar");
        }
    }

    private void atualizarTituloJanela(String caminho) {
        String titulo = "Compilador";
        if (caminho != null) {
            titulo += " - " + new File(caminho).getName();
        }
        setTitle(titulo);
    }

    private void aplicarDestaqueLinhaAtual() {
        if (areaEdicao == null) {
            return;
        }
        Highlighter realcador = areaEdicao.getHighlighter();
        if (marcaLinhaAtual != null) {
            realcador.removeHighlight(marcaLinhaAtual);
            marcaLinhaAtual = null;
        }
        try {
            int posicao = areaEdicao.getCaretPosition();
            int linha = areaEdicao.getLineOfOffset(posicao);
            int inicio = areaEdicao.getLineStartOffset(linha);
            int fim = areaEdicao.getLineEndOffset(linha);
            marcaLinhaAtual = realcador.addHighlight(inicio, fim, pincelLinhaAtual);
        } catch (BadLocationException ex) {
            // ignora
        }
    }

    private void acaoCompilar() {
        String codigoFonte = areaEdicao.getText();
        if (codigoFonte.trim().isEmpty()) {
            areaMensagens.setText("O código-fonte está vazio.\n");
            atualizarStatus("Nada para compilar");
            return;
        }

        atualizarStatus("Compilando...");
        areaMensagens.setText("Compilando...\n");

        try {
            AnalisadorLexico analisador = new AnalisadorLexico(new StringReader(codigoFonte));
            StringBuilder saida = new StringBuilder();

            saida.append("Análise léxica concluída com sucesso:\n\n");
            // LARGURA DA CATEGORIA AUMENTADA AQUI
            saida.append(String.format("%-20s | %-7s | %-7s | %-60s | %-6s\n",
                    "Lexema", "Linha", "Coluna", "Categoria", "Código"));
            // TAMANHO DA LINHA SEPARADORA AUMENTADO
            saida.append(new String(new char[120]).replace('\0', '-')).append("\n");

            java.util.List<Token> tokens = new java.util.ArrayList<>();
            Token token;
            while ((token = analisador.getNextToken()).kind != AnalisadorLexico.EOF) {
                tokens.add(token);
            }

            for (int i = 0; i < tokens.size(); i++) {
                Token atual = tokens.get(i);
                if (atual.image.equals("/") && (i + 1) < tokens.size() && tokens.get(i + 1).image.equals("*")) {
                    saida.append(String.format("%-20s | %-7d | %-7d | %-60s | %-6s\n",
                            "/*", atual.beginLine, atual.beginColumn,
                            "ERRO LÉXICO: comentário de bloco não finalizado.", "-"));
                    break;
                }

                String categoria = obterNomeCategoria(atual);
                String codigo = categoria.startsWith("ERRO") ? "-" : String.valueOf(atual.kind);
                String lexema = atual.image.trim().replace("\n", "\\n").replace("\r", "\\r");
                // E LARGURA DA CATEGORIA AUMENTADA AQUI TAMBÉM
                saida.append(String.format("%-20s | %-7d | %-7d | %-60s | %-6s\n",
                        lexema, atual.beginLine, atual.beginColumn, categoria, codigo));
            }

            areaMensagens.setText(saida.toString());
            atualizarStatus("Compilação concluída");
        } catch (TokenMgrError erro) {
            areaMensagens.setText("ERRO LÉXICO:\n" + erro.getMessage());
            atualizarStatus("Erro na compilação");
        }
    }

    private String obterNomeCategoria(Token token) {
        switch (token.kind) {
            case AnalisadorLexico.ERRO_ID_INICIA_COM_DIGITO:
                return "ERRO LÉXICO: IDENTIFICADOR COMEÇANDO COM DIGITO";
            case AnalisadorLexico.ERRO_ID_DIGITOS_CONSECUTIVOS:
                return "ERRO LÉXICO: IDENTIFICADOR COM DIGITOS CONSECUTIVOS";
            case AnalisadorLexico.ERRO_ID_TERMINA_COM_DIGITO:
                return "ERRO LÉXICO: IDENTIFICADOR TERMINANDO COM DÍGITO";
            case AnalisadorLexico.ERRO_REAL_FRACAO_LONGA:
                return "ERRO LÉXICO: PARTE FRACIONÁRIA COM 3 OU MAIS DÍGITOS";
            case AnalisadorLexico.ERRO_REAL_INTEIRO_LONGO:
                return "ERRO LÉXICO: PARTE INTEIRA COM MAIS DE 2 DÍGITOS";
            case AnalisadorLexico.ERRO_REAL_INCOMPLETO:
                return "ERRO LÉXICO: PARTE FRACIONÁRIA INCOMPLETA";
            case AnalisadorLexico.ERRO_INT_LONGO:
                return "ERRO LÉXICO: PARTE INTEIRA COM 4 OU MAIS DÍGITOS";
            case AnalisadorLexico.BEGIN:
            case AnalisadorLexico.DEFINE:
            case AnalisadorLexico.START:
            case AnalisadorLexico.END:
            case AnalisadorLexico.SET:
            case AnalisadorLexico.READ:
            case AnalisadorLexico.SHOW:
            case AnalisadorLexico.IF:
            case AnalisadorLexico.THEN:
            case AnalisadorLexico.ELSE:
            case AnalisadorLexico.LOOP:
            case AnalisadorLexico.WHILE:
            case AnalisadorLexico.NUM:
            case AnalisadorLexico.REAL:
            case AnalisadorLexico.TEXT:
            case AnalisadorLexico.FLAG:
            case AnalisadorLexico.TRUE:
            case AnalisadorLexico.FALSE:
                return "PALAVRA RESERVADA";
            case AnalisadorLexico.IDENTIFIER:
                return "IDENTIFICADOR";
            case AnalisadorLexico.CONST_REAL:
                return "CONSTANTE NUMÉRICA REAL";
            case AnalisadorLexico.CONST_INT:
                return "CONSTANTE NUMÉRICA INTEIRA";
            case AnalisadorLexico.CONST_LITERAL:
                return "CONSTANTE LITERAL";
            case AnalisadorLexico.OP_REL_LTLT_EQ:
            case AnalisadorLexico.OP_REL_GTGT_EQ:
            case AnalisadorLexico.OP_REL_EQ:
            case AnalisadorLexico.OP_REL_NEQ:
            case AnalisadorLexico.OP_REL_LTLT:
            case AnalisadorLexico.OP_REL_GTGT:
            case AnalisadorLexico.OP_ARIT_POW:
            case AnalisadorLexico.OP_ARIT_DIVINT:
            case AnalisadorLexico.OP_ARIT_SUM:
            case AnalisadorLexico.OP_ARIT_SUB:
            case AnalisadorLexico.OP_ARIT_MUL:
            case AnalisadorLexico.OP_ARIT_DIV:
            case AnalisadorLexico.OP_ARIT_MOD:
            case AnalisadorLexico.OP_LOGIC_AND:
            case AnalisadorLexico.OP_LOGIC_OR:
            case AnalisadorLexico.OP_LOGIC_NOT:
            case AnalisadorLexico.ASSIGN:
            case AnalisadorLexico.SEMICOLON:
            case AnalisadorLexico.COMMA:
            case AnalisadorLexico.LPAREN:
            case AnalisadorLexico.RPAREN:
            case AnalisadorLexico.LBRACKET:
            case AnalisadorLexico.RBRACKET:
            case AnalisadorLexico.LBRACE:
            case AnalisadorLexico.RBRACE:
            case AnalisadorLexico.COLON:
            case AnalisadorLexico.DOT:
                return "SÍMBOLO ESPECIAL";
            case AnalisadorLexico.ERRO_LITERAL:
                return "ERRO LÉXICO: literal não finalizado";
            case AnalisadorLexico.ERRO_LEXICO:
                return "ERRO LÉXICO: Símbolo Inválido";
            default:
                String imagem = AnalisadorLexico.tokenImage[token.kind].replace("\"", "");
                return "NÃO CATEGORIZADO (" + imagem + ")";
        }
    }

    private void acaoExecutar() {
        areaMensagens.setText("Executando o programa...\n");
        areaMensagens.append("Funcionalidade de execução ainda não implementada.\n");
        atualizarStatus("Execução finalizada");
    }

    private BufferedImage criarImagemBase() {
        int tamanho = 30;
        BufferedImage imagem = new BufferedImage(tamanho, tamanho, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
        g2.setColor(new Color(222, 230, 241));
        g2.fillRoundRect(0, 0, tamanho, tamanho, 9, 9);
        g2.setColor(new Color(192, 204, 218));
        g2.setStroke(new BasicStroke(1.3f));
        g2.drawRoundRect(0, 0, tamanho - 1, tamanho - 1, 9, 9);
        g2.dispose();
        return imagem;
    }

    private Icon criarIconeNovoArquivo() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(194, 218, 247));
        g2.fillRoundRect(7, 5, 14, 18, 3, 3);
        g2.setColor(Color.WHITE);
        g2.fillRect(9, 7, 10, 14);
        g2.setColor(new Color(160, 186, 217));
        g2.drawRoundRect(7, 5, 14, 18, 3, 3);
        g2.setColor(new Color(194, 218, 247));
        g2.fillPolygon(new int[]{17, 21, 21}, new int[]{5, 9, 5}, 3);
        g2.setColor(new Color(110, 200, 110));
        g2.fillOval(16, 16, 10, 10);
        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawLine(21, 18, 21, 24);
        g2.drawLine(18, 21, 24, 21);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconeAbrirArquivo() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(252, 203, 77));
        g2.fillRoundRect(6, 11, 18, 11, 3, 3);
        g2.setColor(new Color(248, 183, 54));
        g2.fillPolygon(new int[]{6, 11, 27, 24}, new int[]{14, 8, 8, 14}, 4);
        g2.setColor(new Color(203, 150, 43));
        g2.setStroke(new BasicStroke(1.4f));
        g2.drawRoundRect(6, 11, 18, 11, 3, 3);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconeSalvarArquivo() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(84, 130, 191));
        g2.fillRoundRect(6, 6, 18, 18, 4, 4);
        g2.setColor(new Color(61, 94, 149));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(6, 6, 18, 18, 4, 4);
        g2.setColor(new Color(215, 222, 235));
        g2.fillRect(9, 9, 12, 6);
        g2.setColor(new Color(161, 170, 190));
        g2.fillRect(10, 10, 3, 4);
        g2.setColor(new Color(241, 142, 143));
        g2.fillRect(10, 16, 10, 5);
        g2.setColor(new Color(198, 102, 103));
        g2.drawRect(10, 16, 10, 5);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconeTesoura() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(111, 199, 236));
        g2.setStroke(new BasicStroke(2.2f));
        g2.drawLine(8, 9, 22, 21);
        g2.drawLine(8, 21, 22, 9);
        g2.setColor(new Color(84, 170, 205));
        g2.fillOval(7, 8, 6, 6);
        g2.fillOval(7, 18, 6, 6);
        g2.setColor(Color.WHITE);
        g2.fillOval(9, 10, 2, 2);
        g2.fillOval(9, 20, 2, 2);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconePrancheta() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(137, 203, 241));
        g2.fillRoundRect(9, 7, 14, 18, 4, 4);
        g2.setColor(Color.WHITE);
        g2.fillRoundRect(11, 10, 10, 14, 3, 3);
        g2.setColor(new Color(83, 155, 209));
        g2.drawRoundRect(11, 10, 10, 14, 3, 3);
        g2.setColor(new Color(203, 211, 222));
        g2.fillRect(12, 8, 8, 4);
        g2.setColor(new Color(156, 164, 176));
        g2.drawRect(12, 8, 8, 4);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconeColar() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(132, 206, 180));
        g2.fillRoundRect(8, 8, 14, 16, 4, 4);
        g2.setColor(Color.WHITE);
        g2.fillRect(11, 11, 8, 10);
        g2.setColor(new Color(88, 154, 135));
        g2.drawRect(11, 11, 8, 10);
        g2.setColor(new Color(181, 203, 194));
        g2.fillRect(12, 9, 6, 3);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconeMartelo() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(238, 184, 91));
        g2.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine(11, 10, 19, 22);
        g2.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(142, 158, 178));
        g2.drawLine(8, 12, 19, 12);
        g2.drawLine(12, 8, 8, 12);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconeExecutar() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(102, 178, 238));
        g2.fillOval(7, 7, 16, 16);
        g2.setColor(new Color(71, 137, 193));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawOval(7, 7, 16, 16);
        g2.setColor(Color.WHITE);
        g2.fillPolygon(new int[]{13, 13, 20}, new int[]{10, 20, 15}, 3);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private Icon criarIconeBoia() {
        BufferedImage imagem = criarImagemBase();
        Graphics2D g2 = imagem.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(new Color(233, 85, 139));
        g2.fillOval(6, 6, 18, 18);
        g2.setColor(Color.WHITE);
        g2.fillOval(10, 10, 10, 10);
        g2.setColor(new Color(185, 56, 110));
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(6, 6, 18, 18);
        g2.setStroke(new BasicStroke(3f));
        g2.drawArc(6, 6, 18, 18, 20, 40);
        g2.drawArc(6, 6, 18, 18, 110, 40);
        g2.drawArc(6, 6, 18, 18, 200, 40);
        g2.drawArc(6, 6, 18, 18, 290, 40);
        g2.dispose();
        return new ImageIcon(imagem);
    }

    private static class OuvinteDocumentoSimples implements DocumentListener {
        private final Runnable acao;

        OuvinteDocumentoSimples(Runnable acao) {
            this.acao = acao;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            acao.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            acao.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            acao.run();
        }
    }

    private class AdaptadorFechamentoJanela extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            acaoSair();
        }
    }

    private static class FiltroArquivosTexto extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) {
                return true;
            }
            String nome = f.getName().toLowerCase();
            return nome.endsWith(".txt") || nome.endsWith(".djt") || nome.endsWith(".cmp") || nome.endsWith(".java");
        }

        @Override
        public String getDescription() {
            return "Arquivos de texto (*.txt, *.djt, *.cmp, *.java)";
        }
    }

    class NumeracaoLinhas extends JPanel {
        private final JTextArea editor;

        NumeracaoLinhas(JTextArea editor) {
            this.editor = editor;
            setFont(editor.getFont());
            setBackground(COR_FUNDO_LINHAS);
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, COR_BORDA_PAINEL));
            editor.getDocument().addDocumentListener(new OuvinteDocumentoSimples(this::repaint));
            setPreferredSize(new Dimension(56, getPreferredSize().height));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics fm = g.getFontMetrics();
            Rectangle limite = g.getClipBounds();
            try {
                int inicio = editor.viewToModel2D(new Point(0, limite.y));
                int fim = editor.viewToModel2D(new Point(0, limite.y + limite.height));
                int linhaInicial = editor.getLineOfOffset(inicio);
                int linhaFinal = editor.getLineOfOffset(fim);

                for (int linha = linhaInicial; linha <= linhaFinal; linha++) {
                    Rectangle2D retangulo = editor.modelToView2D(editor.getLineStartOffset(linha));
                    if (retangulo != null) {
                        try {
                            int inicioLinha = editor.getLineStartOffset(linha);
                            int fimLinha = editor.getLineEndOffset(linha);
                            int caret = editor.getCaretPosition();
                            if (caret >= inicioLinha && caret < fimLinha) {
                                g.setColor(COR_DESTAQUE_LINHA);
                                g.fillRect(0, (int) retangulo.getY(), getWidth(), fm.getHeight());
                            }
                        } catch (BadLocationException ignored) {
                            // ignora
                        }
                        String numero = String.valueOf(linha + 1);
                        int x = getWidth() - fm.stringWidth(numero) - 6;
                        int y = (int) retangulo.getY() + fm.getAscent();
                        g.setColor(COR_TEXTO_LINHAS);
                        g.drawString(numero, x, y);
                    }
                }
            } catch (BadLocationException ex) {
                // ignora
            }
        }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new CompilerInterface().setVisible(true));
    }
}