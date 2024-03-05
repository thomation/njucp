import java.io.*; 
public class Main
{    
    public static void main(String[] args){
        String fileName = args[0]; 
        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String str;
            while ((str = in.readLine()) != null) {
                System.out.println(str);
            }
        } catch (IOException e) {
        }
    }
}