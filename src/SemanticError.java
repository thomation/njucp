class SemanticError {
    SemanticErrorType errorType;
    int line;
    String errorMessage;

    public SemanticError(SemanticErrorType errorType, int line, String errorMessage) {
        this.errorType = errorType;
        this.line = line;
        this.errorMessage = errorMessage;
    }

    public String toString() {
        return String.format("Error Type %d at line %d: %s %s", errorType.ordinal(), line, errorType.toString(),
                errorMessage);
    }
}
