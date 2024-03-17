import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
public class Main
{    
    public static void main(String[] args){
         if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        try{
            CharStream input = CharStreams.fromFileName(source);
            SysYLexerLexer sysYLexer = new SysYLexerLexer(input);
            sysYLexer.removeErrorListeners();
            SimpleErrorListner myErrorListener = new SimpleErrorListner();
            sysYLexer.addErrorListener(myErrorListener);
            List<? extends Token> myTokens = sysYLexer.getAllTokens();
            if (myErrorListener.hasErrorInformation()) {
                myErrorListener.printLexerErrorInformation();
            } else {
                for (Token t : myTokens) {
                    printSysYTokenInformation(t, sysYLexer.getVocabulary());
                }
            }
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }
    static void printSysYTokenInformation(Token t, Vocabulary v) {
        System.out.println(String.format("%s %s at Line %d", v.getSymbolicName(t.getType()), t.getText(), t.getLine()));
    }
}