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
        terminalFormats.put("INT","\33[36;4m%s");
        terminalFormats.put("VOID_funcType", "\33[36;22m%s");
        terminalFormats.put("VOID","\33[36;4m%s");
        terminalFormats.put("IDENT_funcDef", "\33[33;22m%s");
        terminalFormats.put("RETURN", "\33[36;1m%s");
    }
    private String GetStringFormat(TerminalNode node) {
        Token t = node.getSymbol();
        String symbolName = SysYParser.VOCABULARY.getSymbolicName(t.getType());
        int index = ((RuleNode) node.getParent()).getRuleContext().getRuleIndex();
        String parentType = SysYParser.ruleNames[index];
        String specialKey = String.format("%s_%s", symbolName, parentType);
        if(terminalFormats.containsKey(specialKey)) {
            return terminalFormats.get(specialKey);
        }
        if(terminalFormats.containsKey(symbolName)) {
            return terminalFormats.get(symbolName);
        }
        return "%s";
    }
    @Override
    public Void visitTerminal(TerminalNode node) {
        if(node.getSymbol().getType() != Token.EOF) {
            String format = GetStringFormat(node);
            System.console().printf(format, node.getText());
            System.console().printf("\33[0m", node.getText()); // Reset
        }
        return super.visitTerminal(node);
    }

    @Override
    public Void visitChildren(RuleNode node) {
        // int index = node.getRuleContext().getRuleIndex();
        // System.console().printf("visit children rule index: %d, name: %s\n", index,
        // SysYParser.ruleNames[index]);
        Void result = this.defaultResult();
        int n = node.getChildCount();

        for (int i = 0; i < n && this.shouldVisitNextChild(node, result); ++i) {
            ParseTree c = node.getChild(i);
            Void childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
        }

        return result;
    }

    @Override
    public Void visitFuncType(SysYParser.FuncTypeContext ctx) {
        Void ret = visitChildren(ctx);
        System.console().printf(" ");
        return ret;
    }

    @Override
    public Void visitBlock(SysYParser.BlockContext ctx) {
        blockDepth++;
        System.console().printf(" ");
        Void result = this.defaultResult();
        result = this.aggregateResult(result, ctx.L_BRACE().accept(this));
        System.out.println();
        for (int i = 0; i < ctx.blockItem().size(); i++) {
            printTab();
            ParseTree c = ctx.blockItem(i);
            Void childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
            System.out.println();
        }
        result = this.aggregateResult(result, ctx.R_BRACE().accept(this));
        System.out.println();
        blockDepth--;
        return result;
    }

    private void printTab() {
        for (int i = 0; i < blockDepth; i++)
            System.console().printf("    ");
    }
}
