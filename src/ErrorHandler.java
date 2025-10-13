// ErrorHandler.java

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ErrorHandler {
    private List<String> errorMessages = new ArrayList<>();

    // Mapeia os KINDs dos tokens de erro léxico para mensagens amigáveis.
    // Esta classe precisa da interface 'AnalisadorLexicoConstants.java',
    // que é gerada automaticamente pelo JavaCC.
    private static final java.util.Map<Integer, String> LEXICAL_ERROR_MESSAGES = new java.util.HashMap<>();
    static {
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_ID_INICIA_COM_DIGITO, "Identificador inválido: não pode começar com um dígito.");
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_ID_DIGITOS_CONSECUTIVOS, "Identificador inválido: não pode conter dois ou mais dígitos consecutivos.");
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_ID_TERMINA_COM_DIGITO, "Identificador inválido: não pode terminar com um dígito.");
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_INT_LONGO, "Constante numérica inteira muito longa.");
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_REAL_FRACAO_LONGA, "Constante real com parte fracionária muito longa.");
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_REAL_INTEIRO_LONGO, "Constante real com parte inteira muito longa.");
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_REAL_INCOMPLETO, "Constante real malformada (faltam dígitos após o ponto).");
        LEXICAL_ERROR_MESSAGES.put(AnalisadorLexicoConstants.ERRO_LEXICO, "Símbolo não reconhecido pela linguagem.");
    }

    /**
     * Processa uma ParseException para gerar uma mensagem de erro de alta qualidade.
     * @param e A exceção lançada pelo parser.
     * @param context Uma string que descreve onde o erro ocorreu (ex: "na declaração de variáveis").
     */
    public void processParseException(ParseException e, String context) {
        Token errorToken = e.currentToken.next;
        int line = errorToken.beginLine;
        int column = errorToken.beginColumn;
        String finalMessage;

        // 1. Verifica se é um erro léxico que foi tokenizado
        if (LEXICAL_ERROR_MESSAGES.containsKey(errorToken.kind)) {
            String specificError = LEXICAL_ERROR_MESSAGES.get(errorToken.kind);
            finalMessage = String.format("Erro Léxico na linha %d, coluna %d (%s): %s", line, column, context, specificError);
        } else {
            // 2. Se não, é um erro sintático
            String encontrado = getErrorTokenDescription(errorToken);
            String esperado = getExpectedTokensDescription(e);

            finalMessage = String.format("Erro Sintático na linha %d, coluna %d (%s): Encontrado %s, mas era esperado %s.",
                    line, column, context, encontrado, esperado);
        }

        addError(finalMessage);
    }

    private String getErrorTokenDescription(Token t) {
        if (t.kind == AnalisadorLexicoConstants.EOF) {
            return "o final do arquivo";
        }
        // Escapa caracteres especiais para exibição clara
        return "'" + t.image.replace("\n", "\\n").replace("\r", "\\r") + "'";
    }

    private String getExpectedTokensDescription(ParseException e) {
        if (e.expectedTokenSequences == null || e.expectedTokenSequences.length == 0) {
            return "uma expressão válida";
        }

        // Usamos um TreeSet para ordenar e evitar duplicatas
        Set<String> expected = new TreeSet<>();
        for (int[] sequence : e.expectedTokenSequences) {
            if (sequence.length == 1) {
                String tokenImage = e.tokenImage[sequence[0]];
                // Limpa a imagem do token para exibição
                expected.add(tokenImage.replace("\"", "'"));
            }
        }

        if (expected.isEmpty()) {
            return "uma construção válida";
        }

        List<String> expectedList = new ArrayList<>(expected);
        if (expectedList.size() == 1) {
            return expectedList.get(0);
        }

        // Constrói uma lista legível: "'A', 'B' ou 'C'"
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < expectedList.size(); i++) {
            sb.append(expectedList.get(i));
            if (i < expectedList.size() - 2) {
                sb.append(", ");
            } else if (i == expectedList.size() - 2) {
                sb.append(" ou ");
            }
        }
        return sb.toString();
    }

    public void addError(String message) {
        errorMessages.add(message);
    }

    public boolean hasErrors() {
        return !errorMessages.isEmpty();
    }



    public List<String> getErrorMessages() {
        return errorMessages;
    }
}