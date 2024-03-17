import java.util.List;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Token;
public class Main
{    
    @SuppressWarnings("deprecation")
    public static void main(String[] args){
         if (args.length < 1) {
            System.err.println("input path is required");
        }
        String source = args[0];
        try{
            CharStream input = CharStreams.fromFileName(source);
            SysYLexerLexer sysYLexer = new SysYLexerLexer(input);
            // sysYLexer.removeErrorListeners();
            // sysYLexer.addErrorListener(myErrorListener);
            List<? extends Token> myTokens = sysYLexer.getAllTokens();
            for (Token t : myTokens) {
                printSysYTokenInformation(t, sysYLexer.getTokenNames());
            }
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }
    static void printSysYTokenInformation(Token t, String[] names) {
        System.out.println(String.format("%s %s at Line %d",names[t.getType()], t.getText(), t.getLine()));
    }
}