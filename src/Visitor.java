import java.util.HashMap;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor<Void> {
    final int BrightRed = 91;
    final int BrightGreen = 92;
    final int BrightYellow = 93;
    final int BrightBlue = 94;
    final int BrightMagenta = 95;
    final int BrightCyan = 96;
    final int White = 97;
    final int UnderLine = 4;
    final int Normal = 22;
    int blockDepth;
    HashMap<String, Integer> terminalColors = new HashMap<String, Integer>();
    HashMap<String, Integer> terminalFonts = new HashMap<String, Integer>();
    BracketFormat[] bracketFormats;

    public Visitor() {
        String[] keywords = new String[] {
                "CONST", "INT", "VOID", "IF", "ELSE", "WHILE", "BREAK", "CONTINUE", "RETURN" };
        initTeminalColors(keywords, BrightCyan);
        String[] operators = new String[] {
                "PLUS", "MINUS", "MUL", "DIV", "MOD", "ASSIGN", "EQ", "NEQ",
                "LT", "GT", "LE", "GE", "NOT", "AND", "OR", "COMMA", "SEMICOLON",
        };
        initTeminalColors(operators, BrightRed);
        terminalColors.put("stmt", White);
        terminalColors.put("funcDef", BrightYellow);
        terminalColors.put("varDef", BrightMagenta);
        terminalColors.put("number", BrightMagenta);
        terminalFonts.put("varDecl", UnderLine);
        // BrightRed,BrightGreen,BrightYellow,BrightBlue,BrightMagenta,BrightCyan
        bracketFormats = new BracketFormat[] {
                new BracketFormat(
                        new int[] { BrightRed, BrightGreen, BrightYellow, BrightBlue, BrightMagenta, BrightCyan },
                        "L_BRACKT", "R_BRACKT"),
                new BracketFormat(
                        new int[] { BrightRed, BrightGreen, BrightYellow, BrightBlue, BrightMagenta, BrightCyan },
                        "L_BRACE", "R_BRACE"),
                new BracketFormat(
                        new int[] { BrightRed, BrightGreen, BrightYellow, BrightBlue, BrightMagenta, BrightCyan },
                        "L_PAREN", "R_PAREN"),
        };
    }

    private void initTeminalColors(String[] symbols, int color) {
        for (int i = 0; i < symbols.length; i++)
            terminalColors.put(symbols[i], color);
    }

    private String GetStringFormat(TerminalNode node) {
        Token t = node.getSymbol();
        String symbolName = SysYParser.VOCABULARY.getSymbolicName(t.getType());
        for (int i = 0; i < bracketFormats.length; i++) {
            if (bracketFormats[i].Check(symbolName)) {
                return bracketFormats[i].getFormatString();
            }
        }
        int color = White;
        int m = matchParent(node, terminalColors);
        if (m > 0)
            color = m;
        int font = Normal;
        m = matchParent(node, terminalFonts);
        if (m > 0)
            font = m;
        if (terminalColors.containsKey(symbolName)) {
            color = terminalColors.get(symbolName);
        }
        return "\33[" + color + ";" + font + "m%s";
    }

    private int matchParent(ParseTree node, HashMap<String, Integer> matchValues) {
        ParseTree cur = node;
        while (cur != null) {
            if (!(cur.getParent() instanceof RuleNode))
                break;
            int index = ((RuleNode) cur.getParent()).getRuleContext().getRuleIndex();
            if (index == -1)
                break;
            String parentType = SysYParser.ruleNames[index];
            if (matchValues.containsKey(parentType)) {
                return matchValues.get(parentType);
            }
            cur = cur.getParent();
        }
        return -1;
    }

    @Override
    public Void visitTerminal(TerminalNode node) {
        if (node.getSymbol().getType() != Token.EOF) {
            String format = GetStringFormat(node);
            System.console().printf(format, node.getText());
            System.console().printf("\33[0m", node.getText()); // Reset
        }
        return super.visitTerminal(node);
    }

    @Override
    public Void visitFuncDef(SysYParser.FuncDefContext ctx) {
        Void ret = visitChildren(ctx);
        printNewLine();
        return ret;
    }

    @Override
    public Void visitFuncType(SysYParser.FuncTypeContext ctx) {
        Void ret = visitChildren(ctx);
        printSpace();
        return ret;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        blockDepth++;
        printSpace();
        Void result = this.defaultResult();
        result = handleChild(result, ctx.L_BRACE());
        printNewLine();
        for (int i = 0; i < ctx.blockItem().size(); i++) {
            printTab();
            result = handleChild(result, ctx.blockItem(i));
            printNewLine();
        }
        blockDepth--;
        printTab();
        result = handleChild(result, ctx.R_BRACE());
        return result;
    }

    @Override
    public Void visitVarDecl(SysYParser.VarDeclContext ctx) {
        Void result = this.defaultResult();
        result = handleChild(result, ctx.btype());
        printSpace();
        for (int i = 0; i < ctx.varDef().size(); i++) {
            result = handleChild(result, ctx.varDef(i));
            if (i < ctx.COMMA().size()) {
                result = handleChild(result, ctx.COMMA(i));
                printSpace();
            }
        }
        result = handleChild(result, ctx.SEMICOLON());
        return result;
    }

    @Override
    public Void visitVarDef(SysYParser.VarDefContext ctx) {
        Void result = this.defaultResult();
        result = handleChild(result, ctx.IDENT());
        if (ctx.L_BRACKT() != null) {
            System.err.println("Does not implement [] in visitVarDef");
        }
        if (ctx.ASSIGN() != null) {
            printSpace();
            result = handleChild(result, ctx.ASSIGN());
            printSpace();
            result = handleChild(result, ctx.initVal());
        }
        return result;
    }

    @Override
    public Void visitStmt(SysYParser.StmtContext ctx) {
        Void result = this.defaultResult();
        if (ctx.RETURN() != null) {
            result = handleChild(result, ctx.RETURN());
            if (ctx.exp() != null) {
                printSpace();
                result = handleChild(result, ctx.exp());
            }
            result = handleChild(result, ctx.SEMICOLON());
        } else if (ctx.ASSIGN() != null) {
            result = handleChild(result, ctx.lVal());
            printSpace();
            result = handleChild(result, ctx.ASSIGN());
            printSpace();
            result = handleChild(result, ctx.exp());
            result = handleChild(result, ctx.SEMICOLON());
        } else {
            result = visitChildren(ctx);
        }
        return result;
    }

    @Override
    public Void visitExp(SysYParser.ExpContext ctx) {
        Void result = this.defaultResult();
        int[] operators = new int[] {
                SysYParser.MUL,
                SysYParser.DIV,
                SysYParser.MOD,
                SysYParser.PLUS,
                SysYParser.MINUS,
        };
        for (int i = 0; i < operators.length; i++) {
            TerminalNode node = ctx.getToken(operators[i], 0);
            if (node != null) {
                result = handleChild(result, ctx.exp(0));
                printSpace();
                handleChild(result, node);
                printSpace();
                result = handleChild(result, ctx.exp(1));
                return result;
            }
        }
        result = visitChildren(ctx);
        return result;
    }

    Void handleChild(Void result, ParseTree child) {
        Void childResult = child.accept(this);
        return this.aggregateResult(result, childResult);
    }

    private void printSpace() {
        System.console().printf(" ");
    }

    private void printNewLine() {
        System.out.println();
    }

    private void printTab() {
        for (int i = 0; i < blockDepth; i++)
            System.console().printf("    ");
    }
}

class BracketFormat {
    int count;
    String left;
    String right;
    String formatString;
    int[] table;

    public BracketFormat(int[] table, String left, String right) {
        this.table = table;
        this.left = left;
        this.right = right;
    }

    public String getFormatString() {
        return formatString;
    }

    public Boolean Check(String symbolName) {
        if (symbolName == left) {
            formatString = "\33[" + table[count] + ";22m%s";
            count++;
            return true;
        }
        if (symbolName == right) {
            count--;
            formatString = "\33[" + table[count] + ";22m%s";
            return true;
        }
        formatString = null;
        return false;
    }
}
