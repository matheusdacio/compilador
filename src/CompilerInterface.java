
import javax.swing.*;
import javax.swing.event.CaretEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
    private JLabel statusLabel;
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

        editorArea = new JTextArea();
        editorArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        editorArea.setBackground(new Color(255, 255, 255, 211));
        editorArea.setForeground(new Color(51, 51, 51));
        editorArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        editorArea.setCaretColor(new Color(0, 120, 215)); // Azul para cursor

        messagesArea = new JTextArea();
        messagesArea.setEditable(false);
        messagesArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        messagesArea.setBackground(new Color(250, 247, 247, 211));
        messagesArea.setForeground(new Color(64, 64, 64));
        messagesArea.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane editorScrollPane = new JScrollPane(editorArea);
        TextLineNumber tln = new TextLineNumber(editorArea);
        editorScrollPane.setRowHeaderView(tln);

        JPanel editorPanel = new JPanel(new BorderLayout());
        JLabel editorLabel = new JLabel("área para edição");
        editorLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        editorLabel.setForeground(Color.BLACK);
        editorLabel.setBackground(Color.WHITE);
        editorLabel.setOpaque(true);
        editorLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        editorPanel.add(editorLabel, BorderLayout.NORTH);
        editorPanel.add(editorScrollPane, BorderLayout.CENTER);

        JPanel messagesPanel = new JPanel(new BorderLayout());
        JLabel messagesLabel = new JLabel("área para mensagens");
        messagesLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        messagesLabel.setForeground(Color.BLACK);
        messagesLabel.setBackground(Color.WHITE);
        messagesLabel.setOpaque(true);
        messagesLabel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(Color.BLACK, 1),
            BorderFactory.createEmptyBorder(2, 4, 2, 4)
        ));
        messagesPanel.add(messagesLabel, BorderLayout.NORTH);
        messagesPanel.add(new JScrollPane(messagesArea), BorderLayout.CENTER);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editorPanel, messagesPanel);
        splitPane.setResizeWeight(0.7);
        add(splitPane, BorderLayout.CENTER);

        statusLabel = new JLabel("Linha: 1, Coluna: 1");
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
        statusLabel.setForeground(new Color(80, 80, 80)); // Cinza escuro
        JPanel statusBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusBar.setBackground(new Color(248, 248, 248, 255)); // Cinza claro
        statusBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(3, 10, 3, 0)
        ));
        statusBar.add(statusLabel);
        add(statusBar, BorderLayout.SOUTH);

        editorArea.addCaretListener(this::updateCaretStatus);
        editorArea.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateFileStatus));

        setupFileChooser();

        setupMenus();
        setupToolbar();

        addWindowListener(new ExitWindowAdapter());

        updateWindowTitle(null);
    }

    private void setupFileChooser() {
        fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        
        fileChooser.addChoosableFileFilter(new TextFileFilter());
        fileChooser.setAcceptAllFileFilterUsed(true);
    }

    private void setupMenus() {
        JMenuBar menuBar = new JMenuBar();

        JMenu fileMenu = new JMenu("Arquivo");
        fileMenu.setMnemonic(KeyEvent.VK_A);
        
        JMenuItem newItem = new JMenuItem("Novo", KeyEvent.VK_N);
        newItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        
        JMenuItem openItem = new JMenuItem("Abrir", KeyEvent.VK_A);
        openItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        
        JMenuItem saveItem = new JMenuItem("Salvar", KeyEvent.VK_S);
        saveItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK));
        
        JMenuItem saveAsItem = new JMenuItem("Salvar como", KeyEvent.VK_V);
        saveAsItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));
        
        JMenuItem exitItem = new JMenuItem("Sair", KeyEvent.VK_S);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F4, ActionEvent.ALT_MASK));

        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edição");
        editMenu.setMnemonic(KeyEvent.VK_E);
        
        JMenuItem copyItem = new JMenuItem("Copiar", KeyEvent.VK_C);
        copyItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        
        JMenuItem cutItem = new JMenuItem("Recortar", KeyEvent.VK_R);
        cutItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.CTRL_MASK));
        
        JMenuItem pasteItem = new JMenuItem("Colar", KeyEvent.VK_L);
        pasteItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK));
        
        JMenuItem clearConsoleItem = new JMenuItem("Limpar Console", KeyEvent.VK_L);
        clearConsoleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.CTRL_MASK | ActionEvent.SHIFT_MASK));

        editMenu.add(copyItem);
        editMenu.add(cutItem);
        editMenu.add(pasteItem);
        editMenu.addSeparator();
        editMenu.add(clearConsoleItem);

        JMenu compileMenu = new JMenu("Compilação");
        compileMenu.setMnemonic(KeyEvent.VK_C);
        
        JMenuItem compileItem = new JMenuItem("Compilar", KeyEvent.VK_C);
        compileItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0));
        
        JMenuItem executeItem = new JMenuItem("Executar", KeyEvent.VK_E);
        executeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F10, 0));

        compileMenu.add(compileItem);
        compileMenu.add(executeItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(compileMenu);

        setJMenuBar(menuBar);

        newItem.addActionListener(e -> handleNewAction());
        openItem.addActionListener(e -> handleOpenAction());
        saveItem.addActionListener(e -> handleSaveAction());
        saveAsItem.addActionListener(e -> handleSaveAsAction());
        exitItem.addActionListener(e -> handleExitAction());

        compileItem.addActionListener(e -> handleCompileAction());
        executeItem.addActionListener(e -> handleExecuteAction());

        copyItem.addActionListener(e -> editorArea.copy());
        cutItem.addActionListener(e -> editorArea.cut());
        pasteItem.addActionListener(e -> editorArea.paste());
        clearConsoleItem.addActionListener(e -> clearConsole());
    }

    private void setupToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        toolBar.setBackground(new Color(230, 230, 230, 82));
        toolBar.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200, 142)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));
        

        JButton newButton = createToolbarButton("+", "Novo", new Color(0, 150, 0));
        JButton clearButton = createToolbarButton("×", "Limpar Console", new Color(60, 60, 60));
        JButton copyButton = createToolbarButton("⧉", "Copiar", new Color(0, 100, 200));
        JButton runButton = createToolbarButton("▶", "Executar", new Color(0, 120, 0));
        JButton stopButton = createToolbarButton("■", "Parar", new Color(200, 0, 0));

        newButton.addActionListener(e -> handleNewAction());
        clearButton.addActionListener(e -> clearConsole());
        copyButton.addActionListener(e -> editorArea.copy());
        runButton.addActionListener(e -> handleCompileAction());
        stopButton.addActionListener(e -> messagesArea.setText("Operação interrompida.\n"));

        toolBar.add(newButton);
        toolBar.add(clearButton);
        toolBar.add(copyButton);
        toolBar.add(runButton);
        toolBar.add(stopButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private JButton createToolbarButton(String icon, String tooltip, Color color) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        button.setForeground(color);
        button.setPreferredSize(new Dimension(35, 30));
        button.setBorder(BorderFactory.createRaisedBevelBorder());
        button.setFocusPainted(false);
        button.setContentAreaFilled(true);
        button.setBackground(new Color(255, 255, 255, 84));
        button.setMargin(new Insets(3, 3, 3, 3));
        
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(240, 248, 255));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(255, 255, 255));
            }
        });
        
        return button;
    }

    private void handleNewAction() {
        if (isFileEdited) {
            int result = showSaveConfirmationDialog();
            if (result == JOptionPane.YES_OPTION) {
                if (isNewFile) {
                    handleSaveAsAction();
                } else {
                    handleSaveAction();
                }
                if (!isFileEdited) {
                    clearInterface();
                }
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
                if (isNewFile) {
                    handleSaveAsAction();
                } else {
                    handleSaveAction();
                }
                if (!isFileEdited) {
                    openFile();
                }
            } else if (result == JOptionPane.NO_OPTION) {
                openFile();
            }
        } else {
            openFile();
        }
    }

    private void handleSaveAction() {
        if (isNewFile) {
            handleSaveAsAction();
        } else {
            try {
                saveFile(currentFilePath);
                isFileEdited = false;
                messagesArea.setText("Arquivo salvo com sucesso.\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + e.getMessage(), 
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleSaveAsAction() {
        fileChooser.setDialogTitle("Salvar como");
        fileChooser.setSelectedFile(new File("novo_arquivo.djt"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            

            if (selectedFile.exists()) {
                int overwrite = JOptionPane.showConfirmDialog(this,
                    "O arquivo já existe. Deseja sobrescrevê-lo?",
                    "Arquivo existente",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
                
                if (overwrite != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            
            try {
                saveFile(selectedFile.getAbsolutePath());
                currentFilePath = selectedFile.getAbsolutePath();
                isNewFile = false;
                isFileEdited = false;
                updateWindowTitle(currentFilePath);
                messagesArea.setText("Arquivo salvo como: " + selectedFile.getName() + "\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao salvar arquivo: " + e.getMessage(), 
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleExitAction() {
        if (isFileEdited) {
            int result = showSaveConfirmationDialog();
            if (result == JOptionPane.YES_OPTION) {
                if (isNewFile) {
                    handleSaveAsAction();
                } else {
                    handleSaveAction();
                }
                if (!isFileEdited) {
                    System.exit(0);
                }
            } else if (result == JOptionPane.NO_OPTION) {
                System.exit(0);
            }
        } else {
            System.exit(0);
        }
    }

    private void handleCompileAction() {
        String content = editorArea.getText().trim();
        if (content.isEmpty()) {
            messagesArea.setText("Erro: Não é possível compilar um arquivo vazio.\n");
            return;
        }
        
        messagesArea.setText("Compilando...\n");
        
        try {
            AnalisadorLexico lexer = new AnalisadorLexico(new StringReader(content));
            Token token;
            StringBuilder output = new StringBuilder();
            output.append("=== ANÁLISE LÉXICA ===\n\n");
            
            output.append(String.format("%-15s %-6s %-8s %-35s %-6s\n",
                "Lexema", "Linha", "Coluna", "Categoria", "Código"));
            output.append(String.format("%-15s %-6s %-8s %-35s %-6s\n", 
                "-------", "-----", "-------", "-----------------------------------", "------"));
            
            while ((token = lexer.getNextToken()) != null) {
                if (token.kind == AnalisadorLexicoConstants.EOF) {
                    break;
                }
                
                String lexema = token.image;
                int linha = token.beginLine;
                int coluna = token.beginColumn;
                String categoria = getTokenCategory(token.kind);
                String codigo = getTokenCode(token.kind);
                
                output.append(String.format("%-15s %-6d %-8d %-35s %-6s\n", 
                    lexema, linha, coluna, categoria, codigo));
            }
            
            output.append("\nCompilação concluída com sucesso!\n");
            messagesArea.setText(output.toString());
            
        } catch (Exception e) {
            messagesArea.setText("Erro durante a compilação: " + e.getMessage() + "\n");
        }
    }

    private void handleExecuteAction() {
        messagesArea.setText("Executar - Funcionalidade será implementada posteriormente.\n");
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
        isNewFile = true;
        currentFilePath = null;
    }

    private void clearConsole() {
        messagesArea.setText("");
    }

    private String getTokenCategory(int tokenKind) {
        switch (tokenKind) {
            case AnalisadorLexicoConstants.PALAVRA_RESERVADA:
                return "PALAVRA RESERVADA";
            case AnalisadorLexicoConstants.IDENTIFIER:
                return "IDENTIFICADOR";
            case AnalisadorLexicoConstants.CONST_INT:
                return "CONSTANTE NUMÉRICA INTEIRA";
            case AnalisadorLexicoConstants.CONST_REAL:
                return "CONSTANTE NUMÉRICA REAL";
            case AnalisadorLexicoConstants.CONST_LITERAL:
                return "CONSTANTE LITERAL";
            case AnalisadorLexicoConstants.OP_ARITMETICO:
            case AnalisadorLexicoConstants.OP_RELACIONAL:
            case AnalisadorLexicoConstants.OP_LOGICO:
            case AnalisadorLexicoConstants.SIMBOLO_ESPECIAL:
                return "SÍMBOLO ESPECIAL";
            case AnalisadorLexicoConstants.ERRO_LITERAL:
            case AnalisadorLexicoConstants.ERRO_LEXICO:
                return "ERRO LÉXICO: símbolo invalido";
            default:
                return "DESCONHECIDO";
        }
    }

    private String getTokenCode(int tokenKind) {
        switch (tokenKind) {
            case AnalisadorLexicoConstants.PALAVRA_RESERVADA:
                return "25";
            case AnalisadorLexicoConstants.IDENTIFIER:
                return "3";
            case AnalisadorLexicoConstants.CONST_INT:
                return "2";
            case AnalisadorLexicoConstants.CONST_REAL:
                return "7";
            case AnalisadorLexicoConstants.CONST_LITERAL:
                return "8";
            case AnalisadorLexicoConstants.OP_ARITMETICO:
                return "40";
            case AnalisadorLexicoConstants.OP_RELACIONAL:
                return "41";
            case AnalisadorLexicoConstants.OP_LOGICO:
                return "42";
            case AnalisadorLexicoConstants.SIMBOLO_ESPECIAL:
                return "45";
            case AnalisadorLexicoConstants.ERRO_LITERAL:
            case AnalisadorLexicoConstants.ERRO_LEXICO:
                return "-";
            default:
                return "-";
        }
    }

    private void openFile() {
        fileChooser.setDialogTitle("Abrir arquivo");
        fileChooser.setSelectedFile(null);
        
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            try {
                loadFile(selectedFile.getAbsolutePath());
                currentFilePath = selectedFile.getAbsolutePath();
                isNewFile = false;
                isFileEdited = false;
                updateWindowTitle(currentFilePath);
                messagesArea.setText("Arquivo aberto: " + selectedFile.getName() + "\n");
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir arquivo: " + e.getMessage(), 
                    "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadFile(String filePath) throws IOException {
        String content = new String(Files.readAllBytes(Paths.get(filePath)));
        editorArea.setText(content);
        editorArea.setCaretPosition(0);
    }

    private void saveFile(String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write(editorArea.getText());
        }
    }

    private void updateWindowTitle(String fileName) {
        currentFilePath = fileName;
        if (fileName != null) {
            setTitle("Compilador - " + new File(fileName).getName());
        } else {
            setTitle("Compilador");
        }
    }

    // Métodos para listeners (reduz classes anônimas)
    private void updateCaretStatus(CaretEvent e) {
        try {
            int caretPosition = editorArea.getCaretPosition();
            int lineNumber = editorArea.getLineOfOffset(caretPosition);
            int columnNumber = caretPosition - editorArea.getLineStartOffset(lineNumber);
            statusLabel.setText("Linha: " + (lineNumber + 1) + ", Coluna: " + (columnNumber + 1));
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        }
    }

    private void updateFileStatus() {
        isFileEdited = true;
    }

    private class SimpleDocumentListener implements DocumentListener {
        private final Runnable action;
        
        public SimpleDocumentListener(Runnable action) {
            this.action = action;
        }
        
        @Override
        public void insertUpdate(DocumentEvent e) { action.run(); }
        @Override
        public void removeUpdate(DocumentEvent e) { action.run(); }
        @Override
        public void changedUpdate(DocumentEvent e) { action.run(); }
    }

    private class ExitWindowAdapter extends WindowAdapter {
        @Override
        public void windowClosing(WindowEvent e) {
            handleExitAction();
        }
    }

    private class TextFileFilter extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File f) {
            return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt") ||
                   f.getName().toLowerCase().endsWith(".djt") ||
                   f.getName().toLowerCase().endsWith(".cmp") ||
                   f.getName().toLowerCase().endsWith(".java");
        }

        @Override
        public String getDescription() {
            return "Arquivos de texto (*.txt, *.djt, *.cmp, *.java)";
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
            setBackground(new Color(245, 245, 245));
            setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));
            editor.getDocument().addDocumentListener(new SimpleDocumentListener(this::repaint));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            FontMetrics fm = g.getFontMetrics();

            Rectangle visibleRect = editor.getVisibleRect();
            int startOffset = editor.viewToModel2D(new Point(0, visibleRect.y));
            int endOffset = editor.viewToModel2D(new Point(0, visibleRect.y + visibleRect.height));

            try {
                int startLine = editor.getLineOfOffset(startOffset);
                int endLine = editor.getLineOfOffset(endOffset);

                for (int i = startLine; i <= endLine; i++) {
                    Rectangle2D rect = editor.modelToView2D(editor.getLineStartOffset(i));
                    int y = (int)rect.getY() + fm.getAscent();
                    String lineNumber = String.valueOf(i + 1);
                    int x = getWidth() - fm.stringWidth(lineNumber) - 5;
                    
                    g.setColor(new Color(0, 100, 200));
                    g.drawString(lineNumber, x, y);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
    }
}