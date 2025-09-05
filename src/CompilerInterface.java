import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.StringReader;

public class CompilerInterface extends JFrame {

    private JTextArea editorArea;
    private JTextArea messagesArea;
    private JLabel statusLabel;
    private String currentFilePath;
    private boolean isFileEdited = false;

    public CompilerInterface() {
        // Propriedades da janela
        setTitle("Compilador");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(1024, 768);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Áreas de edição e mensagens
        editorArea = new JTextArea();
        editorArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        // Usar uma fonte monoespaçada para alinhar a tabela corretamente
        messagesArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        // Adiciona a numeração de linha à área de edição
        JScrollPane editorScrollPane = new JScrollPane(editorArea);
        TextLineNumber tln = new TextLineNumber(editorArea);
        editorScrollPane.setRowHeaderView(tln);

        // Painel dividido para as áreas de edição e mensagens
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorScrollPane, new JScrollPane(messagesArea));
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);

        // Barra de status para o contador de linha/coluna
        statusLabel = new JLabel("Linha: 1, Coluna: 1");
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        // Adiciona listeners para atualizar o status e o título
        editorArea.addCaretListener(new CaretListener() {
            @Override
            public void caretUpdate(CaretEvent e) {
                try {
                    int caretPosition = editorArea.getCaretPosition();
                    int lineNumber = editorArea.getLineOfOffset(caretPosition);
                    int columnNumber = caretPosition - editorArea.getLineStartOffset(lineNumber);
                    statusLabel.setText("Linha: " + (lineNumber + 1) + ", Coluna: " + (columnNumber + 1));
                } catch (BadLocationException ex) {
                    ex.printStackTrace();
                }
            }
        });

        editorArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { updateFileStatus(); }
            public void removeUpdate(DocumentEvent e) { updateFileStatus(); }
            public void changedUpdate(DocumentEvent e) { updateFileStatus(); }
            private void updateFileStatus() {
                isFileEdited = true;
            }
        });

        // Configura menus e barra de ferramentas
        setupMenus();
        setupToolbar();

        // Lida com o fechamento da janela
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                handleExitAction();
            }
        });

        updateWindowTitle(null);
    }

    private void setupMenus() {
        JMenuBar menuBar = new JMenuBar();

        // Menu Arquivo
        JMenu fileMenu = new JMenu("Arquivo");
        JMenuItem newItem = new JMenuItem("Novo");
        JMenuItem openItem = new JMenuItem("Abrir");
        JMenuItem saveItem = new JMenuItem("Salvar");
        JMenuItem saveAsItem = new JMenuItem("Salvar como");
        JMenuItem exitItem = new JMenuItem("Sair");

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        // Menu Edição
        JMenu editMenu = new JMenu("Edição");
        JMenuItem copyItem = new JMenuItem("Copiar");
        JMenuItem cutItem = new JMenuItem("Recortar");
        JMenuItem pasteItem = new JMenuItem("Colar");

        editMenu.add(copyItem);
        editMenu.add(cutItem);
        editMenu.add(pasteItem);

        // Menu Compilação
        JMenu compileMenu = new JMenu("Compilação");
        JMenuItem compileItem = new JMenuItem("Compilar");

        compileMenu.add(compileItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(compileMenu);

        setJMenuBar(menuBar);

        // Adiciona listeners para os itens de menu
        newItem.addActionListener(e -> handleNewAction());
        openItem.addActionListener(e -> handleOpenAction());
        saveItem.addActionListener(e -> handleSaveAction());
        saveAsItem.addActionListener(e -> handleSaveAsAction());
        exitItem.addActionListener(e -> handleExitAction());
        compileItem.addActionListener(e -> compileSource());
        copyItem.addActionListener(e -> editorArea.copy());
        cutItem.addActionListener(e -> editorArea.cut());
        pasteItem.addActionListener(e -> editorArea.paste());
    }

    private void setupToolbar() {
        JToolBar toolBar = new JToolBar();
        JButton compileButton = new JButton("Compilar");
        compileButton.addActionListener(e -> compileSource());
        toolBar.add(compileButton);
        add(toolBar, BorderLayout.NORTH);
    }

    /**
     * Pega o código-fonte, executa a análise léxica e exibe os resultados formatados.
     */
    private void compileSource() {
        String sourceCode = editorArea.getText();
        if (sourceCode.trim().isEmpty()) {
            messagesArea.setText("O código-fonte está vazio.");
            return;
        }

        try {
            AnalisadorLexico parser = new AnalisadorLexico(new StringReader(sourceCode));
            StringBuilder output = new StringBuilder();

            // Cabeçalho da tabela
            output.append(String.format("%-20s | %-7s | %-7s | %-45s | %s\n",
                    "Lexema", "Linha", "Coluna", "Categoria", "Código"));
            output.append(new String(new char[100]).replace('\0', '-')).append("\n");

            Token t;
            while ((t = parser.getNextToken()).kind != AnalisadorLexico.EOF) {
                String category = getCategoryName(t);
                String code = category.startsWith("ERRO") ? "-" : String.valueOf(t.kind);

                output.append(String.format("%-20s | %-7d | %-7d | %-45s | %s\n",
                        t.image, t.beginLine, t.beginColumn, category, code));
            }

            messagesArea.setText("Análise léxica concluída com sucesso:\n\n" + output.toString());

        } catch (TokenMgrError e) {
            messagesArea.setText("ERRO LÉXICO GRAVE:\n" + e.getMessage());
        }
    }
    /**
     * Mapeia o 'kind' (código) de um token para uma categoria legível.
     * @param t O Token a ser analisado.
     * @return Uma String com o nome da categoria.
     */
    /**
     * Mapeia o 'kind' (código) de um token para uma categoria legível.
     */
    /**
     * Mapeia o 'kind' (código) de um token para uma categoria legível,
     * com sub-categorias para operadores.
     */
    private String getCategoryName(Token t) {
        String tokenName = AnalisadorLexico.tokenImage[t.kind].replace("\"", "").replace("<", "").replace(">", "");

        switch (tokenName.toUpperCase()) {
            // --- PALAVRAS RESERVADAS ---
            case "PALAVRA_RESERVADA":
                return "PALAVRA RESERVADA";

            // --- IDENTIFICADOR ---
            case "IDENTIFIER":
                return "IDENTIFICADOR";

            // --- CONSTANTES ---
            case "CONST_REAL":
                return "CONSTANTE NUMÉRICA REAL";
            case "CONST_INT":
                return "CONSTANTE NUMÉRICA INTEIRA";
            case "CONST_LITERAL":
                return "CONSTANTE LITERAL";

            // --- OPERADORES (AGORA SUB-CATEGORIZADOS) ---
            case "OP_ARITMETICO":
                return "OPERADOR ARITMÉTICO";

            case "OP_RELACIONAL":
                return "OPERADOR RELACIONAL";

            case "OP_LOGICO":
                return "OPERADOR LÓGICO";

            // --- SÍMBOLOS ESPECIAIS (NÃO OPERADORES) ---
            case "SIMBOLO_ESPECIAL":
                return "SÍMBOLO ESPECIAL";

            // --- ERROS ---
            case "INVALID_IDENTIFIER":
                return "ERRO LÉXICO: identificador inválido";
            case "ERRO_LITERAL":
                return "ERRO LÉXICO: constante literal não finalizada";
            case "ERRO_LEXICO":
                return "ERRO LÉXICO: símbolo inválido";
            default:
                return tokenName; // Categoria padrão caso não se encaixe
        }
    }


    // --- Métodos de manipulação de arquivos (sem alterações) ---

    private void handleNewAction() {
        if (isFileEdited) {
            int result = showSaveConfirmationDialog();
            if (result == JOptionPane.YES_OPTION) {
                handleSaveAction();
                clearInterface();
            } else if (result == JOptionPane.NO_OPTION) {
                clearInterface();
            }
        } else {
            clearInterface();
        }
    }

    private void handleOpenAction() {
        if (isFileEdited) {
            int result = showSaveConfirmationDialog();
            if (result == JOptionPane.YES_OPTION) {
                handleSaveAction();
                openFile();
            } else if (result == JOptionPane.NO_OPTION) {
                openFile();
            }
        } else {
            openFile();
        }
    }

    private void handleSaveAction() {
        // Lógica de salvar o arquivo
        System.out.println("Salvar action invoked");
        isFileEdited = false;
    }

    private void handleSaveAsAction() {
        // Lógica de "salvar como"
        System.out.println("Salvar como action invoked");
        isFileEdited = false;
    }

    private void handleExitAction() {
        if (isFileEdited) {
            int result = showSaveConfirmationDialog();
            if (result == JOptionPane.YES_OPTION) {
                handleSaveAction();
                System.exit(0);
            } else if (result == JOptionPane.NO_OPTION) {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    private int showSaveConfirmationDialog() {
        return JOptionPane.showConfirmDialog(this,
                "O arquivo foi modificado. Deseja salvá-lo?", "Salvar arquivo",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    private void clearInterface() {
        editorArea.setText("");
        messagesArea.setText("");
        updateWindowTitle(null);
        isFileEdited = false;
    }

    private void openFile() {
        System.out.println("Abrir action invoked");
        updateWindowTitle("Caminho/para/Arquivo.djt");
    }

    private void updateWindowTitle(String fileName) {
        currentFilePath = fileName;
        if (fileName != null) {
            setTitle("Compilador - " + new File(fileName).getName());
        } else {
            setTitle("Compilador");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new CompilerInterface().setVisible(true);
        });
    }

    /**
     * Classe auxiliar para desenhar os números das linhas ao lado do JTextArea.
     */
    class TextLineNumber extends JPanel {
        private final JTextArea editor;

        public TextLineNumber(JTextArea editor) {
            this.editor = editor;
            setFont(editor.getFont());
            setBackground(new Color(240, 240, 240));
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY));
            editor.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    repaint();
                }
                @Override
                public void removeUpdate(DocumentEvent e) {
                    repaint();
                }
                @Override
                public void changedUpdate(DocumentEvent e) {
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            FontMetrics fm = g.getFontMetrics();
            int fontHeight = fm.getHeight();

            Rectangle visibleRect = editor.getVisibleRect();
            int startOffset = editor.viewToModel(new Point(0, visibleRect.y));
            int endOffset = editor.viewToModel(new Point(0, visibleRect.y + visibleRect.height));

            try {
                int startLine = editor.getLineOfOffset(startOffset);
                int endLine = editor.getLineOfOffset(endOffset);

                for (int i = startLine; i <= endLine; i++) {
                    int y = editor.modelToView(editor.getLineStartOffset(i)).y + fm.getAscent();
                    String lineNumber = String.valueOf(i + 1);
                    int x = getWidth() - fm.stringWidth(lineNumber) - 5;
                    g.drawString(lineNumber, x, y);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }
}