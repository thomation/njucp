import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

public class LexerErrorListner extends BaseErrorListener {
    int errorNo = 0;

    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine,
            String msg, RecognitionException e) {
        System.err.println("Error type A at Line " + line + ":"  + msg);
        errorNo++;
    }

    public Boolean hasErrorInformation() {
        return errorNo > 0;
    }
    public void printLexerErrorInformation() {

    }
}
