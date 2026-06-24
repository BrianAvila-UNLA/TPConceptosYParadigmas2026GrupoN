package com.miorganizacion.simple.interpreteConceptosYParadigmas;

import java.io.IOException;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class Main {

    private static final String EXTENSION = "smp";
    private static final String DIRBASE   = "src/test/resources/";

    public static void main(String[] args) throws IOException {
        String[] files = args.length == 0
            ? new String[]{ "test." + EXTENSION }
            : args;

        for (String file : files) {
            System.out.println("=== START: " + file + " ===");
            try {
                CharStream input = CharStreams.fromFileName(DIRBASE + file);

                // Análisis léxico
                SimpleLexer lexer = new SimpleLexer(input);
                lexer.removeErrorListeners();
                lexer.addErrorListener(new BaseErrorListener() {
                    @Override
                    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                            int line, int charPositionInLine,
                                            String msg, RecognitionException e) {
                        throw new RuntimeException(
                            "Error Lexico [linea " + line + ":" + charPositionInLine + "]: " + msg
                        );
                    }
                });

                // Análisis sintáctico 
                CommonTokenStream tokens = new CommonTokenStream(lexer);
                SimpleParser parser = new SimpleParser(tokens);
                parser.removeErrorListeners();
                parser.addErrorListener(new BaseErrorListener() {
                    @Override
                    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                                            int line, int charPositionInLine,
                                            String msg, RecognitionException e) {
                        throw new RuntimeException(
                            "Error Sintactico [linea " + line + ":" + charPositionInLine + "]: " + msg
                        );
                    }
                });

                SimpleParser.ProgramContext tree = parser.program();

                // Interpretación (análisis semántico + ejecución)
                SimpleCustomVisitor visitor = new SimpleCustomVisitor();
                visitor.visit(tree);

            } catch (RuntimeException e) {
                System.err.println(e.getMessage());
            }
            System.out.println("=== FINISH: " + file + " ===");
        }
    }
}