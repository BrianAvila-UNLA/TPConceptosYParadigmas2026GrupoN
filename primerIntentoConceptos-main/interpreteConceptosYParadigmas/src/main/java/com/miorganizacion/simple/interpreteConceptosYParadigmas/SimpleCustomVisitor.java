package com.miorganizacion.simple.interpreteConceptosYParadigmas;

import java.util.HashMap;
import java.util.Map;

/**
 * Visitor que realiza análisis semántico y ejecución del programa.
 * Recorre el árbol sintáctico generado por ANTLR e interpreta cada nodo.
 * La tabla de símbolos guarda nombre → valor (Integer, Double, Boolean, String).
 * Un valor null significa "declarada pero sin asignar".
 */
public class SimpleCustomVisitor extends SimpleBaseVisitor<Object> {

    private final Map<String, Object> symbolTable = new HashMap<>();

    // program
    @Override
    public Object visitProgram(SimpleParser.ProgramContext ctx) {
        for (SimpleParser.SentenceContext s : ctx.sentence()) {
            visit(s);
        }
        return null;
    }

    // sentence
    @Override
    public Object visitSentence(SimpleParser.SentenceContext ctx) {
        return visitChildren(ctx);
    }

    // var_decl : VAR ID SEMICOLON
    // Semántico: error si ya fue declarada.
    @Override
    public Object visitVar_decl(SimpleParser.Var_declContext ctx) {
        String name = ctx.ID().getText();
        if (symbolTable.containsKey(name)) {
            throw new RuntimeException(
                "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                "La variable '" + name + "' ya fue declarada."
            );
        }
        symbolTable.put(name, null);
        return null;
    }

    // var_assign : ID ASSIGN comparison SEMICOLON
    // Semántico: variable debe estar declarada; tipos compatibles.
    @Override
    public Object visitVar_assign(SimpleParser.Var_assignContext ctx) {
        String name = ctx.ID().getText();
        if (!symbolTable.containsKey(name)) {
            throw new RuntimeException(
                "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                "La variable '" + name + "' no fue declarada."
            );
        }
        Object newValue = visit(ctx.comparison());
        Object currentValue = symbolTable.get(name);
        if (currentValue != null && newValue != null &&
            !currentValue.getClass().equals(newValue.getClass())) {
            throw new RuntimeException(
                "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                "Incompatibilidad de tipos en '" + name + "': era " +
                typeName(currentValue) + ", se asigno " + typeName(newValue) + "."
            );
        }
        symbolTable.put(name, newValue);
        return null;
    }

    // println
    @Override
    public Object visitPrintln(SimpleParser.PrintlnContext ctx) {
        Object value = visit(ctx.comparison());
        if (value instanceof String) {
            String s = (String) value;
            if (s.startsWith("\"") && s.endsWith("\"")) {
                s = s.substring(1, s.length() - 1);
            }
            System.out.println(s);
        } else {
            System.out.println(value);
        }
        return null;
    }

    // conditional : IF PAR_OPEN comparison PAR_CLOSE { ... } ELSE { ... }
    // Semántico: condición debe ser booleana.
    @Override
    public Object visitConditional(SimpleParser.ConditionalContext ctx) {
        Object condValue = visit(ctx.comparison());
        if (!(condValue instanceof Boolean)) {
            throw new RuntimeException(
                "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                "La condicion del if-else debe ser booleana, se obtuvo " + typeName(condValue) + "."
            );
        }
        if ((Boolean) condValue) {
            visitBlock(ctx, true);
        } else {
            visitBlock(ctx, false);
        }
        return null;
    }

    private void visitBlock(SimpleParser.ConditionalContext ctx, boolean thenBlock) {
        int firstOpen  = ctx.BRACKET_OPEN(0).getSymbol().getTokenIndex();
        int firstClose = ctx.BRACKET_CLOSE(0).getSymbol().getTokenIndex();
        int secondOpen = ctx.BRACKET_OPEN(1).getSymbol().getTokenIndex();
        for (SimpleParser.SentenceContext s : ctx.sentence()) {
            int idx = s.getStart().getTokenIndex();
            if (thenBlock && idx > firstOpen && idx < firstClose) visit(s);
            if (!thenBlock && idx > secondOpen) visit(s);
        }
    }

