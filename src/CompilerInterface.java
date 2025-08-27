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
        setSize(800, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Áreas de edição e mensagens
        editorArea = new JTextArea();
        messagesArea = new JTextArea();
        messagesArea.setEditable(false);

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
        JMenuItem executeItem = new JMenuItem("Executar");

        compileMenu.add(compileItem);
        compileMenu.add(executeItem);

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

        compileItem.addActionListener(e -> {
            messagesArea.setText("Compilando...\n");
            // Adicione a lógica do compilador aqui
        });

        executeItem.addActionListener(e -> {
            messagesArea.setText("Executando...\n");
            // Adicione a lógica de execução aqui
        });

        copyItem.addActionListener(e -> editorArea.copy());
        cutItem.addActionListener(e -> editorArea.cut());
        pasteItem.addActionListener(e -> editorArea.paste());
    }

    private void setupToolbar() {
        JToolBar toolBar = new JToolBar();
        // Botões para o menu Compilação
        JButton compileButton = new JButton("Compilar");
        JButton executeButton = new JButton("Executar");

        compileButton.addActionListener(e -> {
            messagesArea.setText("Compilando...\n");
        });
        executeButton.addActionListener(e -> {
            messagesArea.setText("Executando...\n");
        });

        toolBar.add(compileButton);
        toolBar.add(executeButton);

        add(toolBar, BorderLayout.NORTH);
    }

    // Métodos para manipulação de arquivos
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