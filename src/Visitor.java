import java.util.HashMap;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor<Void> {
    int blockDepth;
    HashMap<String, String> terminalFormats = new HashMap<String, String>();

    public Visitor() {
        terminalFormats.put("INT_funcType", "\33[36;22m%s");
        terminalFormats.put("INT", "\33[36;4m%s");
        terminalFormats.put("VOID_funcType", "\33[36;22m%s");
        terminalFormats.put("VOID", "\33[36;4m%s");
        terminalFormats.put("IDENT_funcDef", "\33[33;22m%s");
        terminalFormats.put("RETURN", "\33[36;1m%s");
    }

    private String GetStringFormat(TerminalNode node) {
        Token t = node.getSymbol();
        String symbolName = SysYParser.VOCABULARY.getSymbolicName(t.getType());
        int index = ((RuleNode) node.getParent()).getRuleContext().getRuleIndex();
        String parentType = SysYParser.ruleNames[index];
        String specialKey = String.format("%s_%s", symbolName, parentType);
        if (terminalFormats.containsKey(specialKey)) {
            return terminalFormats.get(specialKey);
        }
        if (terminalFormats.containsKey(symbolName)) {
            return terminalFormats.get(symbolName);
        }
        return "%s";
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
        result = handleChild(result, ctx.R_BRACE());
        printNewLine();
        blockDepth--;
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
                result = handleChild(result, ctx);
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
