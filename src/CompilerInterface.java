import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class CompilerInterface extends JFrame {

    private JTextArea editorArea;
    private JTextArea messagesArea;
    private String currentFilePath;
    private boolean isFileEdited = false;
    private boolean isNewFile = true;
    private JFileChooser fileChooser;

    public CompilerInterface() {
        setTitle("Compilador");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Componentes Visuais
        editorArea = new JTextArea();
        editorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        editorArea.setBackground(new Color(255, 255, 255));
        editorArea.setForeground(new Color(51, 51, 51));
        editorArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        editorArea.setCaretColor(new Color(0, 120, 215));

        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        messagesArea.setBackground(new Color(250, 247, 247));
        messagesArea.setForeground(new Color(64, 64, 64));
        messagesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane editorScrollPane = new JScrollPane(editorArea);
        TextLineNumber tln = new TextLineNumber(editorArea);
        editorScrollPane.setRowHeaderView(tln);

        JPanel editorPanel = new JPanel(new BorderLayout());
        JLabel editorLabel = new JLabel("Área para Edição");
        editorLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        editorPanel.add(editorLabel, BorderLayout.NORTH);
        editorPanel.add(editorScrollPane, BorderLayout.CENTER);

        JPanel messagesPanel = new JPanel(new BorderLayout());
        JLabel messagesLabel = new JLabel("Área para Mensagens");
        messagesLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        messagesPanel.add(messagesLabel, BorderLayout.NORTH);
        messagesPanel.add(new JScrollPane(messagesArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, messagesPanel);
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);

        editorArea.getDocument().addDocumentListener(new SimpleDocumentListener(() -> isFileEdited = true));

        setupFileChooser();
        setupMenus();
        setupToolbar();

        addWindowListener(new ExitWindowAdapter());
        updateWindowTitle(null);
    }

    private void setupFileChooser() {
        fileChooser = new JFileChooser(System.getProperty("user.dir"));
        fileChooser.setFileFilter(new TextFileFilter());
        fileChooser.setAcceptAllFileFilterUsed(true);
    }

    private void setupMenus() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Arquivo");
        fileMenu.setMnemonic(KeyEvent.VK_A);

        JMenuItem newItem = createMenuItem("Novo", KeyEvent.VK_N, KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        JMenuItem openItem = createMenuItem("Abrir", KeyEvent.VK_O, KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        JMenuItem saveItem = createMenuItem("Salvar", KeyEvent.VK_S, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        JMenuItem saveAsItem = createMenuItem("Salvar como...", KeyEvent.VK_A, KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        JMenuItem exitItem = createMenuItem("Sair", KeyEvent.VK_R, KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edição");
        editMenu.setMnemonic(KeyEvent.VK_E);
        JMenuItem copyItem = createMenuItem("Copiar", KeyEvent.VK_C, KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        JMenuItem cutItem = createMenuItem("Recortar", KeyEvent.VK_T, KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        JMenuItem pasteItem = createMenuItem("Colar", KeyEvent.VK_L, KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        JMenuItem clearConsoleItem = createMenuItem("Limpar Console", KeyEvent.VK_M, KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));

        editMenu.add(copyItem);
        editMenu.add(cutItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(clearConsoleItem);

        JMenu compileMenu = new JMenu("Compilação");
        compileMenu.setMnemonic(KeyEvent.VK_C);
        JMenuItem compileItem = createMenuItem("Compilar", KeyEvent.VK_P, KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));

        // ADICIONADO: Novo item de menu "Executar"
        JMenuItem executeItem = createMenuItem("Executar", KeyEvent.VK_E, KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));

        compileMenu.add(compileItem);
        compileMenu.add(executeItem); // Adiciona o novo item ao menu

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(compileMenu);
        setJMenuBar(menuBar);

        // Ações dos menus
        newItem.addActionListener(e -> handleNewAction());
        openItem.addActionListener(e -> handleOpenAction());
        saveItem.addActionListener(e -> handleSaveAction());
        saveAsItem.addActionListener(e -> handleSaveAsAction());
        exitItem.addActionListener(e -> handleExitAction());
        copyItem.addActionListener(e -> editorArea.copy());
        cutItem.addActionListener(e -> editorArea.cut());
        pasteItem.addActionListener(e -> editorArea.paste());
        clearConsoleItem.addActionListener(e -> clearConsole());
        compileItem.addActionListener(e -> handleCompileAction());

        // ADICIONADO: Ação para o novo item de menu
        executeItem.addActionListener(e -> {
            messagesArea.setText("Funcionalidade 'Executar' ainda não implementada.");
        });
    }

    private JMenuItem createMenuItem(String text, int mnemonic, KeyStroke accelerator) {
        JMenuItem menuItem = new JMenuItem(text, mnemonic);
        menuItem.setAccelerator(accelerator);
        return menuItem;
    }

    private void setupToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)));

        JButton newButton = createToolbarButton("✚", "Novo Arquivo", new Color(0, 150, 0));
        JButton openButton = createToolbarButton("📂", "Abrir Arquivo", new Color(0, 100, 200));
        JButton saveButton = createToolbarButton("💾", "Salvar Arquivo", new Color(0, 100, 200));
        JButton compileButton = createToolbarButton("▶", "Compilar (F9)", new Color(0, 120, 0));
        JButton clearButton = createToolbarButton("❌", "Limpar Console", new Color(200, 0, 0));

        newButton.addActionListener(e -> handleNewAction());
        openButton.addActionListener(e -> handleOpenAction());
        saveButton.addActionListener(e -> handleSaveAction());
        compileButton.addActionListener(e -> handleCompileAction());
        clearButton.addActionListener(e -> clearConsole());

        toolBar.add(newButton);
        toolBar.add(openButton);
        toolBar.add(saveButton);
        toolBar.add(Box.createHorizontalGlue());
        toolBar.add(compileButton);
        toolBar.add(clearButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private JButton createToolbarButton(String text, String tooltip, Color color) {
        JButton button = new JButton(text);
        button.setToolTipText(tooltip);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        button.setForeground(color);
        button.setFocusPainted(false);
        button.setOpaque(false);
        button.setContentAreaFilled(false);
        button.setBorderPainted(false);
        return button;
    }

    // --- LÓGICA DE COMPILAÇÃO ---
    private void handleCompileAction() {
        String sourceCode = editorArea.getText();
        if (sourceCode.trim().isEmpty()) {
            messagesArea.setText("O código-fonte está vazio.");
            return;
        }

        messagesArea.setText("Compilando...\n");

        try {
            AnalisadorLexico parser = new AnalisadorLexico(new StringReader(sourceCode));
            StringBuilder output = new StringBuilder();

            output.append("Análise léxica concluída com sucesso:\n\n");
            output.append(String.format("%-20s | %-7s | %-7s | %-45s | %s\n",
                    "Lexema", "Linha", "Coluna", "Categoria", "Código"));
            output.append(new String(new char[100]).replace('\0', '-')).append("\n");

            // 1. Coletar todos os tokens primeiro
            java.util.List<Token> allTokens = new java.util.ArrayList<>();
            Token t;
            while ((t = parser.getNextToken()).kind != AnalisadorLexico.EOF) {
                allTokens.add(t);
            }

            // 2. Iterar sobre os tokens e formatar a saída
            for (int i = 0; i < allTokens.size(); i++) {
                Token currentToken = allTokens.get(i);

                // VERIFICAÇÃO: Se encontrarmos um token "/" seguido por um "*", é um erro.
                if (currentToken.image.equals("/") && (i + 1) < allTokens.size() && allTokens.get(i + 1).image.equals("*")) {

                    // Monta a linha de erro formatada
                    String lexemaDeErro = "/*";
                    int linha = currentToken.beginLine;
                    int coluna = currentToken.beginColumn;
                    String categoriaDeErro = "ERRO LÉXICO: comentário de bloco não finalizado.";
                    String codigoDeErro = "-";

                    output.append(String.format("%-20s | %-7d | %-7d | %-45s | %s\n",
                            lexemaDeErro, linha, coluna, categoriaDeErro, codigoDeErro));

                    // Para o loop para ignorar tudo que vem depois
                    break;
                }

                // Se não for um erro, processa e exibe o token normalmente
                String category = getCategoryName(currentToken);
                String code = category.startsWith("ERRO") ? "-" : String.valueOf(currentToken.kind);
                String lexemaParaExibir = currentToken.image.trim().replace("\n", "\\n").replace("\r", "\\r");

                output.append(String.format("%-20s | %-7d | %-7d | %-45s | %s\n",
                        lexemaParaExibir, currentToken.beginLine, currentToken.beginColumn, category, code));
            }

            messagesArea.setText(output.toString());

        } catch (TokenMgrError e) {
            messagesArea.setText("ERRO LÉXICO GRAVE:\n" + e.getMessage());
        }
    }
    private String getCategoryName(Token t) {
        switch (t.kind) {
            case AnalisadorLexico.BEGIN: case AnalisadorLexico.DEFINE: case AnalisadorLexico.START:
            case AnalisadorLexico.END: case AnalisadorLexico.SET: case AnalisadorLexico.READ:
            case AnalisadorLexico.SHOW: case AnalisadorLexico.IF: case AnalisadorLexico.THEN:
            case AnalisadorLexico.ELSE: case AnalisadorLexico.LOOP: case AnalisadorLexico.WHILE:
            case AnalisadorLexico.NUM: case AnalisadorLexico.REAL: case AnalisadorLexico.TEXT:
            case AnalisadorLexico.FLAG: case AnalisadorLexico.TRUE: case AnalisadorLexico.FALSE:
                return "PALAVRA RESERVADA";
            case AnalisadorLexico.IDENTIFIER:
                return "IDENTIFICADOR";
            case AnalisadorLexico.CONST_REAL:
                return "CONSTANTE NUMÉRICA REAL";
            case AnalisadorLexico.CONST_INT:
                return "CONSTANTE NUMÉRICA INTEIRA";
            case AnalisadorLexico.CONST_LITERAL:
                return "CONSTANTE LITERAL";
            case AnalisadorLexico.OP_REL_LTLT_EQ: case AnalisadorLexico.OP_REL_GTGT_EQ:
            case AnalisadorLexico.OP_REL_EQ: case AnalisadorLexico.OP_REL_NEQ:
            case AnalisadorLexico.OP_REL_LTLT: case AnalisadorLexico.OP_REL_GTGT:
                return "SÍMBOLO ESPECIAL - OPERADOR RELACIONAL";
            case AnalisadorLexico.OP_ARIT_POW: case AnalisadorLexico.OP_ARIT_DIVINT:
            case AnalisadorLexico.OP_ARIT_SUM: case AnalisadorLexico.OP_ARIT_SUB:
            case AnalisadorLexico.OP_ARIT_MUL: case AnalisadorLexico.OP_ARIT_DIV:
            case AnalisadorLexico.OP_ARIT_MOD:
                return "SÍMBOLO ESPECIAL - OPERADOR ARITMÉTICO";
            case AnalisadorLexico.OP_LOGIC_AND: case AnalisadorLexico.OP_LOGIC_OR:
            case AnalisadorLexico.OP_LOGIC_NOT:
                return "SÍMBOLO ESPECIAL - OPERADOR LÓGICO";
            case AnalisadorLexico.ASSIGN: case AnalisadorLexico.SEMICOLON:
            case AnalisadorLexico.COMMA: case AnalisadorLexico.LPAREN:
            case AnalisadorLexico.RPAREN: case AnalisadorLexico.LBRACKET:
            case AnalisadorLexico.RBRACKET: case AnalisadorLexico.LBRACE:
            case AnalisadorLexico.RBRACE:
                return "SÍMBOLO ESPECIAL";
            case AnalisadorLexico.INVALID_IDENTIFIER:
                return "ERRO LÉXICO: identificador inválido";
            case AnalisadorLexico.ERRO_LITERAL:
                return "ERRO LÉXICO: constante literal não finalizada";
            case AnalisadorLexico.ERRO_LEXICO:
                return "ERRO LÉXICO: símbolo inválido";
            default:
                String tokenImage = AnalisadorLexico.tokenImage[t.kind].replace("\"", "");
                return "NÃO CATEGORIZADO (" + tokenImage + ")";
        }
    }

    private void handleNewAction() {
        if (confirmAndSaveIfNeeded()) clearInterface();
    }

    private void handleOpenAction() {
        if (confirmAndSaveIfNeeded()) openFile();
    }

    private boolean confirmAndSaveIfNeeded() {
        if (!isFileEdited) return true;
        int result = showSaveConfirmationDialog();
        if (result == JOptionPane.YES_OPTION) {
            handleSaveAction();
            return !isFileEdited;
        }
        return result == JOptionPane.NO_OPTION;
    }

    private void handleSaveAction() {
        if (isNewFile || currentFilePath == null) {
            handleSaveAsAction();
        } else {
            saveFile(currentFilePath);
        }
    }

    private void handleSaveAsAction() {
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file.exists()) {
                int result = JOptionPane.showConfirmDialog(this, "O arquivo já existe. Deseja sobrescrevê-lo?", "Confirmar Sobrescrita", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.NO_OPTION) return;
            }
            saveFile(file.getAbsolutePath());
        }
    }

    private void handleExitAction() {
        if (confirmAndSaveIfNeeded()) System.exit(0);
    }

    private int showSaveConfirmationDialog() {
        return JOptionPane.showConfirmDialog(this,
                "O arquivo foi modificado. Deseja salvá-lo?", "Salvar Arquivo",
                JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
    }

    private void clearInterface() {
        editorArea.setText("");
        messagesArea.setText("");
        currentFilePath = null;
        isNewFile = true;
        isFileEdited = false;
        updateWindowTitle(null);
    }

    private void clearConsole() {
        messagesArea.setText("");
    }

    private void openFile() {
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                String content = new String(Files.readAllBytes(Paths.get(file.getAbsolutePath())));
                editorArea.setText(content);
                editorArea.setCaretPosition(0);
                currentFilePath = file.getAbsolutePath();
                isNewFile = false;
                isFileEdited = false;
                updateWindowTitle(currentFilePath);
                messagesArea.setText("Arquivo aberto: " + file.getName() + "\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir o arquivo: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void saveFile(String filePath) {
        try {
            Files.write(Paths.get(filePath), editorArea.getText().getBytes());
            isFileEdited = false;
            isNewFile = false;
            currentFilePath = filePath;
            updateWindowTitle(filePath);
            messagesArea.setText("Arquivo salvo com sucesso em: " + filePath + "\n");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Erro ao salvar o arquivo: " + e.getMessage(), "Erro de Salvamento", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateWindowTitle(String fileName) {
        String title = "Compilador";
        if (fileName != null) {
            title += " - " + new File(fileName).getName();
        }
        setTitle(title);
    }

    private static class SimpleDocumentListener implements DocumentListener {
        private final Runnable action;
        public SimpleDocumentListener(Runnable action) { this.action = action; }
        @Override public void insertUpdate(DocumentEvent e) { action.run(); }
        @Override public void removeUpdate(DocumentEvent e) { action.run(); }
        @Override public void changedUpdate(DocumentEvent e) { action.run(); }
    }

    private class ExitWindowAdapter extends WindowAdapter {
        @Override public void windowClosing(WindowEvent e) { handleExitAction(); }
    }

    private static class TextFileFilter extends FileFilter {
        @Override
        public boolean accept(File f) {
            if (f.isDirectory()) return true;
            String name = f.getName().toLowerCase();
            return name.endsWith(".txt") || name.endsWith(".djt") || name.endsWith(".cmp") || name.endsWith(".java");
        }
        @Override public String getDescription() { return "Arquivos de Texto (*.txt, *.djt, *.cmp, *.java)"; }
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(() -> new CompilerInterface().setVisible(true));
    }

    class TextLineNumber extends JPanel {
        private final JTextArea editor;
        public TextLineNumber(JTextArea editor) {
            this.editor = editor;
            setFont(editor.getFont());
            setBackground(new Color(245, 245, 245));
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));
            editor.getDocument().addDocumentListener(new SimpleDocumentListener(this::repaint));
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            FontMetrics fm = g.getFontMetrics();
            Rectangle clip = g.getClipBounds();
            try {
                int startOffset = editor.viewToModel2D(new Point(0, clip.y));
                int endOffset = editor.viewToModel2D(new Point(0, clip.y + clip.height));
                int startLine = editor.getLineOfOffset(startOffset);
                int endLine = editor.getLineOfOffset(endOffset);

                for (int i = startLine; i <= endLine; i++) {
                    Rectangle2D rect = editor.modelToView2D(editor.getLineStartOffset(i));
                    if (rect != null) {
                        String lineNumber = String.valueOf(i + 1);
                        int x = getWidth() - fm.stringWidth(lineNumber) - 5;
                        int y = (int) rect.getY() + fm.getAscent();
                        g.setColor(new Color(150, 150, 150));
                        g.drawString(lineNumber, x, y);
                    }
                }
            } catch (BadLocationException e) {
                // Ignore
            }
        }
    }
}