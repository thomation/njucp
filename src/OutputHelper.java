import java.util.ArrayList;
import java.util.List;

public class OutputHelper {
    static OutputHelper instance;
    List<SemanticError> errorList = new ArrayList<SemanticError>();
        
    public static OutputHelper getInstance() {
        if (instance == null)
            instance = new OutputHelper();
        return instance;
    }

    public void addSemanticError(SemanticErrorType errorType, int line, String errorMessage) {
        errorList.add(new SemanticError(errorType, line, errorMessage));
    }

    public void addSemantic() {

    }

    public void printResult() {
        if (errorList.size() > 0) {
            for (SemanticError semanticError : errorList) {
                System.err.println(semanticError.toString());
            }
        } else {

        }
    }

}
