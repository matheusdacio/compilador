// ErrorHandler.java

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class ErrorHandler {
    private List<String> errorMessages = new ArrayList<>();

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
        Token errorToken = (e.currentToken.next != null) ? e.currentToken.next : e.currentToken;
        int line = errorToken.beginLine;
        int column = errorToken.beginColumn;
        String finalMessage;

        if (LEXICAL_ERROR_MESSAGES.containsKey(errorToken.kind)) {
            String specificError = LEXICAL_ERROR_MESSAGES.get(errorToken.kind);
            finalMessage = String.format("Erro Léxico na linha %d, coluna %d (%s): %s", line, column, context, specificError);
        } else {
            String encontrado = getErrorTokenDescription(errorToken);
            String esperado = getExpectedTokensDescription(e);

            finalMessage = String.format("Erro Sintático na linha %d, coluna %d (%s): Encontrado %s, mas era esperado %s.",
                    line, column, context, encontrado, esperado);
        }

        addError(finalMessage);
    }


    /**
     * Processa um erro léxico direto detectado pelo analisador.
     * Exemplo: símbolo inválido, identificador incorreto, etc.
     *
     * @param t Token que causou o erro.
     * @param context Contexto textual (ex: "em uma declaração de variável").
     */
    public void processLexicalError(Token t, String context) {
        if (t == null) return;

        int line = t.beginLine;
        int column = t.beginColumn;
        String message;

        if (LEXICAL_ERROR_MESSAGES.containsKey(t.kind)) {
            // Usa mensagem específica
            String specific = LEXICAL_ERROR_MESSAGES.get(t.kind);
            message = String.format("Erro Léxico na linha %d, coluna %d (%s): %s",
                    line, column, context, specific);
        } else {
            // Mensagem genérica
            message = String.format("Erro Léxico na linha %d, coluna %d (%s): Símbolo não reconhecido pela linguagem.",
                    line, column, context);
        }

        addError(message);
    }


    private String getErrorTokenDescription(Token t) {
        if (t.kind == AnalisadorLexicoConstants.EOF) {
            return "o final do arquivo";
        }
        return "'" + t.image.replace("\n", "\\n").replace("\r", "\\r") + "'";
    }

    private String getExpectedTokensDescription(ParseException e) {
        if (e.expectedTokenSequences == null || e.expectedTokenSequences.length == 0) {
            return "uma expressão válida";
        }

        Set<String> expected = new TreeSet<>();
        for (int[] sequence : e.expectedTokenSequences) {
            if (sequence.length == 1) {
                String tokenImage = e.tokenImage[sequence[0]];
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