    // do_while
    // Semántico: condición debe ser booleana.
    // -----------------------------------------------------------------------
    @Override
    public Object visitDo_while(SimpleParser.Do_whileContext ctx) {
        do {
            for (SimpleParser.SentenceContext s : ctx.sentence()) {
                visit(s);
            }
            Object condValue = visit(ctx.comparison());
            if (!(condValue instanceof Boolean)) {
                throw new RuntimeException(
                    "Error Semantico [linea " + ctx.WHILE().getSymbol().getLine() + "]: " +
                    "La condicion del do-while debe ser booleana, se obtuvo " + typeName(condValue) + "."
                );
            }
            if (!(Boolean) condValue) break;
        } while (true);
        return null;
    }

    // comparison : logicalOr ( (GT|LT|GEQ|LEQ|EQ|NEQ) logicalOr )?
    // Semántico: tipos compatibles para comparar.
    @Override
    public Object visitComparison(SimpleParser.ComparisonContext ctx) {
        Object left = visit(ctx.logicalOr(0));
        if (ctx.logicalOr().size() == 1) return left;

        Object right = visit(ctx.logicalOr(1));
        String op = ctx.getChild(1).getText();

        if (left == null || right == null) {
            throw new RuntimeException(
                "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                "No se puede comparar una variable sin valor asignado."
            );
        }
        if (!left.getClass().equals(right.getClass())) {
            throw new RuntimeException(
                "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                "No se pueden comparar tipos distintos: " + typeName(left) + " y " + typeName(right) + "."
            );
        }
        return applyComparison(left, op, right, ctx.getStart().getLine());
    }

    // logicalOr : logicalAnd ( OR logicalAnd )*
    @Override
    public Object visitLogicalOr(SimpleParser.LogicalOrContext ctx) {
        Object result = visit(ctx.logicalAnd(0));
        for (int i = 1; i < ctx.logicalAnd().size(); i++) {
            Object right = visit(ctx.logicalAnd(i));
            checkBoolean(result, "||", ctx.getStart().getLine());
            checkBoolean(right,  "||", ctx.getStart().getLine());
            result = (Boolean) result || (Boolean) right;
        }
        return result;
    }

    // logicalAnd : logicalNot ( AND logicalNot )*
    @Override
    public Object visitLogicalAnd(SimpleParser.LogicalAndContext ctx) {
        Object result = visit(ctx.logicalNot(0));
        for (int i = 1; i < ctx.logicalNot().size(); i++) {
            Object right = visit(ctx.logicalNot(i));
            checkBoolean(result, "&&", ctx.getStart().getLine());
            checkBoolean(right,  "&&", ctx.getStart().getLine());
            result = (Boolean) result && (Boolean) right;
        }
        return result;
    }

    // logicalNot : NOT logicalNot | expression
    @Override
    public Object visitLogicalNot(SimpleParser.LogicalNotContext ctx) {
        if (ctx.NOT() != null) {
            Object val = visit(ctx.logicalNot());
            checkBoolean(val, "!", ctx.getStart().getLine());
            return !(Boolean) val;
        }
        return visit(ctx.expression());
    }

    // expression : term ( ( PLUS | MINUS ) term )*
    @Override
    public Object visitExpression(SimpleParser.ExpressionContext ctx) {
        Object result = visit(ctx.term(0));
        for (int i = 1; i < ctx.term().size(); i++) {
            // El operador está justo antes del siguiente término
            String op = ctx.getChild(2 * i - 1).getText(); 
            Object right = visit(ctx.term(i));
            result = applyArithmetic(result, op, right, ctx.getStart().getLine());
        }
        return result;
    }

    // term : factor ( ( MULT | DIV ) factor )*
    @Override
    public Object visitTerm(SimpleParser.TermContext ctx) {
        Object result = visit(ctx.factor(0));
        for (int i = 1; i < ctx.factor().size(); i++) {
            String op = ctx.getChild(2 * i - 1).getText(); 
            Object right = visit(ctx.factor(i));
            result = applyArithmetic(result, op, right, ctx.getStart().getLine());
        }
        return result;
    }

