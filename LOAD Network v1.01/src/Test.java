import java.io.File;

public class Test {
    public static void main(String[] args){
        File folder = new File("../../scratch");
        System.out.println(folder.getAbsolutePath());
    }
}