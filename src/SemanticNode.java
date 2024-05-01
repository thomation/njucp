public class SemanticNode {
    int depth;
    String message;

    public SemanticNode(int depth, String message) {
        this.depth = depth;
        this.message = message;
    }
    public String toString() {
        StringBuffer sb = new StringBuffer();
        for(int i = 0; i < depth; i ++) {
            sb.append("--");
        }
        sb.append(message);
        return sb.toString();
    }
}