    // factor : INTEGER | REAL | MINUS factor | NOT factor | ID | BOOLEAN | STRING | PAR_OPEN comparison PAR_CLOSE
    @Override
    public Object visitFactor(SimpleParser.FactorContext ctx) {
        if (ctx.INTEGER() != null) {
            return Integer.parseInt(ctx.INTEGER().getText());
        }
        if (ctx.REAL() != null) {
            return Double.parseDouble(ctx.REAL().getText());
        }
        if (ctx.BOOLEAN() != null) {
            return Boolean.parseBoolean(ctx.BOOLEAN().getText());
        }
        if (ctx.STRING() != null) {
            return ctx.STRING().getText();
        }
        if (ctx.ID() != null) {
            String name = ctx.ID().getText();
            if (!symbolTable.containsKey(name)) {
                throw new RuntimeException(
                    "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                    "La variable '" + name + "' no fue declarada."
                );
            }
            Object value = symbolTable.get(name);
            if (value == null) {
                throw new RuntimeException(
                    "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                    "La variable '" + name + "' fue declarada pero no tiene valor asignado."
                );
            }
            return value;
        }
        if (ctx.MINUS() != null) {
            Object val = visit(ctx.factor());
            if (val instanceof Integer) return -(Integer) val;
            if (val instanceof Double)  return -(Double) val;
            throw new RuntimeException(
                "Error Semantico [linea " + ctx.getStart().getLine() + "]: " +
                "El operador unario '-' solo aplica a números."
            );
        }
        if (ctx.NOT() != null) {
            Object val = visit(ctx.factor());
            checkBoolean(val, "!", ctx.getStart().getLine());
            return !(Boolean) val;
        }
        // PAR_OPEN comparison PAR_CLOSE
        return visit(ctx.comparison());
    }

    // Helpers

    private Object applyArithmetic(Object left, String op, Object right, int line) {
        if (!(left instanceof Number) || !(right instanceof Number)) {
            throw new RuntimeException(
                "Error Semantico [linea " + line + "]: " +
                "Operacion '" + op + "' requiere operandos numericos, " +
                "se obtuvo " + typeName(left) + " y " + typeName(right) + "."
            );
        }
        boolean useDouble = (left instanceof Double) || (right instanceof Double);
        if (useDouble) {
            double l = ((Number) left).doubleValue();
            double r = ((Number) right).doubleValue();
            switch (op) {
                case "+": return l + r;
                case "-": return l - r;
                case "*": return l * r;
                case "/":
                    if (r == 0.0) throw new RuntimeException(
                        "Error Semantico [linea " + line + "]: Division por cero."
                    );
                    return l / r;
            }
        } else {
            int l = (Integer) left;
            int r = (Integer) right;
            switch (op) {
                case "+": return l + r;
                case "-": return l - r;
                case "*": return l * r;
                case "/":
                    if (r == 0) throw new RuntimeException(
                        "Error Semantico [linea " + line + "]: Division por cero."
                    );
                    return l / r;
            }
        }
        throw new RuntimeException("Operador aritmetico desconocido: " + op);
    }

    private Object applyComparison(Object left, String op, Object right, int line) {
        switch (op) {
            case "==": return left.equals(right);
            case "!=": return !left.equals(right);
        }
        if (!(left instanceof Number)) {
            throw new RuntimeException(
                "Error Semantico [linea " + line + "]: " +
                "El operador '" + op + "' solo aplica a numeros."
            );
        }
        double l = ((Number) left).doubleValue();
        double r = ((Number) right).doubleValue();
        switch (op) {
            case ">":  return l > r;
            case "<":  return l < r;
            case ">=": return l >= r;
            case "<=": return l <= r;
        }
        throw new RuntimeException("Operador de comparacion desconocido: " + op);
    }

    private void checkBoolean(Object val, String op, int line) {
        if (!(val instanceof Boolean)) {
            throw new RuntimeException(
                "Error Semantico [linea " + line + "]: " +
                "El operador '" + op + "' requiere operandos booleanos, se obtuvo " + typeName(val) + "."
            );
        }
    }

    private String typeName(Object value) {
        if (value == null)            return "null";
        if (value instanceof Integer) return "entero";
        if (value instanceof Double)  return "real";
        if (value instanceof Boolean) return "booleano";
        if (value instanceof String)  return "cadena";
        return value.getClass().getSimpleName();
    }
}