import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;

public class LexerTest {
    SysYLexerLexer sysYLexer;

    public LexerTest(SysYLexerLexer sysYLexer) {
        this.sysYLexer = sysYLexer;
    }

    public void run() {
        sysYLexer.removeErrorListeners();
        LexerErrorListner myErrorListener = new LexerErrorListner();
        sysYLexer.addErrorListener(myErrorListener);
        List<? extends Token> myTokens = sysYLexer.getAllTokens();
        if (myErrorListener.hasErrorInformation()) {
            myErrorListener.printLexerErrorInformation();
        } else {
            for (Token t : myTokens) {
                printSysYTokenInformation(t, sysYLexer.getVocabulary());
            }
        }
    }

    static void printSysYTokenInformation(Token t, Vocabulary v) {
        String symbolName = v.getSymbolicName(t.getType());
        String text = t.getText();
        if (symbolName == "INTEGER_CONST") {
            int intValue = 0;
            if (text.length() > 1 && text.charAt(0) == '0' && (text.charAt(1) == 'x' || text.charAt(1) == 'X')) {
                intValue = Integer.parseInt(text.substring(2), 16);
            } else if (text.length() > 1 && text.charAt(0) == '0') {
                intValue = Integer.parseInt(text.substring(1), 8);
            } else {
                intValue = Integer.parseInt(text);
            }
            System.out.println(String.format("%s %d at Line %d.", symbolName, intValue, t.getLine()));
        } else {
            System.out.println(String.format("%s %s at Line %d.", symbolName, text, t.getLine()));
        }
    }
}
