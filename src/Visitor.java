import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.RuleNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class Visitor extends SysYParserBaseVisitor<Void> {
    int blockDepth;
    @Override
    public Void visitTerminal(TerminalNode node) {
        System.console().printf("%s", node.getText());
        switch (node.getSymbol().getType()) {
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
        blockDepth ++;
        System.console().printf(" ");
        Void result = this.defaultResult();
        result = this.aggregateResult(result, ctx.L_BRACE().accept(this));
        System.out.println();
        for (int i = 0; i < ctx.blockItem().size(); i ++) {
            printTab();
            ParseTree c = ctx.blockItem(i);
            Void childResult = c.accept(this);
            result = this.aggregateResult(result, childResult);
            System.out.println();
        }
        result = this.aggregateResult(result, ctx.R_BRACE().accept(this));
        System.out.println();
        blockDepth --;
        return result;
    }
    private void printTab() {
        for(int i = 0; i < blockDepth ; i ++)
            System.console().printf("    ");
    }
}
