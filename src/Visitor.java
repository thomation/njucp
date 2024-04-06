import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor<Void> {
    int blockDepth;

    @Override
    public Void visitTerminal(TerminalNode node) {
        Token t = node.getSymbol();
        String symbolName = SysYParser.VOCABULARY.getSymbolicName(t.getType());
        int index = ((RuleNode) node.getParent()).getRuleContext().getRuleIndex();
        String parentType = SysYParser.ruleNames[index];
        String format = "%s";
        switch (symbolName) {
            case "INT":
            case "VOID":
                format = "\33[36;4m%s ";
                if(parentType == "funcType")
                    format = "\33[36;22m%s ";
                break;
            case "RETURN":
                format = "\33[36;1m%s ";
                break;
            case "IDENT":
                if(parentType == "funcDef")
                    format = "\33[33;22m%s ";
                break;
        }
        System.console().printf(format, node.getText());
        System.console().printf("\33[0m", node.getText()); // Reset
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
