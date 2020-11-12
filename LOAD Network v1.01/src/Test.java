

public class Test {
    public static void main(String[] args){
        for(String key : System.getenv().keySet())
            System.out.println(key + " : " + System.getenv(key));
    }
}